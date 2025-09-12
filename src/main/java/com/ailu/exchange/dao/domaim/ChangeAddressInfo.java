package com.ailu.exchange.dao.domaim;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class ChangeAddressInfo {
    public String address;
    private Long  changeTime;
    private BigDecimal changeAmount; //变动数量
    private BigDecimal  nowAmount; //变动后这个还有的数量

    private String typeName;// 属于那个类型


    public ChangeAddressInfo(String address, Long changeTime, BigDecimal changeAmount, BigDecimal nowAmount,String typeName) {
        this.address = address;
        this.changeTime = changeTime;
        this.changeAmount = changeAmount;
        this.nowAmount = nowAmount;
        this.typeName = typeName;
    }

    public ChangeAddressInfo() {
    }
}
