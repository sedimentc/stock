package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class AddressTxMonitorVo {

    private String date;
    private Long nonContractCount; //普通地址转账数量
    private Long contractCount; //合约地址转账数量
    private BigDecimal totalAmount;
    private BigDecimal transferOut;

}
