package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class DailyVo {

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private String tradeDate;


    public DailyVo(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, String tradeDate) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.tradeDate = tradeDate;
    }

    public DailyVo() {
    }
}
