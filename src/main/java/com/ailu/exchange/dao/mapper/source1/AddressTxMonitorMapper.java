package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.AddressTxMonitor;
import com.ailu.exchange.dao.vo.AddressTxMonitorVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface AddressTxMonitorMapper {


    @Insert("<script>" +
            "insert IGNORE into address_tx_monitor ( `from`,`to`,amount,transaction_time,tx_id,is_contract )" +
            " values " +
            " <foreach collection='insertList' item='data' open='' separator=',' close=''> " +
            "(#{data.from}, #{data.to},#{data.amount},#{data.transactionTime},#{data.txId},#{data.isContract} )" +
            " </foreach> " +
            "</script>")
    Long insertList(@Param("insertList") List<AddressTxMonitor> list);



    @Select("SELECT\n" +
            "\t`from`,\n" +
            "\t`to`,\n" +
            "\tamount,\n" +
            "\ttransaction_time AS transactionTime,\n" +
            "\ttx_id AS txId,\n" +
            "\tis_contract AS isContract \n" +
            "FROM\n" +
            "\t`address_tx_monitor` \n" +
            "ORDER BY\n" +
            "\t`transaction_time` DESC \n" +
            "\tLIMIT 1")
    AddressTxMonitor getLatestData();



    // 分页查询
    @Select("SELECT\n" +
            "\tDATE(transaction_time) AS date,\n" +
            "\tSUM(amount) AS totalAmount,\n" +
            "\tCOUNT(DISTINCT CASE WHEN is_contract = 0 THEN `from` END) AS nonContractCount,\n" +
            "\tCOUNT(DISTINCT CASE WHEN is_contract = 1 THEN `from` END) AS contractCount,\n" +
            "\tCEIL(SUM(CASE WHEN amount < 0 THEN amount ELSE 0 END)) AS transferOut\n" +
            "FROM\n" +
            "\taddress_tx_monitor\n" +
            "GROUP BY\n" +
            "\tDATE(transaction_time)\n" +
            "ORDER BY\n" +
            "\tdate DESC\n" +
            "LIMIT #{limit} OFFSET #{offset};")
    List<AddressTxMonitorVo> getDateTotalAmountWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    // 获取总记录数
    @Select("SELECT COUNT(DISTINCT DATE(transaction_time)) FROM address_tx_monitor")
    long getTotalCount();





}
