package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.TokenGrassInfo;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

public interface TokenGrassInfoMapper {

    @Insert("<script>" +
            "INSERT INTO token_grass_info (contract_id,token_account,quantity) VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.contractId},#{info.tokenAccount},#{info.quantity} " +
            "</foreach>" +
            "</script>")
    void batchInsertNew(List<TokenGrassInfo> infos);

}
