package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.TokenGrassConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TokenGrassConfigMapper {

    @Select("SELECT id,`name`,`value`,run_alias as runAlias ,sort_number as sortNumber ,sort_number2 as sortNumber2 " +
            " FROM token_grass_config GROUP BY id ")
    List<TokenGrassConfig> getTokenGrassConfigList();

    @Insert("insert into token_grass_config (`name`,`value`) values (#{name},#{value})")
    void addTokenGrassConfig(TokenGrassConfig tokenGrassConfig);


        @Insert("<script>" +
            "INSERT INTO token_grass_config (id,`name`,`value`,run_alias,sort_number,sort_number2) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.id},#{info.name}, #{info.value}, #{info.runAlias}, #{info.sortNumber}, #{info.sortNumber2}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "name = VALUES(name), " +
            "value = VALUES(value), " +
            "run_alias = VALUES(run_alias), " +
            "sort_number = VALUES(sort_number), " +
            "sort_number2 = VALUES(sort_number2) " +
            "</script>")
    void batchInsertOrUpdate(List<TokenGrassConfig> infos);


    @Select("SELECT `value` FROM token_grass_config WHERE is_exchange = '1'")
    List<String> getConfigValueList();




}
