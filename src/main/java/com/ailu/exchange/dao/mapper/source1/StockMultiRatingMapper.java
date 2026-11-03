package com.ailu.exchange.dao.mapper.source1;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StockMultiRatingMapper {
    @Select("SELECT ticker_symbol AS tickerSymbol, rater_name AS raterName, overall_score AS overallScore, potential_score AS potentialScore FROM stock_multi_rating ORDER BY ticker_symbol, rater_name")
    List<Map<String, Object>> findAll();

    @Insert("INSERT INTO stock_multi_rating (ticker_symbol, rater_name, overall_score, potential_score) VALUES (#{ticker}, #{name}, #{overall}, #{potential}) ON DUPLICATE KEY UPDATE overall_score=VALUES(overall_score), potential_score=VALUES(potential_score)")
    int save(@Param("ticker") String ticker, @Param("name") String name, @Param("overall") BigDecimal overall, @Param("potential") BigDecimal potential);

    @Delete("DELETE FROM stock_multi_rating WHERE ticker_symbol=#{ticker} AND rater_name=#{name}")
    int delete(@Param("ticker") String ticker, @Param("name") String name);
}
