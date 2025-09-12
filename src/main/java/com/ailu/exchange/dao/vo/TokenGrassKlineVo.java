package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class TokenGrassKlineVo {

    public BigDecimal _10Min;
    private BigDecimal _1Hour;
    private BigDecimal _4Hour;
    private BigDecimal _8Hour;
    private BigDecimal _12Hour;
    private BigDecimal _24Hour;
    private BigDecimal _2Days;
    private BigDecimal _3Days;
    private BigDecimal _7Days;

    public TokenGrassKlineVo() {
        // 默认值为0
        this._10Min = BigDecimal.ZERO;
        this._1Hour = BigDecimal.ZERO;
        this._4Hour = BigDecimal.ZERO;
        this._8Hour = BigDecimal.ZERO;
        this._12Hour = BigDecimal.ZERO;
        this._24Hour = BigDecimal.ZERO;
        this._2Days = BigDecimal.ZERO;
        this._3Days = BigDecimal.ZERO;
        this._7Days = BigDecimal.ZERO;
    }


    public TokenGrassKlineVo(BigDecimal _10Min, BigDecimal _1Hour, BigDecimal _4Hour, BigDecimal _8Hour, BigDecimal _12Hour,
                             BigDecimal _24Hour, BigDecimal _2Days, BigDecimal _3Days, BigDecimal _7Days) {
        this._10Min = _10Min;
        this._1Hour = _1Hour;
        this._4Hour = _4Hour;
        this._8Hour = _8Hour;
        this._12Hour = _12Hour;
        this._24Hour = _24Hour;
        this._2Days = _2Days;
        this._3Days = _3Days;
        this._7Days = _7Days;
    }
}
