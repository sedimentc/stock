package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.TokenGrassRemind;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TokenGrassRemindMapper {


    @Insert("<script>" +
            "INSERT INTO token_grass_remind (id,exchange,upwards,downward,contract_id) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.id},#{info.exchange}, #{info.upwards}, #{info.downward},#{info.contractId}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "exchange = VALUES(exchange), " +
            "upwards = VALUES(upwards), " +
            "downward = VALUES(downward) " +
            "</script>")
    void batchInsert(List<TokenGrassRemind> infos);

    @Select("SELECT id,exchange,upwards,downward  FROM token_grass_remind where contract_id=#{contractId} ")
    List<TokenGrassRemind> getTokenGrassRemind(@Param("contractId") String contractId);


    @Delete("DELETE FROM token_grass_remind WHERE `id` = #{id}")
    void del(@Param("id") Integer id);

    @Update("UPDATE token_grass_remind SET `upwards` = '' WHERE `id` = #{id}")
    void updateUpwards(@Param("id") Integer id);

    @Update("UPDATE token_grass_remind SET  `downward` = '' WHERE `id` = #{id}")
    void updateDownward(@Param("id") Integer id);


}
