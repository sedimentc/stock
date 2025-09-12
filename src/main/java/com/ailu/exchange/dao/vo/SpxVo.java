package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class SpxVo {
    private String address;
    private BigDecimal totalAmountIn = BigDecimal.ZERO; // 累计转入金额
    private BigDecimal totalAmountOut = BigDecimal.ZERO; // 累计转出金额
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String hasChange;
    private String currentTotal;

    public SpxVo(String address, BigDecimal totalAmountIn, BigDecimal totalAmountOut, BigDecimal totalAmount,
                 String hasChange,String currentTotal) {
        this.address = address;
        this.totalAmountIn = totalAmountIn;
        this.totalAmountOut = totalAmountOut;
        this.totalAmount = totalAmount;
        this.hasChange = hasChange;
        this.currentTotal = currentTotal;
    }
}
