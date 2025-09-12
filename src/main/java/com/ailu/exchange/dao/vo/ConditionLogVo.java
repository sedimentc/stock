package com.ailu.exchange.dao.vo;


import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ConditionLogVo {

    private Integer id;
    private JSONArray data;

    private Long ctime;

    public ConditionLogVo(JSONArray data, Long ctime) {
        this.data = data;
        this.ctime = ctime;
    }

    public ConditionLogVo() {
    }
}
