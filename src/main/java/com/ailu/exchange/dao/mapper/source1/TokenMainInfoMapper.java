package com.ailu.exchange.dao.mapper.source1;


import com.ailu.exchange.dao.domaim.TokenMainInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TokenMainInfoMapper {

    @Insert("<script>" +
            "INSERT INTO token_main_info (id,contract_id, type, address_list,main_type) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.id,info.contractId}, #{info.type}, #{info.addressList},#{info.mainType}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "address_list = VALUES(address_list), " +
            "contract_id = VALUES(contract_id), " +
            "type = VALUES(type) " +
            "</script>")
    void batchInsertOrUpdate(List<TokenMainInfo> infos);

    @Insert("<script>" +
            "INSERT IGNORE token_main_info (contract_id, type, address_list,chain_name,token_name,main_token,main_type) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.contractId}, #{info.type}, #{info.addressList}," +
            "#{info.chainName},#{info.tokenName},#{info.mainToken},#{info.mainType}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "address_list = VALUES(address_list) " +
            "</script>")
    void batchInsertOrUpdateNew(List<TokenMainInfo> infos);

    @Select("SELECT contract_id FROM `token_main_info`  GROUP BY contract_id ")
    List<String> getContractIdList();

    @Select("SELECT\n" +
            "\tcontract_id AS contractId,\n" +
            "\ttoken_name AS tokenName, \n" +
            "\tchain_name AS chainName \n" +
            "FROM\n" +
            "\t`token_main_info` \n" +
            "GROUP BY\n" +
            "\tcontract_id ")
    List<TokenMainInfo> getContractIdByMainToken();


    @Select("SELECT\n" +
            "\tcontract_id AS contractId,\n" +
            "\tchain_name AS chainName,\n" +
            "\ttoken_name AS tokenName,\n" +
            "\tmain_token AS mainToken \n" +
            "FROM\n" +
            "\t`token_main_info` \n" +
            "GROUP BY\n" +
            "\tcontract_id,\n" +
            "\tchain_name,\n" +
            "\ttoken_name")
    List<TokenMainInfo> getTokenMainInfo();

    @Select("SELECT id,type, address_list as addressList,main_type as mainType,chain_name as chainName FROM token_main_info " +
            " where " +
            " contract_id=#{contractId}")
    List<TokenMainInfo> getTokenMainInfoByContractId(@Param("contractId") String contractId);


    @Select("SELECT id,type, address_list as addressList,main_type as mainType FROM token_main_info")
    List<TokenMainInfo> getTokenMainInfoAll();


    @Delete("DELETE FROM token_main_info WHERE `id` = #{id}")
    void delTokenMainInfo(@Param("id") Integer id);

    @Select("SELECT\n" +
            "\ttype \n" +
            "FROM\n" +
            "\t token_main_info \n" +
            "WHERE\n" +
            "\tmain_type =4\n" +
            "GROUP BY\n" +
            "\ttype")
    List<String> getMainTypeList();

    /**
     * 获取 改代币是否存在
     *
     * @param tokenName
     * @return
     */
    @Select("SELECT count(*) FROM token_main_info WHERE token_name = #{tokenName} ")
    Integer getTokneName(@Param("tokenName") String tokenName);


    @Select("SELECT\n" +
            "\ttype,\n" +
            "\taddress_list AS addressList \n" +
            "FROM\n" +
            "\t`token_main_info` \n" +
            "WHERE\n" +
            "\tcontract_id =#{contractId}  AND chain_name=#{chainName}")
    List<TokenMainInfo> getTokenMainInfoByAddressList(@Param("contractId") String contractId,
                                                      @Param("chainName") String chainName);


}
