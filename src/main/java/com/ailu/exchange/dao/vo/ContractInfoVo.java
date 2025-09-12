package com.ailu.exchange.dao.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ContractInfoVo {
    private String contractId;//合约id
    private String virtualQuantity;//代币数量
    private String virtualAmountUsd;//代币金额
    private String otherAmount;//其他金额数量
    private String transactionCount; //交易总数
    private String isContract;//是否为合约

    private String sender;//交易总数
    private String dateTime;//最早入账时间
    private String balance;//最早一次转账数量大于10万的
    private String suspectedAssociatedAddress; //疑似关联地址

    public ContractInfoVo() {
    }

    public ContractInfoVo(String contractId, String virtualQuantity, String virtualAmountUsd, String otherAmount,
                          String transactionCount, String isContract, String dateTime, String balance, String sender,
                          String suspectedAssociatedAddress) {
        this.contractId = contractId;
        this.virtualQuantity = virtualQuantity;
        this.virtualAmountUsd = virtualAmountUsd;
        this.otherAmount = otherAmount;
        this.transactionCount = transactionCount;
        this.dateTime = dateTime;
        this.balance = balance;
        this.sender = sender;
        this.isContract = isContract;
        this.suspectedAssociatedAddress = suspectedAssociatedAddress;
    }


}
