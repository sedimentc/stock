package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface HistoricalStocksInfoMapper {

    @Insert("INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, gross_revenue, net_profit, total_bonus, " +
            "net_profit_deduction, industry_category) " +
            "VALUES (#{tickerSymbol}, #{tickerName}, #{grossRevenue}, #{netProfit}, #{totalBonus}, #{netProfitDeduction}, " +
            "#{industryCategory})")
    void insert(HistoricalStocksInfo info);

    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, gross_revenue, net_profit) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.grossRevenue}, #{info.netProfit}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "gross_revenue = VALUES(gross_revenue), " +
            "net_profit = VALUES(net_profit) " +
            "</script>")
    void batchInsertOrUpdate(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, bvps_data) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.bvpsData}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "bvps_data = VALUES(bvps_data) " +
            "</script>")
    void batchInsertOrUpdateByBvpsData(List<HistoricalStocksInfo> infos);


    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, net_assets) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.netAssets}" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "net_assets = VALUES(net_assets) " +
            "</script>")
    void batchInsertOrUpdateByNetAssets(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name,total_bonus) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.totalBonus} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "total_bonus = VALUES(total_bonus) " +
            "</script>")
    void batchInsertOrUpdate2(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, " +
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
            "INSERT INTO historical_stocks_info (ticker_symbol, ticker_name, " +
            "announce_date) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.announceDate} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "announce_date = VALUES(announce_date) " +
            "</script>")
    void batchInsertOrUpdate4(List<HistoricalStocksInfo> infos);


    @Delete("DELETE FROM historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    void delete(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Update("UPDATE historical_stocks_info SET gross_revenue = #{grossRevenue}, net_profit = #{netProfit}, total_bonus = " +
            "#{totalBonus}, " +
            "net_profit_deduction = #{netProfitDeduction}, industry_category = #{industryCategory} WHERE ticker_symbol = " +
            "#{tickerSymbol} AND ticker_name = #{tickerName}")
    void update(HistoricalStocksInfo info);

    @Select("SELECT * FROM historical_stocks_info WHERE ticker_symbol = #{tickerSymbol} AND ticker_name = #{tickerName}")
    HistoricalStocksInfo findById(@Param("tickerSymbol") String tickerSymbol, @Param("tickerName") String tickerName);

    @Select("SELECT SUBSTRING_INDEX(ticker_symbol, '.', 1) as tickerSymbol,ticker_name as tickerName, gross_revenue as " +
            "grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction,announce_date as announceDate," +
            "daily_dividends as dailyDividends,net_assets as netAssets,bvps_data bvpsData,dividends_data as dividendsData  " +
            "FROM " +
            "historical_stocks_info " +
            "  ")
    List<HistoricalStocksInfo> findAll();  //WHERE `ticker_symbol` = '000004.SZ'

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name tickerName, gross_revenue as grossRevenue,net_profit as " +
            "netProfit,total_bonus as  totalBonus,net_profit_deduction as  netProfitDeduction, announce_date as announceDate  " +
            "FROM" +
            " " +
            "historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllNew();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, bvps_data bvpsData  FROM historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllBvpsData();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, net_assets as netAssets  FROM historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllNetAssets();


    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, dividends_data as dividendsData  FROM historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllDividendsData();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, dividends_data as dividendsData, " +
            "daily_dividends as dailyDividends FROM historical_stocks_info")
    List<HistoricalStocksInfo> findAllDividendPayloads();

    @Select("SELECT ticker_symbol as tickerSymbol,ticker_name as tickerName, daily_dividends as DailyDividends  FROM historical_stocks_info  ")
    List<HistoricalStocksInfo> findAllDailyDividends();


    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, " +
            "dividends_data) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.dividendsData} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "dividends_data = VALUES(dividends_data) " +
            "</script>")
    void batchInsertOrUpdateDividendsData(List<HistoricalStocksInfo> infos);

    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, dividends_data, daily_dividends) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.dividendsData}, #{info.dailyDividends} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "dividends_data = COALESCE(VALUES(dividends_data), dividends_data), " +
            "daily_dividends = COALESCE(VALUES(daily_dividends), daily_dividends) " +
            "</script>")
    void batchInsertOrUpdateDividendPayloads(List<HistoricalStocksInfo> infos);


    @Insert("<script>" +
            "INSERT INTO historical_stocks_info (ticker_symbol, " +
            "daily_dividends) " +
            "VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.dailyDividends} " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "daily_dividends = VALUES(daily_dividends) " +
            "</script>")
    void batchInsertOrUpdateDailyDividends(List<HistoricalStocksInfo> infos);

}
