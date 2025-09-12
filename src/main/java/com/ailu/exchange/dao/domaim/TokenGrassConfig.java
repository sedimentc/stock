package com.ailu.exchange.dao.domaim;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TokenGrassConfig {
    private Integer id;
    private String name;
    private String value;
    private String runAlias;
    private Integer sortNumber;
    private Integer sortNumber2;


    public TokenGrassConfig() {
    }
}
