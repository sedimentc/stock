package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UsHistoricalStocksInfoMapper {

    @Insert("INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name, gross_revenue, net_profit, total_bonus, " +
            "net_profit_deduction, industry_category) " +
            "VALUES (#{tickerSymbol}, #{tickerName}, #{grossRevenue}, #{netProfit}, #{totalBonus}, #{netProfitDeduction}, " +
            "#{industryCategory})")
    void insert(HistoricalStocksInfo info);

    @Insert("<script>" +
            "INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name, gross_revenue) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.grossRevenue}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "gross_revenue = VALUES(gross_revenue) " +
            "</script>")
    void batchInsertOrUpdateGrossRevenue(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name, net_profit) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.netProfit}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "net_profit = VALUES(net_profit) " +
            "</script>")
    void batchInsertOrUpdateNetProfit(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name,total_bonus) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.totalBonus} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "total_bonus = VALUES(total_bonus) " +
            "</script>")
    void batchInsertOrUpdate2(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name, " +
            "net_profit_deduction) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.netProfitDeduction} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "net_profit_deduction = VALUES(net_profit_deduction) " +
            "</script>")
    void batchInsertOrUpdate3(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_historical_stocks_info (ticker_symbol, ticker_name, " +
            "announce_date) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.announceDate} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "announce_date = VALUES(announce_date) " +
            "</script>")
    void batchInsertOrUpdate4(List<HistoricalStocksInfo> infos);


    @Delete("DELETE FROM us_historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    void delete(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Update("UPDATE us_historical_stocks_info SET gross_revenue = #{grossRevenue}, net_profit = #{netProfit}, total_bonus = " +
            "#{totalBonus}, " +
            "net_profit_deduction = #{netProfitDeduction}, industry_category = #{industryCategory} WHERE ticker_symbol = " +
            "#{tickerSymbol} AND ticker_name = #{tickerName}")
    void update(HistoricalStocksInfo info);

    @Select("SELECT * FROM us_historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    HistoricalStocksInfo findById(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Select("SELECT CASE\n" +
            "\t\t\n" +
            "\tWHEN\n" +
            "\t\tticker_symbol LIKE '%.%' THEN\n" +
            "\t\t\tSUBSTRING_INDEX(\n" +
            "\t\t\t\tticker_symbol,\n" +
            "\t\t\t\t'.',\n" +
            "\t\t\t\tLENGTH( ticker_symbol ) - LENGTH(\n" +
            "\t\t\t\tREPLACE ( ticker_symbol, '.', '' ))) ELSE ticker_symbol \n" +
            "\t\tEND AS tickerSymbol ,ticker_name as tickerName, gross_revenue as " +
            "grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction,announce_date as announceDate," +
            "daily_dividends as dailyDividends,net_assets as netAssets,bvps_data bvpsData,dividends_data as dividendsData  " +
            "FROM " +
            "us_historical_stocks_info " +
            "  ")
    List<HistoricalStocksInfo> findAll();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, gross_revenue as grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction, announce_date as announceDate  " +
            " FROM  us_historical_stocks_info")
    List<HistoricalStocksInfo> findAllNew();
}
