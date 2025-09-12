package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.ConditionLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface HkConditionLogMapper {


    @Insert("insert into hk_condition_log (data) values (#{data})")
    void addConditionLog(ConditionLog conditionLog);

    @Select("SELECT\n" +
            "\tid,\n" +
            "\t`data`,\n" +
            "\tUNIX_TIMESTAMP(`ctime`) AS ctime\n" + // 获取毫秒时间戳
            "FROM\n" +
            "\t`hk_condition_log` \n" +
            "ORDER BY\n" +
            "\t`ctime` DESC ")
    List<ConditionLog> getConditionLogNew();

    @Delete("DELETE FROM hk_condition_log WHERE `id` = #{id}")
    void delConditionLog(@Param("id") Integer id);


}
