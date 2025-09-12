package com.ailu.exchange.task;


import com.ailu.exchange.dao.domaim.*;
import com.ailu.exchange.dao.mapper.source1.*;
import com.ailu.exchange.dao.vo.AddressInfoVo;
import com.ailu.exchange.rest.api.OkLinkApi;
import com.ailu.exchange.service.redis.ObjectRedisService;
import com.ailu.exchange.utils.OkHttpUtil;
import com.ailu.exchange.utils.WarnUtil;
import com.ailu.exchange.utils.sms.SmsUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ailu.exchange.base.GlobalCache.*;

@Component
@Slf4j
public class TokenGrassInfoTask {

    @Resource
    TokenGrassInfoMapper tokenGrassInfoMapper;

    @Resource
    TokenGrassKlineMapper tokenGrassKlineMapper;

    @Resource
    OkHttpUtil okHttpUtil;

    @Resource
    TokenMainInfoMapper tokenMainInfoMapper;

    @Resource
    OkLinkApi okLinkApi;

    @Value("${oklink.apikey}")
    private String apikey;


    @Resource
    TokenGrassConfigMapper tokenGrassConfigMapper;


    @PostConstruct
    @Scheduled(fixedDelay = 10 * 1000L)
    public void initTest() {
        List<TokenMainInfo> contractIdList = tokenMainInfoMapper.getTokenMainInfo();
        for (TokenMainInfo tokenMainInfoNew : contractIdList) {
            String contractId = tokenMainInfoNew.getContractId();
            String tokenName = tokenMainInfoNew.getTokenName();
            //主账号编号 1主账号0 附属账号
            Integer mainToken = tokenMainInfoNew.getMainToken();

            List<TokenMainInfo> tokenMainInfoByContractId = tokenMainInfoMapper.getTokenMainInfoByContractId(contractId);
            Map<String, List<String>> typeMap = new HashMap<>();
            Set<String> addressAll = new HashSet<>();
            Set<String> typeAll = new HashSet<>();
            for (TokenMainInfo tokenMainInfo : tokenMainInfoByContractId) {
                String chainName = tokenMainInfo.getChainName();
                String type = tokenMainInfo.getType();
                if (chainName.equalsIgnoreCase("sol")) {
                    typeMap.computeIfAbsent(type, k -> new ArrayList<>()).addAll(Arrays.asList(tokenMainInfo.getAddressList().split(",")));
                    addressAll.addAll(Arrays.asList(tokenMainInfo.getAddressList().split(",")));
                } else {
                    typeMap.computeIfAbsent(type, k -> new ArrayList<>()).addAll(Arrays.asList(tokenMainInfo.getAddressList().toLowerCase().split(",")));
                    addressAll.addAll(Arrays.asList(tokenMainInfo.getAddressList().toLowerCase().split(",")));
                }
                typeAll.add(type);
            }
            //保存对应的代币名称对应的地址 存在相同代币名称 不同链的地址 这种数据在显示的时候会融合 所以可以覆盖
            if (mainToken.equals(1)) {
                TOKEN_NAME_AND_CONTRACT_ID.put(tokenName, contractId);
            }
            //缓存归属对应合约id的所有地址
            CONTRACT_ID_ADDRESS_ALL_MAP.put(contractId, addressAll);
            //缓存归属对应合约id的所有类型名
            CONTRACT_ID_TYPE_INFO_MAP.put(contractId, typeAll);
            CONTRACT_ID_TYPE_MAP.put(contractId, typeMap);
        }

        List<TokenGrassConfig> tokenGrassConfigList = tokenGrassConfigMapper.getTokenGrassConfigList();
        for (TokenGrassConfig tokenGrassConfig : tokenGrassConfigList) {
            String key = tokenGrassConfig.getValue().toUpperCase();
            String name = tokenGrassConfig.getName();
            String runAlias = tokenGrassConfig.getRunAlias();
            Integer sortNumber = tokenGrassConfig.getSortNumber();
            Integer sortNumber2 = tokenGrassConfig.getSortNumber2();

            TOKEN_BY_NAME_CONFIG_INFO_MAP.put(key, name);
            TOKEN_BY_NAME_CONFIG_INFO_MAP_NEW.put(key, tokenGrassConfig.getValue());
            TOKEN_BY_RUN_ALIAS_CONFIG_INFO_MAP.put(key, runAlias);
            TOKEN_BY_SORT_NUMBER_CONFIG_INFO_MAP.put(key, sortNumber);
            TOKEN_BY_SORT_NUMBER2_CONFIG_INFO_MAP.put(key, sortNumber2);
            TOKEN_BY_VALUE_CONFIG_INFO_MAP.put(runAlias, key);
            TOKEN_BY_VALUE_CONFIG_INFO_MAP2.put(name.toUpperCase(), tokenGrassConfig.getValue());
        }

        Map<String, List<TokenMainInfo>> collect =
                contractIdList.stream().collect(Collectors.groupingBy(TokenMainInfo::getTokenName));
        for (Map.Entry<String, List<TokenMainInfo>> stringListEntry : collect.entrySet()) {
            List<TokenMainInfo> value = stringListEntry.getValue();
            for (TokenMainInfo tokenMainInfo : value) {
                CONTRACT_ID_RELATION_MAP.put(tokenMainInfo.getContractId(), value);
            }
        }
    }

    @Scheduled(fixedDelay = 60 * 1000L)
    public void syncTokenGrassInfo() {
        try {
            List<TokenMainInfo> contractIdList = tokenMainInfoMapper.getTokenMainInfo();
            for (TokenMainInfo tokenMainInfo : contractIdList) {
                List<AddressInfoVo> addressInfoVoAllNew = new ArrayList<>();
                String contractId = tokenMainInfo.getContractId();
                String chainName = tokenMainInfo.getChainName();
                String tokenName = tokenMainInfo.getTokenName();

                if (chainName.equalsIgnoreCase("sol")) {
                    String dataStr = okHttpUtil.httpGet("http://47.76.46.94:8080/server/getTokenDataInfo?name=" + tokenName,
                            null);
                    getGrassData(contractId, dataStr, tokenName);
                } else if (chainName.equalsIgnoreCase("eth") || chainName.equalsIgnoreCase("base")) {
                    List<AddressInfoVo> addressInfoVoAll = getAddressInfoVoAll(contractId, chainName);
                    getVirtualData(contractId, addressInfoVoAll);
                    addressInfoVoAllNew.addAll(addressInfoVoAll);
                }
                if (addressInfoVoAllNew.size() > 0) {
                    VIRTUAL_CACHE_DATA_MAP.put(contractId, new ArrayList<>(addressInfoVoAllNew));
                }
            }
            List<TokenGrassKline> tokenGrassKlineList = new ArrayList<>();
            String[] contractIds = {"0x44ff8620b8cA30902395A7bD3F2407e1A091BF73", "0x0b3e328455c4059EEb9e3f84b5543F74E24e7E1b"};
            BigDecimal largeFamilyHoldingAmount = BigDecimal.ZERO;
            for (String contractId : contractIds) {
                List<String> types = getTypes(contractId);
                BigDecimal largeFamilyHoldingAmountNew = tokenGrassKlineMapper.getTotalQuantitySum(String.join(",", types),
                        contractId, types.size());
                if (largeFamilyHoldingAmountNew == null) {
                    largeFamilyHoldingAmountNew = BigDecimal.ZERO;
                }
                largeFamilyHoldingAmount = largeFamilyHoldingAmount.add(largeFamilyHoldingAmountNew);
            }
            String contractId = "0x44ff8620b8cA30902395A7bD3F2407e1A091BF73";
            BigDecimal holdingAmountTotal = new BigDecimal(1000000000L);
            //其他数量
            BigDecimal retailInvestorsHoldingAmount = holdingAmountTotal.subtract(largeFamilyHoldingAmount);
            tokenGrassKlineList.add(new TokenGrassKline(contractId, retailInvestorsHoldingAmount, 10002, null, BigDecimal.ZERO));
            tokenGrassKlineMapper.batchInsertNew(tokenGrassKlineList);
        } catch (Exception e) {
            log.info("获取链上地址数据出现异常！！", e);
        }
    }


    public void getGrassData(String contractId, String dataStr, String tokenName) {
        // 用于存储每种类型的累加值
        Map<Integer, BigDecimal> typeAccumulation = new HashMap<>();
        Map<String, BigDecimal> exchangeAccumulation = new HashMap<>();
        if (StringUtils.isNotEmpty(dataStr)) {
            JSONObject jsonObject = JSONObject.parseObject(dataStr);
            if (jsonObject.getString("code").equals("0")) {
                JSONArray tokenAccounts = jsonObject.getJSONArray("list");
                for (int i = 0; i < tokenAccounts.size(); i++) {
                    JSONObject jsonObject1 = tokenAccounts.getJSONObject(i);
                    String tokenAccount = jsonObject1.getString("tk");
                    BigDecimal uiAmount = jsonObject1.getBigDecimal("balance");
                    Map<String, List<String>> stringListMap = CONTRACT_ID_TYPE_MAP.get(contractId);
                    int type = 0;
                    String exchange = "";
                    for (Map.Entry<String, List<String>> stringListEntry : stringListMap.entrySet()) {
                        List<String> value = stringListEntry.getValue();
                        String key = stringListEntry.getKey();
                        if (value.contains(tokenAccount)) {
                            if (key.matches("\\d+")) {
                                type = Integer.parseInt(key);
                            } else {
                                exchange = key;
                            }
                        }
                    }
                    // 根据确定的类型进行累加
                    if (type > 0) {
                        typeAccumulation.put(type, typeAccumulation.getOrDefault(type, BigDecimal.ZERO).add(uiAmount));
                    }
                    //根据交易对进行累计
                    if (StringUtils.isNotEmpty(exchange)) {
                        exchangeAccumulation.put(exchange,
                                exchangeAccumulation.getOrDefault(exchange, BigDecimal.ZERO).add(uiAmount));
                    }
                }
            }
        }
        BigDecimal lastPrice = getLastPrice();
        List<TokenGrassKline> tokenGrassKlineList = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : typeAccumulation.entrySet()) {
            tokenGrassKlineList.add(new TokenGrassKline(contractId, entry.getValue(), entry.getKey(), null,
                    tokenName.equalsIgnoreCase("grass") ? lastPrice : null));
        }
        for (Map.Entry<String, BigDecimal> entry : exchangeAccumulation.entrySet()) {
            tokenGrassKlineList.add(new TokenGrassKline(contractId, entry.getValue(), null, entry.getKey(),
                    tokenName.equalsIgnoreCase("grass") ? lastPrice : null));
        }
        if (tokenName.equalsIgnoreCase("grass")) {
            List<String> types = getTypes(contractId);
            BigDecimal largeFamilyHoldingAmount = tokenGrassKlineMapper.getTotalQuantitySum(String.join(",", types), contractId
                    , types.size());
            if (largeFamilyHoldingAmount == null) {
                largeFamilyHoldingAmount = BigDecimal.ZERO;
            }
            //10亿总持有量
            BigDecimal holdingAmountTotal = new BigDecimal(1000000000L);
            //剩余散户持有量
            BigDecimal retailInvestorsHoldingAmount = holdingAmountTotal.subtract(largeFamilyHoldingAmount);
            tokenGrassKlineList.add(new TokenGrassKline(contractId, retailInvestorsHoldingAmount, 9, null, lastPrice));
        }
        tokenGrassKlineMapper.batchInsertNew(tokenGrassKlineList);
    }


    public List<String> getTypes(String contractId) {
        List<String> typeByContractId = tokenGrassKlineMapper.getTypeByContractId(contractId);
        List<String> types = new ArrayList<>();
        for (String type : typeByContractId) {
            if (type.equalsIgnoreCase("10002") || type.equalsIgnoreCase("10009")) {
                continue;
            }
            if (type.matches("\\d+")) {
                types.add(type);
            }
        }
        if (types.size() > 0) {
            return types;
        }
        return Collections.emptyList();
    }

    public void getVirtualData(String contractId, List<AddressInfoVo> addressInfoVoAll) {
        // 用于存储每种类型的累加值
        Map<Integer, BigDecimal> typeAccumulation = new HashMap<>();
        Map<String, BigDecimal> exchangeAccumulation = new HashMap<>();
        if (addressInfoVoAll != null && addressInfoVoAll.size() > 0) {
            for (int i = 0; i < addressInfoVoAll.size(); i++) {
                AddressInfoVo addressInfoVo = addressInfoVoAll.get(i);
                String tokenAccount = addressInfoVo.getHolderAddress().toLowerCase();
                BigDecimal uiAmount = new BigDecimal(addressInfoVo.getAmount());
                Map<String, List<String>> stringListMap = CONTRACT_ID_TYPE_MAP.get(contractId);
                int type = 0;
                String exchange = "";
                for (Map.Entry<String, List<String>> stringListEntry : stringListMap.entrySet()) {
                    List<String> value = stringListEntry.getValue();
                    String key = stringListEntry.getKey();
                    if ("10002".equalsIgnoreCase(key)) {
                        continue;
                    }
                    if (value.contains(tokenAccount)) {
                        if (key.matches("\\d+")) {
                            type = Integer.parseInt(key);
                        } else {
                            exchange = key;
                        }
                    }
                }
                // 根据确定的类型进行累加
                if (type > 0) {
                    typeAccumulation.put(type, typeAccumulation.getOrDefault(type, BigDecimal.ZERO).add(uiAmount));
                }
                //根据交易对进行累计
                if (StringUtils.isNotEmpty(exchange)) {
                    exchangeAccumulation.put(exchange,
                            exchangeAccumulation.getOrDefault(exchange, BigDecimal.ZERO).add(uiAmount));
                }
            }
            List<TokenGrassKline> tokenGrassKlineList = new ArrayList<>();
            for (Map.Entry<Integer, BigDecimal> entry : typeAccumulation.entrySet()) {
                tokenGrassKlineList.add(new TokenGrassKline(contractId, entry.getValue(), entry.getKey(), null, BigDecimal.ZERO));
            }
            for (Map.Entry<String, BigDecimal> entry : exchangeAccumulation.entrySet()) {
                tokenGrassKlineList.add(new TokenGrassKline(contractId, entry.getValue(), null, entry.getKey(), BigDecimal.ZERO));
            }

            List<String> types = getTypes(contractId);
            BigDecimal largeFamilyHoldingAmount = tokenGrassKlineMapper.getTotalQuantitySum(String.join(",", types), contractId
                    , types.size());
            if (largeFamilyHoldingAmount == null) {
                largeFamilyHoldingAmount = BigDecimal.ZERO;
            }
            if ("0x57e114B691Db790C35207b2e685D4A43181e6061".equalsIgnoreCase(contractId)) {
                //10亿总持有量
                BigDecimal holdingAmountTotal = new BigDecimal(15000000000L);
                //剩余散户持有量
                BigDecimal retailInvestorsHoldingAmount = holdingAmountTotal.subtract(largeFamilyHoldingAmount);
                tokenGrassKlineList.add(new TokenGrassKline(contractId, retailInvestorsHoldingAmount, 9, null, BigDecimal.ZERO));
            }
            if (tokenGrassKlineList.size() > 0) {
                tokenGrassKlineMapper.batchInsertNew(tokenGrassKlineList);
            }
        }
    }


    /**
     * 获取前200的持有人地址和持有数量
     *
     * @param contractId
     * @param chainName
     * @return
     */
    private List<AddressInfoVo> getAddressInfoVoAll(String contractId, String chainName) {
        List<AddressInfoVo> addressInfoVos = new ArrayList<>();
        for (int i = 1; i < 3; i++) {
            try {
                JSONObject body = okLinkApi.getPositionList(apikey, chainName, contractId, i, "100").execute().body();
                if (body != null && body.getInteger("code").equals(0)) {
                    JSONArray jsonArray = body.getJSONArray("data").getJSONObject(0).getJSONArray("positionList");
                    for (Object o : jsonArray) {
                        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(o));
                        String holderAddress = jsonObject.getString("holderAddress");
                        String amount = jsonObject.getString("amount");
                        addressInfoVos.add(new AddressInfoVo(holderAddress, amount));
                    }
                }
            } catch (IOException e) {
                log.error("获取持有人前两百数据出现异常！！链名:{},地址:{}", chainName, contractId, e);
            }
        }
        return addressInfoVos;
    }


    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void syncTokenGrassInfoNew() {
        try {
            List<TokenMainInfo> tokenMainInfos = tokenMainInfoMapper.getTokenMainInfo();


            for (TokenMainInfo tokenMainInfo : tokenMainInfos) {

                String contractId = tokenMainInfo.getContractId();
                if(!contractId.equalsIgnoreCase("9BB6NFEcjBCtnNLFko2FqVQBq8HHM13kCyYcdQbgpump")){
                    continue;
                }

                List<TokenGrassInfo> tokenGrassInfos = null;
                String tokenName = tokenMainInfo.getTokenName();
                String chainName = tokenMainInfo.getChainName();
                if (chainName.equalsIgnoreCase("sol")) {
                    String dataStr = okHttpUtil.httpGet("http://47.76.46.94:8080/server/getTokenDataInfo?name=" + tokenName,
                            null);
                    tokenGrassInfos = getTokenGrassInfo(dataStr, contractId);
                } else if (chainName.equalsIgnoreCase("eth") || chainName.equalsIgnoreCase("base")) {
                    List<AddressInfoVo> addressInfoVoAll = getAddressInfoVoAll(contractId, chainName);
                    tokenGrassInfos = getTokenEthInfo(addressInfoVoAll, contractId);
                }
                if (tokenGrassInfos != null && tokenGrassInfos.size() > 0) {
                    tokenGrassInfoMapper.batchInsertNew(tokenGrassInfos);
                }
            }
        } catch (Exception e) {
            log.info("获取链上地址数据出现异常！！", e);
        }
    }


    private List<TokenGrassInfo> getTokenGrassInfo(String dataStr, String contractId) {
        List<TokenGrassInfo> tokenGrassInfos = new ArrayList<>();
        if (StringUtils.isNotEmpty(dataStr)) {
            JSONObject jsonObject = JSONObject.parseObject(dataStr);
            if (jsonObject.getString("code").equals("0")) {
                JSONArray tokenAccounts = jsonObject.getJSONArray("list");
                for (int i = 0; i < tokenAccounts.size(); i++) {
                    JSONObject jsonObject1 = tokenAccounts.getJSONObject(i);
                    String tokenAccount = jsonObject1.getString("tk");
                    if (CONTRACT_ID_ADDRESS_ALL_MAP.get(contractId).contains(tokenAccount)) {
                        BigDecimal uiAmount = jsonObject1.getBigDecimal("balance");
                        tokenGrassInfos.add(new TokenGrassInfo(contractId, tokenAccount, uiAmount));
                    }
                }
            }
        }
        return tokenGrassInfos;
    }

    private List<TokenGrassInfo> getTokenEthInfo(List<AddressInfoVo> addressInfoVoAll, String contractId) {
        List<TokenGrassInfo> tokenGrassInfos = new ArrayList<>();
        for (int i = 0; i < addressInfoVoAll.size(); i++) {
            AddressInfoVo addressInfoVo = addressInfoVoAll.get(i);
            String tokenAccount = addressInfoVo.getHolderAddress();
            if (CONTRACT_ID_ADDRESS_ALL_MAP.get(contractId).contains(tokenAccount)) {
                String amount = addressInfoVo.getAmount();
                tokenGrassInfos.add(new TokenGrassInfo(contractId, tokenAccount, new BigDecimal(amount)));
            }
        }
        return tokenGrassInfos;
    }


    private BigDecimal getLastPrice() {
        BigDecimal lastPrice = BigDecimal.ZERO;
        try {
            String dataStr2 = okHttpUtil.httpGet("https://api.bybit.com/v5/market/tickers?category=spot&symbol" + "=GRASSUSDT",
                    null);
            if (StringUtils.isNotEmpty(dataStr2)) {
                JSONObject jsonObject1 = JSONObject.parseObject(dataStr2);
                if (jsonObject1.getString("retCode").equals("0")) {
                    JSONObject jsonObject2 = jsonObject1.getJSONObject("result").getJSONArray("list").getJSONObject(0);
                    lastPrice = jsonObject2.getBigDecimal("lastPrice");
                }
            }
        } catch (Exception e) {
            log.error("获取bybit价格出现异常！！", e);
        }
        return lastPrice;
    }


    @Resource
    SmsUtil smsUtil;
    @Resource
    ObjectRedisService objectRedisService;
    @Resource
    TokenGrassRemindMapper tokenGrassRemindMapper;

        @Scheduled(fixedDelay = 60 * 1000L)
    public void timedAlarm() {
        List<TokenMainInfo> contractIdList = tokenMainInfoMapper.getContractIdByMainToken();
        for (TokenMainInfo tokenMainInfo : contractIdList) {
            String contractId = tokenMainInfo.getContractId();

            String tokenName = tokenMainInfo.getTokenName();

            String chainName = tokenMainInfo.getChainName();

            Object remind = objectRedisService.get(contractId + "_remind");
            if (remind == null) {
                remind = "0";
            } else {
                remind = String.valueOf(remind); // 转换为字符串
            }
            Object phones = objectRedisService.get(contractId + "_phones");
            String phonesNew;
            if (phones == null) {
                phonesNew = ""; // 可以根据需要设置一个默认值
            } else {
                phonesNew = String.valueOf(phones); // 转换为字符串
            }
            if ("1".equals(remind)) {
                String[] split = phonesNew.split(",");
                List<TokenGrassRemind> tokenGrassRemind = tokenGrassRemindMapper.getTokenGrassRemind(contractId);
                for (TokenGrassRemind grassRemind : tokenGrassRemind) {
                    //交易所
                    String exchange = grassRemind.getExchange();
                    //通过程序别名获取到类型
                    String type = TOKEN_BY_VALUE_CONFIG_INFO_MAP.get(exchange);
                    if (StringUtils.isEmpty(type)) {
                        log.info("通过该数据获取到程序别名出现异常！！{}", exchange);
                        continue;
                    }
                    String typeName = TOKEN_BY_NAME_CONFIG_INFO_MAP.get(type.toUpperCase());
                    //获取同一个币种不同链和地址的数量累计
                    BigDecimal quantitySum = getQuantitySum(type, contractId);
                    //向下
                    String downward = grassRemind.getDownward();
                    if (StringUtils.isNotEmpty(downward)) {
                        BigDecimal downwardBig = new BigDecimal(downward).negate();
                        if (quantitySum.compareTo(downwardBig) < 0) {
                            for (String phone : split) {
                                log.info("小于报警:{}当前数量:{},监控数量:{},交易所:{}", phone, quantitySum, downwardBig, typeName);
                                extractedPhone(phone, typeName, "总量小于" + downwardBig, tokenName, quantitySum, contractId,
                                        tokenName,
                                        chainName, type);
                            }
                            tokenGrassRemindMapper.updateDownward(grassRemind.getId());
                        }
                    }
                    //向上
                    String upwards = grassRemind.getUpwards();
                    if (StringUtils.isNotEmpty(upwards)) {
                        BigDecimal upwardsBig = new BigDecimal(upwards);
                        if (quantitySum.compareTo(upwardsBig) > 0) {
                            for (String phone : split) {
                                log.info("大于报警:{}当前数量:{},监控数量:{},交易所:{}", phone, quantitySum, upwardsBig, typeName);
                                extractedPhone(phone, typeName, "总量大于" + upwardsBig, tokenName, quantitySum, contractId,
                                        tokenName,
                                        chainName, type);
                            }
                            tokenGrassRemindMapper.updateUpwards(grassRemind.getId());
                        }
                    }
                }
            }
        }
    }

    private static long lastCallTime = 0;

    private void extractedPhone(String phone, String account, String typeName, String name, BigDecimal quantitySum,
                                String contractId, String tokenName, String chainName, String type) {

        String contentNew = name + "指标触发" + account.replace("钱包", "W数据").replace("筹码", "C数据") + type + ",数据异常，持续了 ，请检查。";
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime >= TimeUnit.MINUTES.toMillis(1)) {
            smsUtil.sendPhoneData(phone, contentNew, "管理员");
            lastCallTime = currentTime;
        } else {
            log.info("拨打电话的频率太高，已跳过此次拨打。");
        }

        Map<String, BigDecimal> specifyAddressLatestQuantityNew = getSpecifyAddressLatestQuantityNew(contractId,
                tokenName, chainName);
        List<String> list = CONTRACT_ID_TYPE_MAP.get(contractId).get(TOKEN_BY_NAME_CONFIG_INFO_MAP_NEW.get(type));
        if (list == null) {
            log.info("list为null,{}", type);
            list = new ArrayList<>();
        }
        String tokenAccounts = "'" + String.join("','", list) + "'";
        List<TokenGrassInfoVo> tokenGrassInfoVoList = tokenGrassKlineMapper.getTokenGrassInfoVoList(tokenAccounts, contractId,
                24);

        StringBuilder data = new StringBuilder();
        for (TokenGrassInfoVo tokenGrassInfoVo : tokenGrassInfoVoList) {
            String tokenAccount = tokenGrassInfoVo.getTokenAccount();
            BigDecimal nowAmount = specifyAddressLatestQuantityNew.get(tokenAccount);
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
            data.append(tokenAccount).append("_").append(changeAmount).append(";");
        }
        String content = name + "指标触发" + account + typeName + ",波动数量:" + quantitySum + "疑似变动钱包地址:" + data.toString();
        //发送飞书报警通知
        WarnUtil.sendMsgToFeyShu(content);
    }


    private BigDecimal getQuantitySum(String type, String contractId) {
        List<TokenMainInfo> tokenMainInfos = CONTRACT_ID_RELATION_MAP.get(contractId);
        BigDecimal quantitySumNew = BigDecimal.ZERO;
        BigDecimal quantitySumNew2 = BigDecimal.ZERO;
        for (TokenMainInfo tokenMainInfo : tokenMainInfos) {
            //获取当前最新的交易所总量
            TokenGrassKline tokenGrassKline;
            BigDecimal tokenGrassKlineByType10Min;
            if (type.matches("\\d+")) {
                tokenGrassKlineByType10Min = tokenGrassKlineMapper.getLastTotalAssets("type", tokenMainInfo.getContractId(),
                        type, 24);
                tokenGrassKline = tokenGrassKlineMapper.getTokenGrassKlineType(type, tokenMainInfo.getContractId());
            } else {
                tokenGrassKline = tokenGrassKlineMapper.getTokenGrassKline(type, tokenMainInfo.getContractId());

                tokenGrassKlineByType10Min = tokenGrassKlineMapper.getLastTotalAssets("exchange", tokenMainInfo.getContractId(), type, 24);
            }
            if (tokenGrassKline == null) {
                tokenGrassKline = new TokenGrassKline();
                log.info("type:{},contractId:{}", type, tokenMainInfo.getContractId());
            }
            BigDecimal quantitySum = tokenGrassKline.getQuantitySum();
            if (quantitySum == null) {
                quantitySum = BigDecimal.ZERO;
            }
            if (tokenGrassKlineByType10Min == null) {
                tokenGrassKlineByType10Min = BigDecimal.ZERO;
            }
            quantitySumNew = quantitySumNew.add(quantitySum);
            quantitySumNew2 = quantitySumNew2.add(tokenGrassKlineByType10Min);
        }
        return quantitySumNew.subtract(quantitySumNew2);
    }


    private Map<String, BigDecimal> getSpecifyAddressLatestQuantityNew(String contractId, String tokenName, String chainName) {
        Map<String, BigDecimal> specifyAddressLatestQuantity = new HashMap<>();
        if (chainName.equalsIgnoreCase("sol")) {
            specifyAddressLatestQuantity = getSpecifyAddressLatestQuantity(tokenName);
        } else if (chainName.equalsIgnoreCase("eth") || chainName.equalsIgnoreCase("base")) {
            specifyAddressLatestQuantity = getSpecifyVirtualAddressLatestQuantity(contractId);
        }
        return specifyAddressLatestQuantity;
    }


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
}
