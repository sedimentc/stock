package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.TokenMainInfo;
import com.ailu.exchange.dao.mapper.source1.TokenMainInfoMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ailu.exchange.base.GlobalCache.TOKEN_NAME_AND_CONTRACT_ID;

@Service
@Slf4j
public class TokenMainInfoService {


    @Resource
    TokenMainInfoMapper tokenMainInfoMapper;


    public void addTokenMainInfo(JSONObject data) {
        String contractId = data.getString("contractId");
        JSONArray mainData = data.getJSONArray("data");
        List<TokenMainInfo> tokenMainInfoList = new ArrayList<>();
        for (Object mainDatum : mainData) {
            JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(mainDatum));
            String name = jsonObject.getString("type");
            Integer id = jsonObject.getInteger("id");
            String address = jsonObject.getString("address");
            JSONArray secondData = jsonObject.getJSONArray("secondData");
            if (secondData != null && secondData.size() > 0) {
                StringBuilder mainAddress = new StringBuilder();
                for (Object secondDatum : secondData) {
                    JSONObject secondJson = JSONObject.parseObject(JSON.toJSONString(secondDatum));
                    Integer idSecond = jsonObject.getInteger("id");
                    String nameSecond = secondJson.getString("type");
                    String addressSecond = secondJson.getString("address");
                    mainAddress.append(addressSecond).append(",");
                    tokenMainInfoList.add(new TokenMainInfo(idSecond, contractId, nameSecond, addressSecond, name));
                }
                if (mainAddress.length() > 0 && mainAddress.toString().endsWith(",")) {
                    mainAddress = new StringBuilder(mainAddress.substring(0, mainAddress.length() - 1));
                }
                if (StringUtils.isEmpty(address)) {
                    address = mainAddress.toString();
                }
            }
            tokenMainInfoList.add(new TokenMainInfo(id, contractId, name, address, null));
        }
        if (tokenMainInfoList.size() > 0) {
            tokenMainInfoMapper.batchInsertOrUpdate(tokenMainInfoList);
        }
    }


    public List<String> getContractIds() {
        return tokenMainInfoMapper.getContractIdList();
    }

    public void delTokenMainInfo(Integer id) {
        tokenMainInfoMapper.delTokenMainInfo(id);
    }

    public JSONObject getTokenMainInfoList(String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            contractId = "Grass7B4RdKfBCjTKgSqnXkqjwiGvQyFbuSCUJr3XXjs";
        }
        //判断输入的是否为代币名称
        String tokenName = TOKEN_NAME_AND_CONTRACT_ID.get(contractId.toLowerCase());
        if (StringUtils.isNotEmpty(tokenName)) {
            contractId = tokenName;
        }

        List<TokenMainInfo> tokenMainInfoByContractId;
        if (StringUtils.isNotEmpty(contractId)) {
            tokenMainInfoByContractId = tokenMainInfoMapper.getTokenMainInfoAll();
        } else {
            tokenMainInfoByContractId = tokenMainInfoMapper.getTokenMainInfoByContractId(contractId);
        }
        //获取到子分类不为空的分组
        Map<String, List<TokenMainInfo>> secondMap =
                tokenMainInfoByContractId.stream().filter(token -> token.getMainType() != null).collect(Collectors.groupingBy(TokenMainInfo::getMainType));
        //获取所有的主类型
        List<TokenMainInfo> mainList =
                tokenMainInfoByContractId.stream().filter(token -> token.getMainType() == null).collect(Collectors.toList());
        JSONObject data = new JSONObject();
        JSONArray mainData = new JSONArray();
        for (TokenMainInfo tokenMainInfo : mainList) {
            JSONObject dataNow = new JSONObject();
            String type = tokenMainInfo.getType();
            Integer id = tokenMainInfo.getId();
            String addressList = tokenMainInfo.getAddressList();
            List<TokenMainInfo> tokenMainInfos = secondMap.get(type);
            JSONArray secondData = new JSONArray();
            if (tokenMainInfos != null) {
                for (TokenMainInfo secondInfo : tokenMainInfos) {
                    JSONObject data2 = new JSONObject();
                    data2.put("id", secondInfo.getId());
                    data2.put("type", secondInfo.getType());
                    data2.put("address", secondInfo.getAddressList());
                    secondData.add(data2);
                }
            }
            dataNow.put("id", id);
            dataNow.put("type", type);
            dataNow.put("address", addressList);
            dataNow.put("secondData", secondData);
            mainData.add(dataNow);
        }
        data.put("contractId", contractId);
        data.put("data", mainData);
        return data;
    }
}
