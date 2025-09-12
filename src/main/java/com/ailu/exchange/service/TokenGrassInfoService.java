package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.*;
import com.ailu.exchange.dao.mapper.source1.TokenGrassKlineMapper;
import com.ailu.exchange.dao.mapper.source1.TokenGrassRemindMapper;
import com.ailu.exchange.dao.vo.AddressInfoVo;
import com.ailu.exchange.dao.vo.TokenGrassKlineVo;
import com.ailu.exchange.service.redis.ObjectRedisService;
import com.ailu.exchange.utils.OkHttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ailu.exchange.base.GlobalCache.*;

@Service
@Slf4j
public class TokenGrassInfoService {


    @Resource
    TokenGrassKlineMapper tokenGrassKlineMapper;

    @Resource
    TokenGrassRemindMapper tokenGrassRemindMapper;


    public JSONObject getTokenGrassKlineData(String contractId) {

        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }
        List<TokenMainInfo> tokenMainInfos = CONTRACT_ID_RELATION_MAP.get(contractId);
        JSONObject dataJson = new JSONObject();
        Map<String, List<TokenGrassKline>> tokenGrassKlineByTypeMap = new HashMap<>();
        for (TokenMainInfo tokenMainInfo : tokenMainInfos) {
            String contractIdNew = tokenMainInfo.getContractId();
            Set<String> list = CONTRACT_ID_TYPE_INFO_MAP.get(contractIdNew);
            for (String type : list) {
                List<TokenGrassKline> tokenGrassKlineByType;
                if (type.matches("\\d+")) {
                    tokenGrassKlineByType = tokenGrassKlineMapper.getTokenGrassKlineByType(Integer.valueOf(type), contractIdNew);
                } else {
                    tokenGrassKlineByType = tokenGrassKlineMapper.getTokenGrassKlineByExchange(type, contractIdNew);
                }
                tokenGrassKlineByTypeMap.computeIfAbsent(type, k -> new ArrayList<>()).addAll(tokenGrassKlineByType);
            }
        }

        for (Map.Entry<String, List<TokenGrassKline>> stringListEntry : tokenGrassKlineByTypeMap.entrySet()) {
            String type = stringListEntry.getKey();
            List<TokenGrassKline> tokenGrassKlineByType = stringListEntry.getValue();
            getKlineByTypeData(dataJson, type, tokenGrassKlineByType);
        }
        return dataJson;
    }

    private static void getKlineByTypeData(JSONObject dataJson, String type,
                                           List<TokenGrassKline> tokenGrassKlineByType) {
        JSONArray data = new JSONArray();
//         使用一个 Map 来按时间戳对数量进行累加
        Map<Long, BigDecimal> timeToQuantityMap = new HashMap<>();
        Map<Long, Long> originalTimeMap = new HashMap<>();
        // 遍历数据，按照时间戳进行累加
        for (TokenGrassKline tokenGrassKline : tokenGrassKlineByType) {
            BigDecimal quantitySum = tokenGrassKline.getQuantitySum().setScale(4, RoundingMode.HALF_UP);
            long cTimeMillis = tokenGrassKline.getCTime().getTime();
            // 将毫秒时间戳转换为分钟，并去除秒和毫秒部分
            Long cTimeMinutes = (cTimeMillis / (60 * 1000)) * (60 * 1000);
            // 累加相同时间点的数量
            timeToQuantityMap.put(cTimeMinutes,
                    timeToQuantityMap.getOrDefault(cTimeMinutes, BigDecimal.ZERO).add(quantitySum));
            originalTimeMap.put(cTimeMinutes, cTimeMillis);
        }
        for (Map.Entry<Long, BigDecimal> entry : timeToQuantityMap.entrySet()) {
            if (entry.getValue() != null) {
                JSONArray data2 = new JSONArray();
                Long time = originalTimeMap.get(entry.getKey());
                data2.add(entry.getValue());  // 累加后的数量
                data2.add(time);    // 时间戳
                data.add(data2);
            }
        }
        String nameKey;
        String dataKey;
        if (type.matches("\\d+")) {
            nameKey = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
            dataKey = TOKEN_BY_RUN_ALIAS_CONFIG_INFO_MAP.get(type.toUpperCase());
        } else {
            nameKey = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
            dataKey = type;
        }

        JSONArray sortedData = new JSONArray(
                data.stream()
                        .sorted(Comparator.comparing(o -> ((JSONArray) o).getLong(1)))
                        .collect(Collectors.toList())
        );
        Integer sortNumber = TOKEN_BY_SORT_NUMBER_CONFIG_INFO_MAP.get(type.toUpperCase());
        JSONObject klineDataInfo = new JSONObject();
        klineDataInfo.put("data", sortedData);
        klineDataInfo.put("name", nameKey);
        klineDataInfo.put("sortNumber", sortNumber);
        dataJson.put(dataKey, klineDataInfo);
    }

    @Resource
    ObjectRedisService objectRedisService;

    public void addTokenGrassRemind(JSONObject data) {
        String contractId = data.getString("contractId");

        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }

        String phones = data.getString("phones");
        objectRedisService.set(contractId + "_phones", phones);
        List<TokenGrassRemind> tokenGrassReminds = new ArrayList<>();
        if (data.getJSONArray("list").size() > 0) {
            tokenGrassReminds = data.getJSONArray("list").toJavaList(TokenGrassRemind.class);
        }
        if (tokenGrassReminds.size() > 0) {
            for (TokenGrassRemind tokenGrassRemind : tokenGrassReminds) {
                tokenGrassRemind.setContractId(contractId);
            }
            tokenGrassRemindMapper.batchInsert(tokenGrassReminds);
        }
    }

    public JSONObject getTokenGrassRemind(String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }

        JSONObject data = new JSONObject();
        Object remind = objectRedisService.get(contractId + "_remind");
        if (remind == null) {
            remind = "0";
        } else {
            remind = String.valueOf(remind); // 转换为字符串
        }
        Object phones = objectRedisService.get(contractId + "_phones");
        if (phones == null) {
            phones = ""; // 可以根据需要设置一个默认值
        } else {
            phones = String.valueOf(phones); // 转换为字符串
        }
        data.put("remind", remind);
        data.put("phones", phones);
        List<TokenGrassRemind> tokenGrassRemind = tokenGrassRemindMapper.getTokenGrassRemind(contractId);
        data.put("list", tokenGrassRemind);
        return data;
    }


    public JSONObject getTokenGrassKlineVoList(String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }
        JSONObject dataJson = new JSONObject();
        Map<String, TokenGrassKlineVo> tokenGrassKlineVoMap = new HashMap<>();
        Map<String, TokenGrassKline> tokenGrassKlineMap = new HashMap<>();
        List<TokenMainInfo> tokenMainInfos = CONTRACT_ID_RELATION_MAP.get(contractId);
        for (TokenMainInfo tokenMainInfo : tokenMainInfos) {
            String contractIdNew = tokenMainInfo.getContractId();
            List<TokenGrassKline> lastExchangeData = tokenGrassKlineMapper.getLastExchangeData(contractIdNew);
            List<TokenGrassKline> lastTypeData = tokenGrassKlineMapper.getLastTypeData(contractIdNew);
            Set<String> list = CONTRACT_ID_TYPE_INFO_MAP.get(contractIdNew);
            Map<String, TokenGrassKline> lastExchangeMap =
                    lastExchangeData.stream().collect(Collectors.toMap(TokenGrassKline::getExchange,
                            k -> k, (existing, replacement) -> replacement));

            Map<Integer, TokenGrassKline> lastTypeMap = lastTypeData.stream().collect(Collectors.toMap(TokenGrassKline::getType,
                    k -> k, (existing, replacement) -> replacement));
            for (String type : list) {
                TokenGrassKline tokenGrassKline;
                TokenGrassKlineVo tokenGrassKlineVo;
                BigDecimal quantitySum;
                if (type.matches("\\d+")) {
                    tokenGrassKline = lastTypeMap.get(Integer.valueOf(type));
                    if (tokenGrassKline == null) {
                        continue;
                    }
                    quantitySum = tokenGrassKline.getQuantitySum().setScale(4, RoundingMode.HALF_UP);
                    tokenGrassKlineVo = getTokenGrassKlineVo(quantitySum, "type", contractIdNew, type);
                } else {
                    tokenGrassKline = lastExchangeMap.get(type);
                    if (tokenGrassKline == null) {
                        continue;
                    }
                    quantitySum = tokenGrassKline.getQuantitySum().setScale(4, RoundingMode.HALF_UP);
                    tokenGrassKlineVo = getTokenGrassKlineVo(quantitySum, "exchange", contractIdNew, type);
                }
                // 使用 merge 进行累加操作
                tokenGrassKlineVoMap.merge(type, tokenGrassKlineVo, (existingVo, newVo) -> {
                    // 累加每个字段
                    existingVo.set_10Min(existingVo.get_10Min().add(newVo.get_10Min()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_1Hour(existingVo.get_1Hour().add(newVo.get_1Hour()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_4Hour(existingVo.get_4Hour().add(newVo.get_4Hour()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_8Hour(existingVo.get_8Hour().add(newVo.get_8Hour()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_12Hour(existingVo.get_12Hour().add(newVo.get_12Hour()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_24Hour(existingVo.get_24Hour().add(newVo.get_24Hour()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_2Days(existingVo.get_2Days().add(newVo.get_2Days()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_3Days(existingVo.get_3Days().add(newVo.get_3Days()).setScale(4, RoundingMode.HALF_UP));
                    existingVo.set_7Days(existingVo.get_7Days().add(newVo.get_7Days()).setScale(4, RoundingMode.HALF_UP));
                    return existingVo;
                });
                // 对 tokenGrassKlineMap 进行相同操作
                tokenGrassKlineMap.merge(type, tokenGrassKline, (existingKline, newKline) -> {
                    // 累加 quantitySum
                    existingKline.setQuantitySum(existingKline.getQuantitySum().add(newKline.getQuantitySum()).setScale(4,
                            RoundingMode.HALF_UP));
                    return existingKline;
                });
            }
        }
        for (Map.Entry<String, TokenGrassKlineVo> stringTokenGrassKlineVoEntry : tokenGrassKlineVoMap.entrySet()) {
            String type = stringTokenGrassKlineVoEntry.getKey();
            String nameKey;
            String dataKey;
            TokenGrassKline tokenGrassKline = tokenGrassKlineMap.get(type);
            TokenGrassKlineVo tokenGrassKlineVo = tokenGrassKlineVoMap.get(type);
            BigDecimal quantitySum = tokenGrassKline.getQuantitySum().setScale(4, RoundingMode.HALF_UP);
            if (type.matches("\\d+")) {
                nameKey = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
                dataKey = TOKEN_BY_RUN_ALIAS_CONFIG_INFO_MAP.get(type.toUpperCase());
            } else {
                nameKey = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
                dataKey = type;
            }
            int sortNumber = TOKEN_BY_SORT_NUMBER2_CONFIG_INFO_MAP.get(type.toUpperCase());
            JSONObject data = new JSONObject();
            data.put("data", tokenGrassKlineVo);
            data.put("name", nameKey);
            data.put("sortNumber", sortNumber);
            data.put("quantitySum", quantitySum);
            dataJson.put(dataKey, data);
        }
        if ("Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs".equalsIgnoreCase(contractId)) {
            getRetailRemainingData(dataJson, "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs", 1000000000L);
        } else if ("0x57e114B691Db790C35207b2e685D4A43181e6061".equalsIgnoreCase(contractId)) {
            getRetailRemainingData(dataJson, "0x57e114B691Db790C35207b2e685D4A43181e6061", 15000000000L);
        }
        dataJson.put("time", System.currentTimeMillis());
        return dataJson;
    }

    /**
     * 设置特殊散户持有地址
     *
     * @param dataJson
     */
    private void getRetailRemainingData(JSONObject dataJson, String contractId, Long maximumSupply) {
        List<String> typeByContractId = tokenGrassKlineMapper.getTypeByContractId(contractId);
        List<String> types = new ArrayList<>();
        for (String type : typeByContractId) {
            if (type.matches("\\d+")) {
                types.add(type);
            }
        }
        //获取大户持有数量
        BigDecimal totalQuantitySum = tokenGrassKlineMapper.getTotalQuantitySum(String.join(",", types), contractId,
                types.size());
        if (totalQuantitySum == null) {
            totalQuantitySum = BigDecimal.ZERO;
        }
        //10亿总持有量
        BigDecimal holdingAmountTotal = new BigDecimal(maximumSupply);
        //最新剩余散户持有量
        BigDecimal retailInvestorsHoldingAmount = holdingAmountTotal.subtract(totalQuantitySum);
        TokenGrassKlineVo tokenGrassKlineVoData = getTokenGrassKlineVoData(retailInvestorsHoldingAmount,
                holdingAmountTotal, contractId);
        JSONObject retailRemainingData = new JSONObject();
        retailRemainingData.put("data", tokenGrassKlineVoData == null ? getDefaultData() : tokenGrassKlineVoData);
        retailRemainingData.put("name", "剩余散户");
        retailRemainingData.put("sortNumber", 8);
        retailRemainingData.put("quantitySum", retailInvestorsHoldingAmount);
        dataJson.put("retailRemaining", retailRemainingData);
    }

    public TokenGrassKlineVo getDefaultData() {
        return new TokenGrassKlineVo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private TokenGrassKlineVo getTokenGrassKlineVo(BigDecimal quantitySum, String paramName, String contractId,
                                                   String type) {

        BigDecimal _10Min = tokenGrassKlineMapper.getLastTotalAssets_10Min(paramName, contractId, type);
        BigDecimal _1Hour = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 1);
        BigDecimal _4Hour = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 4);
        BigDecimal _8Hour = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 8);
        BigDecimal _12Hour = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 12);
        BigDecimal _24Hour = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 24);
        BigDecimal _2Days = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 48);
        BigDecimal _3Days = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 72);
        BigDecimal _7Days = tokenGrassKlineMapper.getLastTotalAssets(paramName, contractId, type, 168);

        return new TokenGrassKlineVo(
                _10Min == null ? BigDecimal.ZERO : quantitySum.subtract(_10Min),
                _1Hour == null ? BigDecimal.ZERO : quantitySum.subtract(_1Hour),
                _4Hour == null ? BigDecimal.ZERO : quantitySum.subtract(_4Hour),
                _8Hour == null ? BigDecimal.ZERO : quantitySum.subtract(_8Hour),
                _12Hour == null ? BigDecimal.ZERO : quantitySum.subtract(_12Hour),
                _24Hour == null ? BigDecimal.ZERO : quantitySum.subtract(_24Hour),
                _2Days == null ? BigDecimal.ZERO : quantitySum.subtract(_2Days),
                _3Days == null ? BigDecimal.ZERO : quantitySum.subtract(_3Days),
                _7Days == null ? BigDecimal.ZERO : quantitySum.subtract(_7Days));
    }

    private static final Map<String, Long> CHANGE_TIME_MAP = new ConcurrentHashMap<>();


    public JSONObject getChangeDataInfo(String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }
        List<ChangeAddressInfo> changeAddressInfos = new ArrayList<>();
        List<TokenMainInfo> tokenMainInfos = CONTRACT_ID_RELATION_MAP.get(contractId);
        for (TokenMainInfo tokenMainInfo : tokenMainInfos) {
            String contractIdNow = tokenMainInfo.getContractId();
            String name = tokenMainInfo.getTokenName();
            Map<String, BigDecimal> specifyAddressLatestQuantity = new HashMap<>();
            String chainName = tokenMainInfo.getChainName();
            if (chainName.equalsIgnoreCase("sol")) {
                specifyAddressLatestQuantity = getSpecifyAddressLatestQuantity(name);
            } else if (chainName.equalsIgnoreCase("eth") || chainName.equalsIgnoreCase("base")) {
                specifyAddressLatestQuantity = getSpecifyVirtualAddressLatestQuantity(contractIdNow);
            }
            // 获取当前变动的最新值超出20万
            BigDecimal tokenGrassKlineByType10Min;
            Set<String> exchangeList = CONTRACT_ID_TYPE_INFO_MAP.get(contractIdNow);
            for (String type : exchangeList) {
                if (type.equalsIgnoreCase("4")) {
                    continue;
                }
                String typeName;
                if (type.matches("\\d+")) {
                    typeName = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
                    tokenGrassKlineByType10Min = tokenGrassKlineMapper.getLastTotalAssetsNew("type", contractIdNow, type, 72);
                } else {
                    tokenGrassKlineByType10Min = tokenGrassKlineMapper.getLastTotalAssetsNew("exchange", contractIdNow, type, 72);
                    typeName = type;
                }
                if (tokenGrassKlineByType10Min == null) {
                    tokenGrassKlineByType10Min = BigDecimal.ZERO;
                }
                if (tokenGrassKlineByType10Min.abs().compareTo(BigDecimal.valueOf(200000)) > 0) {
                    List<String> list = CONTRACT_ID_TYPE_MAP.get(contractIdNow).get(type);
                    if (list == null) {
                        log.info("list为null,{}", type);
                        continue;
                    }
                    String tokenAccounts = "'" + String.join("','", list) + "'";
                    List<TokenGrassInfoVo> tokenGrassInfoVoList = tokenGrassKlineMapper.getTokenGrassInfoVoList(tokenAccounts,
                            contractIdNow, 72);
                    for (TokenGrassInfoVo tokenGrassInfoVo : tokenGrassInfoVoList) {
                        String tokenAccount = tokenGrassInfoVo.getTokenAccount();
                        BigDecimal nowAmount = specifyAddressLatestQuantity.get(tokenAccount);
                        // 如果没有最新数量，则跳过该地址
                        if (nowAmount == null) {
                            log.info("该地址已经不存在排名前200之内:{}", tokenAccount);
                            nowAmount = BigDecimal.ZERO;
                        }
                        // 计算变动数量
                        BigDecimal changeAmount = nowAmount.subtract(tokenGrassInfoVo.getQuantity());
                        if (changeAmount.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                        String key = tokenAccount + "_" + nowAmount.toPlainString();
                        Long changeTime = CHANGE_TIME_MAP.get(key);
                        // 如果缓存中没有时间，才查询数据库并更新缓存
                        if (changeTime == null) {
                            Long timeFromDb;
                            if (nowAmount.abs().compareTo(BigDecimal.ZERO) > 0) {
                                timeFromDb = tokenGrassKlineMapper.getChangeTime(tokenAccount, nowAmount.toPlainString());
                            } else {
                                timeFromDb = tokenGrassKlineMapper.getChangeTimeOld(tokenAccount);
                            }
                            // 如果数据库返回 null，使用当前时间
                            changeTime = (timeFromDb != null) ? timeFromDb : System.currentTimeMillis();
                            CHANGE_TIME_MAP.put(key, changeTime);
                        }
                        changeAddressInfos.add(new ChangeAddressInfo(tokenAccount, changeTime, changeAmount, nowAmount,
                                typeName));
                    }
                }
            }
        }
        JSONObject data = new JSONObject();
        changeAddressInfos.sort((o1, o2) ->
                Comparator.comparing(ChangeAddressInfo::getChangeTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .compare(o1, o2));
        data.put("list", changeAddressInfos);
        return data;
    }


    public void delTokenGrassRemind(Integer id) {
        tokenGrassRemindMapper.del(id);
    }

    public void reviseRemind(String remind, String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }
        List<TokenMainInfo> tokenMainInfos = CONTRACT_ID_RELATION_MAP.get(contractId);
        for (TokenMainInfo tokenMainInfo : tokenMainInfos) {
            objectRedisService.set(tokenMainInfo.getContractId() + "_remind", remind);
        }
    }

    public List<String> getExchangeList() {
        return tokenGrassKlineMapper.getExchangeList();
    }


    @Resource
    OkHttpUtil okHttpUtil;

    public Map<String, BigDecimal> getSpecifyAddressLatestQuantity(String name) {
        try {
            String dataStr = okHttpUtil.httpGet("http://47.76.46.94:8080/server/getTokenDataInfo?name=" + name, null);
            return getTokenGrassInfo(dataStr);
        } catch (Exception e) {
            log.error("获取最新指定地址的数据出现异常！！", e);
        }
        return null;
    }

    public Map<String, BigDecimal> getSpecifyVirtualAddressLatestQuantity(String contractId) {
        try {
            List<AddressInfoVo> addressInfoVos = VIRTUAL_CACHE_DATA_MAP.get(contractId);
            if (addressInfoVos != null) {
                return addressInfoVos.stream().collect(
                        Collectors.toMap(
                                AddressInfoVo::getHolderAddress, // 键：holderAddress
                                vo -> new BigDecimal(vo.getAmount()), // 值：转换成 BigDecimal
                                (existing, replacement) -> replacement // 如果有重复的键，保留新的值
                        )
                );
            }
        } catch (Exception e) {
            log.error("获取最新指定地址的数据出现异常！！", e);
        }
        return null;
    }

    private Map<String, BigDecimal> getTokenGrassInfo(String dataStr) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (StringUtils.isNotEmpty(dataStr)) {
            JSONObject jsonObject = JSONObject.parseObject(dataStr);
            if (jsonObject.getString("code").equals("0")) {
                JSONArray tokenAccounts = jsonObject.getJSONArray("list");
                for (int i = 0; i < tokenAccounts.size(); i++) {
                    JSONObject jsonObject1 = tokenAccounts.getJSONObject(i);
                    String tokenAccount = jsonObject1.getString("tk");
                    BigDecimal uiAmount = jsonObject1.getBigDecimal("balance");
                    map.put(tokenAccount, uiAmount);
                }
            }
        }
        return map;
    }


    public TokenGrassKlineVo getTokenGrassKlineVoData(BigDecimal retailInvestorsHoldingAmount, BigDecimal holdingAmountTotal,
                                                      String contractId) {
        List<TokenGrassKlineVo> tokenGrassKlineVos = new ArrayList<>();

        List<String> typeByContractId = tokenGrassKlineMapper.getTypeByContractId(contractId);
        for (String type : typeByContractId) {
            //跳过交易所
            if (!type.matches("\\d+")) {
                continue;
            }
            BigDecimal _10Min = tokenGrassKlineMapper.getLastTotalAssetsMinute(10, type, contractId);
            BigDecimal _1Hour = tokenGrassKlineMapper.getLastTotalAssetsHour(1, type, contractId);
            BigDecimal _4Hour = tokenGrassKlineMapper.getLastTotalAssetsHour(4, type, contractId);
            BigDecimal _8Hour = tokenGrassKlineMapper.getLastTotalAssetsHour(8, type, contractId);
            BigDecimal _12Hour = tokenGrassKlineMapper.getLastTotalAssetsHour(12, type, contractId);
            BigDecimal _24Hour = tokenGrassKlineMapper.getLastTotalAssetsHour(24, type, contractId);
            BigDecimal _2Days = tokenGrassKlineMapper.getLastTotalAssetsHour(48, type, contractId);
            BigDecimal _3Days = tokenGrassKlineMapper.getLastTotalAssetsHour(72, type, contractId);
            BigDecimal _7Days = tokenGrassKlineMapper.getLastTotalAssetsHour(168, type, contractId);
            tokenGrassKlineVos.add(new TokenGrassKlineVo(_10Min, _1Hour, _4Hour, _8Hour, _12Hour, _24Hour, _2Days, _3Days,
                    _7Days));
        }
        //获取到每个类型某个时间段的所有累计金额
        TokenGrassKlineVo accumulatedVo = tokenGrassKlineVos.stream()
                .reduce(new TokenGrassKlineVo(), (acc, vo) -> {
                    acc.set_10Min(acc.get_10Min().add(vo.get_10Min()));
                    acc.set_1Hour(acc.get_1Hour().add(vo.get_1Hour()));
                    acc.set_4Hour(acc.get_4Hour().add(vo.get_4Hour()));
                    acc.set_8Hour(acc.get_8Hour().add(vo.get_8Hour()));
                    acc.set_12Hour(acc.get_12Hour().add(vo.get_12Hour()));
                    acc.set_24Hour(acc.get_24Hour().add(vo.get_24Hour()));
                    acc.set_2Days(acc.get_2Days().add(vo.get_2Days()));
                    acc.set_3Days(acc.get_3Days().add(vo.get_3Days()));
                    acc.set_7Days(acc.get_7Days().add(vo.get_7Days()));
                    return acc;
                });
        BigDecimal retailInvestors_10Min = holdingAmountTotal.subtract(accumulatedVo.get_10Min());
        BigDecimal retailInvestors_1Hour = holdingAmountTotal.subtract(accumulatedVo.get_1Hour());
        BigDecimal retailInvestors_4Hour = holdingAmountTotal.subtract(accumulatedVo.get_4Hour());
        BigDecimal retailInvestors_8Hour = holdingAmountTotal.subtract(accumulatedVo.get_8Hour());
        BigDecimal retailInvestors_12Hour = holdingAmountTotal.subtract(accumulatedVo.get_12Hour());
        BigDecimal retailInvestors_24Hour = holdingAmountTotal.subtract(accumulatedVo.get_24Hour());
        BigDecimal retailInvestors_2Days = holdingAmountTotal.subtract(accumulatedVo.get_2Days());
        BigDecimal retailInvestors_3Days = holdingAmountTotal.subtract(accumulatedVo.get_3Days());
        BigDecimal retailInvestors_7Days = holdingAmountTotal.subtract(accumulatedVo.get_7Days());


        return new TokenGrassKlineVo(retailInvestorsHoldingAmount.subtract(retailInvestors_10Min),
                retailInvestorsHoldingAmount.subtract(retailInvestors_1Hour),
                retailInvestorsHoldingAmount.subtract(retailInvestors_4Hour),
                retailInvestorsHoldingAmount.subtract(retailInvestors_8Hour),
                retailInvestorsHoldingAmount.subtract(retailInvestors_12Hour),
                retailInvestorsHoldingAmount.subtract(retailInvestors_24Hour),
                retailInvestorsHoldingAmount.subtract(retailInvestors_2Days),
                retailInvestorsHoldingAmount.subtract(retailInvestors_3Days),
                retailInvestorsHoldingAmount.subtract(retailInvestors_7Days));

    }


    private static int getSortNumber(String exchange) {
        int sortNumber = 9999;
        if (exchange.equalsIgnoreCase("1")) {
            sortNumber = 1;
        } else if (exchange.equalsIgnoreCase("2")) {
            sortNumber = 4;
        } else if (exchange.equalsIgnoreCase("3")) {
            sortNumber = 2;
        } else if (exchange.equalsIgnoreCase("4")) {
            sortNumber = 3;
        } else if (exchange.equalsIgnoreCase("5")) {
            sortNumber = 5;
        } else if (exchange.equalsIgnoreCase("6")) {
            sortNumber = 6;
        } else if (exchange.equalsIgnoreCase("7")) {
            sortNumber = 7;
        } else if (exchange.equalsIgnoreCase("8")) {
            sortNumber = 8;
        } else if (exchange.equalsIgnoreCase("Bybit")) {
            sortNumber = 101;
        } else if (exchange.equalsIgnoreCase("BitGet")) {
            sortNumber = 102;
        } else if (exchange.equalsIgnoreCase("GATE")) {
            sortNumber = 103;
        } else if (exchange.equalsIgnoreCase("KuCoin")) {
            sortNumber = 104;
        } else if (exchange.equalsIgnoreCase("MEXC")) {
            sortNumber = 105;
        } else if (exchange.equalsIgnoreCase("DEX")) {
            sortNumber = 106;
        } else if (exchange.equalsIgnoreCase("Crypto")) {
            sortNumber = 107;
        } else if (exchange.equalsIgnoreCase("HTX")) {
            sortNumber = 108;
        } else if (exchange.equalsIgnoreCase("karken")) {
            sortNumber = 109;
        } else if (exchange.equalsIgnoreCase("bitstamp")) {
            sortNumber = 110;
        } else if (exchange.equalsIgnoreCase("coinone")) {
            sortNumber = 111;
        } else if (exchange.equalsIgnoreCase("10003")) {
            sortNumber = 8;
        } else if (exchange.equalsIgnoreCase("10004")) {
            sortNumber = 4;
        }
        return sortNumber;
    }

    private static int getSortNumber_v2(String exchange) {
        int sortNumber = 9999;
        if (exchange.equalsIgnoreCase("1")) {
            sortNumber = 1;
        } else if (exchange.equalsIgnoreCase("2")) {
            sortNumber = 2;
        } else if (exchange.equalsIgnoreCase("3")) {
            sortNumber = 3;
        } else if (exchange.equalsIgnoreCase("4")) {
            sortNumber = 5;
        } else if (exchange.equalsIgnoreCase("5")) {
            sortNumber = 4;
        } else if (exchange.equalsIgnoreCase("6")) {
            sortNumber = 6;
        } else if (exchange.equalsIgnoreCase("7")) {
            sortNumber = 7;
        } else if (exchange.equalsIgnoreCase("8")) {
            sortNumber = 8;
        } else if (exchange.equalsIgnoreCase("Bybit")) {
            sortNumber = 101;
        } else if (exchange.equalsIgnoreCase("BitGet")) {
            sortNumber = 102;
        } else if (exchange.equalsIgnoreCase("GATE")) {
            sortNumber = 103;
        } else if (exchange.equalsIgnoreCase("KuCoin")) {
            sortNumber = 104;
        } else if (exchange.equalsIgnoreCase("MEXC")) {
            sortNumber = 105;
        } else if (exchange.equalsIgnoreCase("DEX")) {
            sortNumber = 106;
        } else if (exchange.equalsIgnoreCase("Crypto")) {
            sortNumber = 107;
        } else if (exchange.equalsIgnoreCase("HTX")) {
            sortNumber = 108;
        } else if (exchange.equalsIgnoreCase("karken")) {
            sortNumber = 109;
        } else if (exchange.equalsIgnoreCase("bitstamp")) {
            sortNumber = 110;
        } else if (exchange.equalsIgnoreCase("coinone")) {
            sortNumber = 111;
        } else if (exchange.equalsIgnoreCase("10003")) {
            sortNumber = 8;
        } else if (exchange.equalsIgnoreCase("10004")) {
            sortNumber = 8;
        }
        return sortNumber;
    }

    private static String getNameKey(String type) {
        String nameKey = "";
        if (type.equals("1")) {
            nameKey = "项目方";
        } else if (type.equals("2")) {
            nameKey = "项目方发币";
        } else if (type.equals("3")) {
            nameKey = "机构锁定筹码";
        } else if (type.equals("4")) {
            nameKey = "交易所钱包";
        } else if (type.equals("5")) {
            nameKey = "质押";
        } else if (type.equals("6")) {
            nameKey = "大户";
        } else if (type.equals("7")) {
            nameKey = "项目方转Bybit";
        } else if (type.equals("8")) {
            nameKey = "做市商";
        } else if (type.equals("10000")) {
            nameKey = "解压池";
        } else if (type.equals("10001")) {
            nameKey = "合约";
        } else if (type.equals("10002")) {
            nameKey = "其他";
        } else if (type.equals("10003")) {
            nameKey = "机构";
        } else if (type.equalsIgnoreCase("10004")) {
            nameKey = "正在解压的ena数量";
        } else if (type.equalsIgnoreCase("10005")) {
            nameKey = "sena";
        } else if (type.equalsIgnoreCase("10006")) {
            nameKey = "手续费收入";
        } else if (type.equalsIgnoreCase("10007")) {
            nameKey = "早期筹码";
        }
        return nameKey;
    }

    private static String getDataKey(String type) {
        String dataKey = "";
        if (type.equals("1")) {
            dataKey = "projectLocked";
        } else if (type.equals("2")) {
            dataKey = "projectCoin";
        } else if (type.equals("3")) {
            dataKey = "institutionLocked";
        } else if (type.equals("4")) {
            dataKey = "exchangeSum";
        } else if (type.equals("5")) {
            dataKey = "pledge";
        } else if (type.equals("6")) {
            dataKey = "largeFamily";
        } else if (type.equals("7")) {
            dataKey = "projectToBybit";
        } else if (type.equals("8")) {
            dataKey = "marketMaker";
        } else if (type.equals("10000")) {
            dataKey = "decompressionPool";
        } else if (type.equals("10001")) {
            dataKey = "contract";
        } else if (type.equals("10002")) {
            dataKey = "other";
        } else if (type.equals("10003")) {
            dataKey = "institution";
        } else if (type.equals("10004")) {
            dataKey = "extractEnaAmount";
        } else if (type.equals("10005")) {
            dataKey = "sena";
        } else if (type.equalsIgnoreCase("10006")) {
            dataKey = "feeIncome";
        } else if (type.equalsIgnoreCase("10007")) {
            dataKey = "earlyChips";
        }
        return dataKey;
    }

//    public static void main(String[] args) {
//        System.out.println((1733297735000L / (60 * 1000)) * (60 * 1000));
//    }
}
