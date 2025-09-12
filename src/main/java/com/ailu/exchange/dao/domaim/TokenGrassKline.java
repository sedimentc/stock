package com.ailu.exchange.dao.domaim;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TokenGrassKline {
    private Integer id;
    private BigDecimal quantitySum;
    private Integer type;
    private Date cTime;
    private String exchange;

    private BigDecimal price;
    private String contractId;


    public TokenGrassKline() {
    }



    public TokenGrassKline(BigDecimal quantitySum, Integer type, String exchange, BigDecimal price) {
        this.quantitySum = quantitySum;
        this.type = type;
        this.exchange = exchange;
        this.price = price;
    }

    public TokenGrassKline(String contractId,BigDecimal quantitySum, Integer type, String exchange, BigDecimal price) {
        this.contractId = contractId;
        this.quantitySum = quantitySum;
        this.type = type;
        this.exchange = exchange;
        this.price = price;
    }
}
