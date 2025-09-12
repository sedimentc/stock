package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.TokenMainInfo;
import com.ailu.exchange.dao.mapper.source1.TokenGrassConfigMapper;
import com.ailu.exchange.dao.mapper.source1.TokenMainInfoMapper;
import com.ailu.exchange.dao.vo.DailyVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ailu.exchange.base.GlobalCache.TOKEN_BY_VALUE_CONFIG_INFO_MAP2;

@Service
@Slf4j
public class FileUploadService {

    @Resource
    TokenMainInfoMapper tokenMainInfoMapper;

    @Resource
    TokenGrassConfigMapper tokenGrassConfigMapper;

    public void addTokenInfo(Map<String, String> setMap, String tokenContractAddress, String tokenName, String chainShortName) {
        List<TokenMainInfo> infos = new ArrayList<>();
        Integer tokneName = tokenMainInfoMapper.getTokneName(tokenName);

        List<TokenMainInfo> tokenMainInfoByAddressList = tokenMainInfoMapper.getTokenMainInfoByAddressList(tokenContractAddress
                , chainShortName);

        Map<String, String> tokenMainInfoHashMap = new HashMap<>();

        if (tokenMainInfoByAddressList != null && tokenMainInfoByAddressList.size() > 0) {
            tokenMainInfoHashMap =
                    tokenMainInfoByAddressList.stream().collect(Collectors.toMap(TokenMainInfo::getType,
                            TokenMainInfo::getAddressList, (t, t2) -> t));
        }
        //设置为主的合约地址
        int mainToken = 0;
        if (tokneName == 0) {
            mainToken = 1;
        }
        //累加交易所相关地址
        String exchangeSum = "";
        //找到现有的交易所名称
        List<String> configValueList = tokenGrassConfigMapper.getConfigValueList();
        for (Map.Entry<String, String> stringStringEntry : setMap.entrySet()) {
            //
            String label = stringStringEntry.getKey().replace("交易所", "").toUpperCase();
            String value = TOKEN_BY_VALUE_CONFIG_INFO_MAP2.get(label);
            if (StringUtils.isEmpty(value)) {
                log.info("改配置标签不存在需要添加后再进行保存:{}", label);
                continue;
            }

            //
            String addressListNew = stringStringEntry.getValue();

            String addressListOld = tokenMainInfoHashMap.get(value);
            if (StringUtils.isNotEmpty(addressListOld)) {
                addressListNew = addressListNew + "," + addressListOld;
            }
            //判断改标签是否为交易所 需要另外标明归属
            String mainType = null;
            if (configValueList.contains(value)) {
                mainType = "4";
                exchangeSum += addressListNew + ",";
            }
            infos.add(new TokenMainInfo(tokenContractAddress, value, addressListNew, mainType, chainShortName, tokenName,
                    mainToken));
        }
        if (exchangeSum.endsWith(",")) {
            exchangeSum = exchangeSum.substring(0, exchangeSum.length() - 1);
        }
        if (StringUtils.isNotEmpty(exchangeSum)) {
            //设置交易所钱包总汇
            infos.add(new TokenMainInfo(tokenContractAddress, "4", exchangeSum, null, chainShortName, tokenName,
                    mainToken));
        }
        if (infos.size() > 0) {
            tokenMainInfoMapper.batchInsertOrUpdateNew(infos);
        }
    }

}
