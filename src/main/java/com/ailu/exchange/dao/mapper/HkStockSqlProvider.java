package com.ailu.exchange.dao.mapper;

import org.apache.ibatis.jdbc.SQL;

public class HkStockSqlProvider {


    public String findAll() {
        return new SQL() {{
            SELECT("id, SUBSTRING_INDEX(ticker_symbol, '.', 1) AS tickerSymbol, ticker_name AS tickerName, market_cap AS " +
                    "marketCap, pe_ratio_ttm AS peRatioTtm, pb_ratio AS pbRatio, avg_dividend_yield_3yr AS avgDividendYield3yr," +
                    " stock_price AS stockPrice, total_shares AS totalShares, book_value_per_share AS bookValuePerShare, " +
                    "capital_changes AS capitalChanges, price_to_earnings_ratio AS priceToEarningsRatio, total_assets AS " +
                    "totalAssets, total_liabilities AS totalLiabilities, net_assets AS netAssets, industry_category AS " +
                    "industryCategory, performance_growth_score AS performanceGrowthScore, overall_score AS overallScore, " +
                    "liuda_score AS liudaScore,primary_target_price as targetPrice1 ," +
                    " secondary_target_price as targetPrice2, note1," +
                    "note2,forecast_revenue_growth_rate as forecastRevenueGrowthRate," +
                    "forecast_profit_growth_rate as forecastProfitGrowthRate  ," +
                    " forecast_pe_3_years as forecastPe3Years,annual_price_change as  annualPriceChange," +
                    "rev_growth_rate_new as revGrowthRateNew ,profit_growth_rate_new as profitGrowthRateNew," +
                    "pe_forecast_new as peForecastNew,last_year_dividend_rate as lastYearDividendRate," +
                    "last_year_dividend_rate2 as lastYearDividendRate2, " +
                    "last_year_dividend_rate3 as lastYearDividendRate3,max_price_5y as maxPrice5y ,current_annual_dividend as " +
                    "currentAnnualDividend ");
            FROM("hk_stock_info");
            ORDER_BY(" SUBSTRING_INDEX(ticker_symbol, '.', 1) ");

        }}.toString();
    }


}
