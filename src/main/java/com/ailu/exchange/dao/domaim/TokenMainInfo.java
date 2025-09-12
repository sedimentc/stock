package com.ailu.exchange.dao.domaim;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class TokenMainInfo {
    private Integer id;
    private String contractId;
    private String type;
    private String addressList;
    private String mainType;
    private Date cTime;
    private String chainName;

    private String tokenName;

    private Integer mainToken;


    public TokenMainInfo(Integer id, String contractId, String type, String addressList, String mainType) {
        this.contractId = contractId;
        this.id = id;
        this.type = type;
        this.addressList = addressList;
        this.mainType = mainType;
    }

    public TokenMainInfo(String contractId, String type, String addressList, String mainType, String chainName,
                         String tokenName, Integer mainToken) {
        this.contractId = contractId;
        this.type = type;
        this.addressList = addressList;
        this.mainType = mainType;
        this.chainName = chainName;
        this.tokenName = tokenName;
        this.mainToken = mainToken;
    }

    public TokenMainInfo() {
    }
}
