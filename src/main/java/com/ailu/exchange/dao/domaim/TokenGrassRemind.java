package com.ailu.exchange.dao.domaim;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TokenGrassRemind {

    public Integer id;
    private String exchange;
    private String upwards;
    private String downward;

    private String contractId;

    public TokenGrassRemind() {
    }

    public TokenGrassRemind(String exchange, String upwards, String downward) {
        this.exchange = exchange;
        this.upwards = upwards;
        this.downward = downward;
    }
}
