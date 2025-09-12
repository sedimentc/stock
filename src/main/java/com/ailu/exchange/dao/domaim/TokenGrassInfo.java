package com.ailu.exchange.dao.domaim;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TokenGrassInfo {

    public Integer id;
    private String tokenAccount;
    private BigDecimal quantity;

    private String contractId;


    public TokenGrassInfo(String contractId, String tokenAccount, BigDecimal quantity) {
        this.contractId = contractId;
        this.tokenAccount = tokenAccount;
        this.quantity = quantity;
    }

    public TokenGrassInfo() {
    }
}
