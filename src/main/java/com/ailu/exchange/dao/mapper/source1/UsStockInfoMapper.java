package com.ailu.exchange.dao.mapper.source1;

import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.StockSqlProvider;
import com.ailu.exchange.dao.mapper.UsStockSqlProvider;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

public interface UsStockInfoMapper {

    @Insert("INSERT INTO us_stock_info (ticker_symbol, ticker_name, market_cap, pe_ratio_ttm, avg_dividend_yield_3yr, " +
            "stock_price," +
            " total_shares, " +
            "book_value_per_share, capital_changes, price_to_earnings_ratio, total_assets, total_liabilities, net_assets, " +
            "industry_category) " +
            "VALUES (#{tickerSymbol}, #{tickerName}, #{marketCap}, #{peRatioTtm}, #{avgDividendYield3yr}, #{stockPrice}, " +
            "#{totalShares}, " +
            "#{bookValuePerShare}, #{capitalChanges}, #{priceToEarningsRatio}, #{totalAssets}, #{totalLiabilities}, " +
            "#{netAssets}, #{industryCategory})")
    void insert(StockInfo info);

    @Insert("<script>" +
            "INSERT INTO us_stock_info (" +
            "ticker_symbol, market_cap, pe_ratio_ttm, pb_ratio, last_year_dividend_rate, last_year_dividend_rate2," +
            "forecast_revenue_growth_rate ,forecast_profit_growth_rate ,forecast_pe_3_years,rev_growth_rate_new ," +
            "profit_growth_rate_new ,pe_forecast_new,annual_price_change  " +
            ") VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol},#{info.marketCap}, #{info.peRatioTtm}, #{info.pbRatio}, " +
            "#{info.lastYearDividendRate}, #{info.lastYearDividendRate2},#{info.forecastRevenueGrowthRate},#{info" +
            ".forecastProfitGrowthRate},#{info.forecastPe3Years},#{info.revGrowthRateNew},#{info.profitGrowthRateNew},#{info" +
            ".peForecastNew},#{info.annualPriceChange}" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "pe_ratio_ttm = VALUES(pe_ratio_ttm), " +
            "pb_ratio = VALUES(pb_ratio), " +
            "last_year_dividend_rate = VALUES(last_year_dividend_rate), " +
            "last_year_dividend_rate2 = VALUES(last_year_dividend_rate2)," +
            "forecast_revenue_growth_rate = VALUES(forecast_revenue_growth_rate)," +
            "forecast_profit_growth_rate = VALUES(forecast_profit_growth_rate)," +
            "rev_growth_rate_new = VALUES(rev_growth_rate_new)," +
            "profit_growth_rate_new = VALUES(profit_growth_rate_new)," +
            "forecast_pe_3_years = VALUES(forecast_pe_3_years)," +
            "market_cap = VALUES(market_cap)," +
            "pe_forecast_new = VALUES(pe_forecast_new),annual_price_change = VALUES(annual_price_change)" +
            "</script>")
    void batchInsertNew(List<StockInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_stock_info (" +
            "ticker_symbol, annual_price_change,stock_price, market_cap, total_shares" +
            ") VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.annualPriceChange},#{info.stockPrice}, #{info.marketCap}, #{info" +
            ".totalShares}" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "annual_price_change = VALUES(annual_price_change), " +
            "stock_price = VALUES(stock_price), " +
            "market_cap = VALUES(market_cap), " +
            "total_shares = VALUES(total_shares)" +
            "</script>")
    void batchInsertNew3(List<StockInfo> infos);




    @Update("<script>" +
            "UPDATE us_stock_info " +
            "SET " +
            "annual_price_change = CASE " +  // 用CASE WHEN匹配对应的ticker_symbol更新
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.annualPriceChange} " +
            "</foreach>" +
            "ELSE annual_price_change END, " +  // 不匹配的记录保持原值
            "max_price_5y = CASE " +
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.maxPrice5y} " +
            "</foreach>" +
            "ELSE max_price_5y END " +
            "WHERE ticker_symbol IN (" +  // 只更新存在于列表中的ticker_symbol
            "<foreach collection='list' item='info' separator=','>" +
            "#{info.tickerSymbol}" +
            "</foreach>" +
            ")" +
            "</script>")
    void batchUpdateIfExists(List<StockInfo> infos);



    @Insert("<script>" +
            "INSERT INTO us_stock_info (" +
            "ticker_symbol, market_cap, total_shares" +
            ") VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.marketCap}, #{info.totalShares}" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "market_cap = VALUES(market_cap), " +
            "total_shares = VALUES(total_shares)" +
            "</script>")
    void batchInsertMarketCap(List<StockInfo> infos);

    @Insert("<script>" +
            "INSERT INTO us_stock_info (" +
            "ticker_symbol, ticker_name, market_cap, pb_ratio, stock_price, total_shares, " +
            "book_value_per_share, total_assets, total_liabilities, net_assets,industry_category" +
            ") VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.marketCap}, #{info.pbRatio}, " +
            "#{info.stockPrice}, #{info.totalShares}, #{info.bookValuePerShare}, " +
            "#{info.totalAssets}, #{info.totalLiabilities}, #{info.netAssets},#{info.industryCategory}" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "market_cap = VALUES(market_cap), " +
//            "pb_ratio = VALUES(pb_ratio), " +
            "stock_price = VALUES(stock_price), " +
            "total_shares = VALUES(total_shares), " +
//            "book_value_per_share = VALUES(book_value_per_share), " +
            "total_assets = VALUES(total_assets), " +
            "total_liabilities = VALUES(total_liabilities), " +
            "net_assets = VALUES(net_assets)" +
            "</script>")
    void batchInsertNew2(List<StockInfo> infos);


    @Insert("<script>" +
            "INSERT INTO us_stock_info (" +
            "ticker_symbol, ticker_name, market_cap, pe_ratio_ttm,pb_ratio, avg_dividend_yield_3yr, stock_price, total_shares, " +
            "book_value_per_share, capital_changes, price_to_earnings_ratio, total_assets, total_liabilities, net_assets, " +
            "industry_category" +
            ") VALUES " +
            "<foreach collection='list' item='info' index='index' open='(' separator='), (' close=')'>" +
            "#{info.tickerSymbol}, #{info.tickerName}, #{info.marketCap}, #{info.peRatioTtm},#{info.pbRatio}, #{info" +
            ".avgDividendYield3yr}, " +
            "#{info.stockPrice}, #{info.totalShares}, #{info.bookValuePerShare}, #{info.capitalChanges}, " +
            "#{info.priceToEarningsRatio}, #{info.totalAssets}, #{info.totalLiabilities}, #{info.netAssets}, " +
            "#{info.industryCategory}" +
            "</foreach>" +
            "</script>")
    void batchInsert(List<StockInfo> infos);

    @Select("Select count(*) FROM us_stock_info ")
    Integer count();

    @Update("UPDATE us_stock_info SET ticker_name = #{tickerName}, market_cap = #{marketCap}, pe_ratio_ttm = #{peRatioTtm}, " +
            "avg_dividend_yield_3yr = #{avgDividendYield3yr}, " +
            "stock_price = #{stockPrice}, total_shares = #{totalShares}, book_value_per_share = #{bookValuePerShare}, " +
            "capital_changes = #{capitalChanges}, " +
            "price_to_earnings_ratio = #{priceToEarningsRatio}, total_assets = #{totalAssets}, total_liabilities = " +
            "#{totalLiabilities}, net_assets = #{netAssets}, " +
            "industry_category = #{industryCategory} WHERE ticker_symbol = #{tickerSymbol}")
    void update(StockInfo info);


    @Update("UPDATE us_stock_info " +
            "SET performance_growth_score = #{performanceGrowthScore} " +
            "WHERE ticker_symbol LIKE CONCAT('%', #{tickerSymbol}, '%')")
    void updateByPerformanceGrowthScore(@Param("tickerSymbol") String tickerSymbol,
                                        @Param("performanceGrowthScore") BigDecimal performanceGrowthScore);


    @Update("UPDATE us_stock_info SET overall_score=#{overallScore} WHERE ticker_symbol LIKE CONCAT('%', #{tickerSymbol}, '%')")
    void updateByOverallScore(@Param("tickerSymbol") String tickerSymbol, @Param("overallScore") BigDecimal overallScore);

    @Update("UPDATE us_stock_info SET liuda_score=#{liudaScore} WHERE ticker_symbol LIKE CONCAT('%', #{tickerSymbol}, '%')")
    void updateByLiudaScore(@Param("tickerSymbol") String tickerSymbol, @Param("liudaScore") BigDecimal liudaScore);

    @Update("UPDATE us_stock_info SET primary_target_price=#{primaryTargetPrice} WHERE id = #{id}")
    void updateByPrimaryTargetPrice(@Param("id") Integer id, @Param("primaryTargetPrice") String primaryTargetPrice);

    @Update("UPDATE us_stock_info SET secondary_target_price=#{secondaryTargetPrice} WHERE id = #{id}")
    void updateBySecondaryTargetPrice(@Param("id") Integer id, @Param("secondaryTargetPrice") String secondaryTargetPrice);

    @Update("UPDATE us_stock_info SET note1=#{note1} WHERE id = #{id}")
    void updateByNote1(@Param("id") Integer id, @Param("note1") String note1);

    @Update("UPDATE us_stock_info SET note2=#{note2} WHERE id = #{id}")
    void updateByNote2(@Param("id") Integer id, @Param("note2") String note2);


    @SelectProvider(type = UsStockSqlProvider.class, method = "findAll")
    List<StockInfo> findAll();

    @Select("SELECT CASE\n" +
            "\t\t\n" +
            "\tWHEN\n" +
            "\t\tticker_symbol LIKE '%.%' THEN\n" +
            "\t\t\tSUBSTRING_INDEX(\n" +
            "\t\t\t\tticker_symbol,\n" +
            "\t\t\t\t'.',\n" +
            "\t\t\t\tLENGTH( ticker_symbol ) - LENGTH(\n" +
            "\t\t\t\tREPLACE ( ticker_symbol, '.', '' ))) ELSE ticker_symbol \n" +
            "\t\tEND AS tickerSymbol  , ticker_symbol as tickerSymbolNew ,ticker_name as tickerName  FROM `us_stock_info`")
    List<StockInfo> getAllTickerSymbol();

    @Select("SELECT CASE\n" +
            "\t\t\n" +
            "\tWHEN\n" +
            "\t\tticker_symbol LIKE '%.%' THEN\n" +
            "\t\t\tSUBSTRING_INDEX(\n" +
            "\t\t\t\tticker_symbol,\n" +
            "\t\t\t\t'.',\n" +
            "\t\t\t\tLENGTH( ticker_symbol ) - LENGTH(\n" +
            "\t\t\t\tREPLACE ( ticker_symbol, '.', '' ))) ELSE ticker_symbol \n" +
            "\t\tEND AS tickerSymbol  , ticker_symbol as tickerSymbolNew ,market_cap as marketCap,max_price_5y as maxPrice5y,stock_price as " +
            " stockPrice,total_shares as totalShares "+
            " FROM " +
            "`us_stock_info`")
    List<StockInfo> getAllTickerSymbolNew();


    @Update("<script>" +
            "UPDATE us_stock_info " +
            "SET " +
            "last_year_dividend_rate = CASE " +  // 匹配ticker_symbol更新对应的字段
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.lastYearDividendRate} " +
            "</foreach>" +
            "ELSE last_year_dividend_rate END, " +  // 不匹配的记录保持原值
            "last_year_dividend_rate2 = CASE " +
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.lastYearDividendRate2} " +
            "</foreach>" +
            "ELSE last_year_dividend_rate2 END, " +
            "last_year_dividend_rate3 = CASE " +
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.lastYearDividendRate3} " +
            "</foreach>" +
            "ELSE last_year_dividend_rate3 END, " +
            "current_annual_dividend = CASE " +
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.currentAnnualDividend} " +
            "</foreach>" +
            "ELSE current_annual_dividend END " +
            "WHERE ticker_symbol IN (" +  // 只更新列表中存在的ticker_symbol
            "<foreach collection='list' item='info' separator=','>" +
            "#{info.tickerSymbol}" +
            "</foreach>" +
            ")" +
            "</script>")
    void batchUpdateLastYearDividendRate(List<StockInfo> infos);



    @Update("<script>" +
            "UPDATE us_stock_info " +
            "SET " +
            "book_value_per_share = CASE " +  // 匹配ticker_symbol更新字段
            "<foreach collection='list' item='info' separator=''>" +
            "WHEN ticker_symbol = #{info.tickerSymbol} THEN #{info.bookValuePerShare} " +
            "</foreach>" +
            "ELSE book_value_per_share END " +  // 不匹配的记录保持原值
            "WHERE ticker_symbol IN (" +  // 只更新列表中存在的ticker_symbol
            "<foreach collection='list' item='info' separator=','>" +
            "#{info.tickerSymbol}" +
            "</foreach>" +
            ")" +
            "</script>")
    void batchUpdateBookValuePerShare(List<StockInfo> infos);



}
