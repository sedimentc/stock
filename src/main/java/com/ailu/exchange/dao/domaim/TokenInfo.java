package com.ailu.exchange.dao.domaim;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TokenInfo {

    private String address; //钱包地址
    private String tokenAmount;//代币余额
    private String otherAmount;//其他代币余额
    private String isContract;//是否合约
    private String transactionCount;//交易次数
    private Long transactionTime;//入账时间
    private String amount;//转账数量
    private String from;//发起人地址
    private String suspectedAssociatedAddress; //疑似存在关联地址
    private String topLevelAddress; //最上层地址
    private String type; //当前地址类型
    private Integer score; //评分


    public TokenInfo(String address, String tokenAmount, String otherAmount, String isContract, String transactionCount,
                     Long transactionTime, String amount, String from, String suspectedAssociatedAddress, String type,
                     Integer score) {
        this.address = address;
        this.tokenAmount = tokenAmount;
        this.otherAmount = otherAmount;
        this.isContract = isContract;
        this.transactionCount = transactionCount;
        this.transactionTime = transactionTime;
        this.amount = amount;
        this.from = from;
        this.suspectedAssociatedAddress = suspectedAssociatedAddress;
        this.type = type;
        this.score = score;
    }

    public TokenInfo() {
    }
}
