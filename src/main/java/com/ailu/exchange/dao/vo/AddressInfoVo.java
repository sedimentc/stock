package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AddressInfoVo {
    private  String holderAddress;
    private String amount;

    public AddressInfoVo() {
    }

    public AddressInfoVo(String holderAddress, String amount) {
        this.holderAddress = holderAddress;
        this.amount = amount;
    }
}
