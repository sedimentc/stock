package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.TokenGrassInfoVo;
import com.ailu.exchange.dao.domaim.TokenGrassKline;
import com.ailu.exchange.dao.vo.TokenGrassKlineVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface TokenGrassKlineMapper {

    @Insert("<script>" +
            "INSERT INTO token_grass_kline (contract_id,quantity_sum,`type`,`exchange`,`price`) VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.contractId},#{info.quantitySum},#{info.type},#{info.exchange},#{info.price} " +
            "</foreach>" +
            "</script>")
    void batchInsertNew(List<TokenGrassKline> infos);


    @Select("SELECT quantity_sum as quantitySum, `price`, c_time as cTime " +
            "FROM token_grass_kline " +
            "WHERE `type` = #{type} " +
            "AND contract_id = #{contractId} " +
            "AND c_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    List<TokenGrassKline> getTokenGrassKlineByType(@Param("type") Integer type, @Param("contractId") String contractId);


    @Select("SELECT quantity_sum as quantitySum ,`price`,c_time as cTime  " +
            " FROM token_grass_kline WHERE `exchange` =  #{exchange} and " +
            " contract_id=#{contractId} AND c_time >= DATE_SUB(NOW(),INTERVAL 7 DAY)")
    List<TokenGrassKline> getTokenGrassKlineByExchange(@Param("exchange") String exchange,
                                                       @Param("contractId") String contractId);

    @Select("SELECT\n" +
            "\texchange \n" +
            "FROM\n" +
            "\ttoken_grass_kline  WHERE `exchange` IS NOT NULL \n" +
            "GROUP BY\n" +
            "\texchange")
    List<String> getExchangeList();

    @Select("SELECT quantity_sum AS quantitySum, `price`, c_time AS cTime\n" +
            "FROM token_grass_kline\n" +
            "WHERE `exchange` = #{exchange} and contract_id=#{contractId} \n" +
            "ORDER BY c_time DESC\n" +
            "LIMIT 1\n")
    TokenGrassKline getTokenGrassKline(@Param("exchange") String exchange, @Param("contractId") String contractId);

    @Select("SELECT quantity_sum AS quantitySum, `price`, c_time AS cTime\n" +
            "FROM token_grass_kline\n" +
            "WHERE `type` = #{type} and contract_id=#{contractId} \n" +
            "ORDER BY c_time DESC\n" +
            "LIMIT 1\n")
    TokenGrassKline getTokenGrassKlineType(@Param("type") String type, @Param("contractId") String contractId);


    /**
     * 获取最新的交易所数据
     *
     * @return
     */

    @Select("SELECT\n" +
            "\ttk.quantity_sum AS quantitySum,\n" +
            "\ttk.price,\n" +
            "\ttk.c_time AS cTime,\n" +
            "\ttk.type \n" +
            "FROM\n" +
            "\ttoken_grass_kline tk\n" +
            "\tJOIN ( SELECT type, MAX( c_time ) AS max_c_time FROM token_grass_kline WHERE contract_id = #{contractId}  GROUP " +
            "BY type ) AS" +
            " latest ON tk.type = latest.type \n" +
            "\tAND tk.c_time = latest.max_c_time \n" +
            "WHERE\n" +
            "\tcontract_id = #{contractId} \n" +
            "ORDER BY\n" +
            "\ttk.type")
    List<TokenGrassKline> getLastTypeData(@Param("contractId") String contractId);

    /**
     * 获取最新的类型
     *
     * @return
     */
    @Select("SELECT\n" +
            "\ttk.quantity_sum AS quantitySum,\n" +
            "\ttk.price,\n" +
            "\ttk.c_time AS cTime,\n" +
            "\ttk.exchange \n" +
            "FROM\n" +
            "\ttoken_grass_kline tk\n" +
            "\tJOIN ( SELECT exchange, MAX( c_time ) AS max_c_time FROM token_grass_kline WHERE contract_id=#{contractId}  " +
            "GROUP BY exchange ) AS latest ON tk" +
            ".exchange = latest.exchange \n" +
            "\tAND tk.c_time = latest.max_c_time  WHERE contract_id=#{contractId} " +
            "ORDER BY\n" +
            "\ttk.exchange;")
    List<TokenGrassKline> getLastExchangeData(@Param("contractId") String contractId);


    @Select("SELECT\n" +
            "\tt.token_account as tokenAccount,\n" +
            "\tt.quantity,\n" +
            "\tt.c_time as cTime \n" +
            "FROM\n" +
            "\t`token_grass_info` t\n" +
            "\tJOIN (\n" +
            "\tSELECT\n" +
            "\t\ttoken_account,\n" +
            "\t\tMIN( c_time ) AS min_c_time \n" +
            "\tFROM\n" +
            "\t\t`token_grass_info` \n" +
            "\tWHERE\n" +
            "\t\ttoken_account IN (${tokenAccounts}) and contract_id=#{contractId} \n" +
            "\t\tAND c_time > NOW() - INTERVAL ${intervalInMinutes}  HOUR \n" +
            "\tGROUP BY\n" +
            "\t\ttoken_account \n" +
            "\t) AS recent_time ON t.token_account = recent_time.token_account \n" +
            "\tAND t.c_time = recent_time.min_c_time \n" +
            "ORDER BY\n" +
            "\tt.token_account;")
    List<TokenGrassInfoVo> getTokenGrassInfoVoList(@Param("tokenAccounts") String tokenAccounts,
                                                   @Param("contractId") String contractId,@Param("intervalInMinutes") Integer intervalInMinutes);


    @Update({
            "SET @row_num = 0;",
            "UPDATE token_grass_info t",
            "JOIN (",
            "    SELECT id FROM token_grass_info",
            "    WHERE token_account = #{tokenAccount}",
            "    AND c_time < #{cTime}",
            "    ORDER BY id DESC",
            ") AS temp ON t.id = temp.id",
            "SET t.c_time = ADDDATE( #{cTime}, INTERVAL - ( @row_num := @row_num + 1 ) * 5 MINUTE )",
            "WHERE t.token_account = #{tokenAccount}",
            "AND t.c_time < #{cTime}"
    })
    int updateTokenGrassInfo(@Param("tokenAccount") String tokenAccount, @Param("cTime") String cTime);

    @Select("SELECT UNIX_TIMESTAMP(c_time)*1000 AS c_time_stamp\n" +
            "FROM token_grass_info\n" +
            "WHERE `token_account` = #{tokenAccount}\n" +
            "  AND `quantity` = #{quantity}\n" +
            "ORDER BY `c_time` LIMIT 0,1;\n")
    Long getChangeTime(@Param("tokenAccount") String tokenAccount, @Param("quantity") String quantity);

    @Select("SELECT UNIX_TIMESTAMP(c_time)*1000 AS c_time_stamp\n" +
            "FROM token_grass_info\n" +
            "WHERE `token_account` = #{tokenAccount}\n" +
            "ORDER BY `c_time` DESC LIMIT 0,1;\n")
    Long getChangeTimeOld(@Param("tokenAccount") String tokenAccount);


    @Select("SELECT\n" +
            "\tquantity_sum AS quantitySum\n" +
            "FROM\n" +
            "\ttoken_grass_kline \n" +
            "WHERE\n" +
            "  ${paramName} = #{type} AND contract_id=#{contractId} and  c_time  <= NOW() - INTERVAL 10 MINUTE \n" +
            " ORDER BY\n" +
            "\tc_time DESC " +
            "\tLIMIT 1;")
    BigDecimal getLastTotalAssets_10Min(@Param("paramName") String paramName, @Param("contractId") String contractId,
                                        @Param("type") String type);


    // AND contract_id=#{contractId}
    @Select("SELECT\n" +
            "\tquantity_sum AS quantitySum\n" +
            "FROM\n" +
            "\ttoken_grass_kline \n" +
            "WHERE\n" +
            "  ${paramName} = #{type} and  contract_id=#{contractId} and  c_time  <= NOW() - INTERVAL ${intervalInMinutes} " +
            "HOUR\n" +
            " ORDER BY\n" +
            "\tc_time DESC " +
            "\tLIMIT 1;")

    BigDecimal getLastTotalAssets(@Param("paramName") String paramName, @Param("contractId") String contractId,
                                  @Param("type") String type, @Param("intervalInMinutes") Integer intervalInMinutes);
    @Select("SELECT\n" +
            "    IFNULL((\n" +
            "        SELECT quantity_sum\n" +
            "        FROM token_grass_kline\n" +
            "        WHERE  ${paramName} = #{type} \n" +
            "          AND contract_id = #{contractId}\n" +
            "          AND c_time <= NOW() - INTERVAL ${intervalInMinutes} HOUR\n" +
            "        ORDER BY c_time DESC\n" +
            "        LIMIT 1\n" +
            "    ), (\n" +
            "        SELECT quantity_sum\n" +
            "        FROM token_grass_kline\n" +
            "        WHERE ${paramName} = #{type} \n" +
            "          AND contract_id = #{contractId} \n" +
            "        ORDER BY c_time ASC\n" +
            "        LIMIT 1\n" +
            "    )) AS quantitySum;")
    BigDecimal getLastTotalAssetsNew(@Param("paramName") String paramName, @Param("contractId") String contractId,
                                  @Param("type") String type, @Param("intervalInMinutes") Integer intervalInMinutes);


    @Select("SELECT\n" +
            "\tSUM( quantity_sum ) AS totalQuantitySum \n" +
            "FROM\n" +
            "\t(\n" +
            "\tSELECT\n" +
            "\t\tquantity_sum \n" +
            "\tFROM\n" +
            "\t\ttoken_grass_kline \n" +
            "\tWHERE\n" +
            "\t\ttype IN ( ${types} )  and  contract_id=#{contractId} \n" +
            "\tORDER BY\n" +
            "\t\tc_time DESC \n" +
            "\tLIMIT ${limit} \n" +
            "\t) AS latest_records;")
    BigDecimal getTotalQuantitySum(@Param("types") String types, @Param("contractId") String contractId,
                                   @Param("limit") Integer limit);


    @Select("SELECT\n" +
            "\tquantity_sum AS quantitySum \n" +
            "FROM\n" +
            "\ttoken_grass_kline \n" +
            "WHERE\n" +
            "\tc_time >= DATE_SUB( NOW(), INTERVAL ${intervalInMinutes} HOUR ) \n" +
            "\tAND type = #{type} and  contract_id=#{contractId} \n" +
            "ORDER BY\n" +
            "\tc_time \n" +
            "\tLIMIT 1;")
    BigDecimal getLastTotalAssetsHour(@Param("intervalInMinutes") Integer intervalInMinutes, @Param("type") String type,
                                      @Param("contractId") String contractId);

    @Select("SELECT\n" +
            "\tquantity_sum AS quantitySum \n" +
            "FROM\n" +
            "\ttoken_grass_kline \n" +
            "WHERE\n" +
            "\tc_time >= DATE_SUB( NOW(), INTERVAL ${intervalInMinutes} MINUTE ) \n" +
            "\tAND type = #{type} and  contract_id=#{contractId} \n" +
            "ORDER BY\n" +
            "\tc_time \n" +
            "\tLIMIT 1;")
    BigDecimal getLastTotalAssetsMinute(@Param("intervalInMinutes") Integer intervalInMinutes, @Param("type") String type,
                                        @Param("contractId") String contractId);


    @Select("SELECT type FROM token_main_info WHERE contract_id = #{contractId} GROUP BY type")
    List<String> getTypeByContractId(@Param("contractId") String contractId);

}
