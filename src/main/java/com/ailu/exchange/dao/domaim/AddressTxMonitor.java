package com.ailu.exchange.dao.domaim;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
public class AddressTxMonitor {
    private Integer id;
    private String from;
    private String to;
    private BigDecimal amount;
    private Date transactionTime;
    private String txId;
    private Integer isContract;

    public AddressTxMonitor(String from, String to, BigDecimal amount, Date transactionTime, String txId, Integer isContract) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.txId = txId;
        this.isContract = isContract;
    }
    public AddressTxMonitor(String from, String to, BigDecimal amount, Date transactionTime) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.txId = txId;
    }


    public AddressTxMonitor() {
    }
}
