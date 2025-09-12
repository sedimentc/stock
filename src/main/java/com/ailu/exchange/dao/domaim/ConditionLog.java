package com.ailu.exchange.dao.domaim;


import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class ConditionLog {

    private Integer id;
    private String data;

    private Long ctime;

    public ConditionLog(String data, Long time) {
        this.data = data;
        this.ctime = time;
    }

    public ConditionLog() {
    }
}
