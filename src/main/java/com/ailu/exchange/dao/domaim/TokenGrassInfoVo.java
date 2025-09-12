package com.ailu.exchange.dao.domaim;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TokenGrassInfoVo {

    private String tokenAccount;
    private BigDecimal quantity;

    private Date cTime;


    public TokenGrassInfoVo(String tokenAccount, BigDecimal quantity) {
        this.tokenAccount = tokenAccount;
        this.quantity = quantity;
    }

    public TokenGrassInfoVo() {
    }
}
