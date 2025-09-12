package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface HkHistoricalStocksInfoMapper {

    @Insert("INSERT INTO hk_historical_stocks_info (ticker_symbol, ticker_name, gross_revenue, net_profit, total_bonus, " +
            "net_profit_deduction, industry_category) " +
            "VALUES (#{tickerSymbol}, #{tickerName}, #{grossRevenue}, #{netProfit}, #{totalBonus}, #{netProfitDeduction}, " +
            "#{industryCategory})")
    void insert(HistoricalStocksInfo info);

    @Insert("<script>" +
            "INSERT INTO hk_historical_stocks_info (ticker_symbol, ticker_name, gross_revenue) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.grossRevenue}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "gross_revenue = VALUES(gross_revenue) " +
            "</script>")
    void batchInsertOrUpdate(List<HistoricalStocksInfo> infos);
//    @Insert("<script>" +
//            "INSERT INTO hk_historical_stocks_info (ticker_symbol, ticker_name, gross_revenue, net_profit) " +
//            "VALUES " +
//            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
//            "#{info.tickerSymbol}, #{info.tickerName}, #{info.grossRevenue}, #{info.netProfit}" +
//            "</foreach> " +
//            "ON DUPLICATE KEY UPDATE " +
//            "gross_revenue = VALUES(gross_revenue), " +
//            "net_profit = VALUES(net_profit) " +
//            "</script>")
//    void batchInsertOrUpdate(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO hk_historical_stocks_info (ticker_symbol, ticker_name,total_bonus) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.totalBonus} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "total_bonus = VALUES(total_bonus) " +
            "</script>")
    void batchInsertOrUpdate2(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO hk_historical_stocks_info (ticker_symbol, ticker_name, " +
            "net_profit_deduction) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.netProfitDeduction} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "net_profit_deduction = VALUES(net_profit_deduction) " +
            "</script>")
    void batchInsertOrUpdate3(List<HistoricalStocksInfo> infos);


    @Delete("DELETE FROM hk_historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    void delete(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Update("UPDATE hk_historical_stocks_info SET gross_revenue = #{grossRevenue}, net_profit = #{netProfit}, total_bonus = " +
            "#{totalBonus}, " +
            "net_profit_deduction = #{netProfitDeduction}, industry_category = #{industryCategory} WHERE ticker_symbol = " +
            "#{tickerSymbol} AND ticker_name = #{tickerName}")
    void update(HistoricalStocksInfo info);

    @Select("SELECT * FROM hk_historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    HistoricalStocksInfo findById(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Select("SELECT SUBSTRING_INDEX(ticker_symbol, '.', 1) as tickerSymbol,ticker_name as tickerName, gross_revenue as " +
            "grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction  FROM hk_historical_stocks_info  ")
    List<HistoricalStocksInfo> findAll();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, gross_revenue as grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction  FROM hk_historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllNew();
}
