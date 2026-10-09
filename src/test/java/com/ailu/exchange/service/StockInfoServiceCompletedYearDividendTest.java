package com.ailu.exchange.service;

import com.ailu.exchange.dao.vo.DailyVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class StockInfoServiceCompletedYearDividendTest {

    private final StockInfoService service = new StockInfoService();

    @Test
    public void switchesOnlyOnCompletionDate() throws Exception {
        JSONObject completedYears = new JSONObject();
        completedYears.put("2023", completedYear("20240614", "0.719"));
        completedYears.put("2024", completedYear("20250612", "0.608"));
        completedYears.put("2025", completedYear("20260612", "0.596"));

        assertDecimal("0", completedYield("20240613", "10", completedYears));
        assertDecimal("0.071900", completedYield("20240614", "10", completedYears));
        assertDecimal("0.071900", completedYield("20250611", "10", completedYears));
        assertDecimal("0.060800", completedYield("20250612", "10", completedYears));
        assertDecimal("0.060800", completedYield("20260611", "10", completedYears));
        assertDecimal("0.059600", completedYield("20260612", "10", completedYears));
    }

    @Test
    public void picksLaterFiscalYearWhenCompletionDatesTie() throws Exception {
        JSONObject completedYears = new JSONObject();
        completedYears.put("2024", completedYear("20260612", "0.608"));
        completedYears.put("2025", completedYear("20260612", "0.596"));

        assertDecimal("0.059600", completedYield("20260612", "10", completedYears));
    }

    @Test
    public void usesCashPerShareAndCloseInsteadOfMarketCap() throws Exception {
        JSONObject completedYears = new JSONObject();
        completedYears.put("2025", completedYear("20260710", "1.003"));

        assertDecimal("0.024571", completedYield("20260710", "40.82", completedYears));
    }

    @Test
    public void emptyPolicyDisablesLegacyFallback() throws Exception {
        Map<String, JSONArray> legacyAmounts = new HashMap<>();
        JSONArray amounts = new JSONArray();
        amounts.add(new BigDecimal("1000"));
        legacyAmounts.put("20240101", amounts);

        JSONObject explicitEmptyPolicy = new JSONObject();
        explicitEmptyPolicy.put("_completedYears", new JSONObject());
        assertDecimal("0", calculatedYield("20250102", legacyAmounts, "100000", "10", explicitEmptyPolicy));

        assertDecimal("0.010000", calculatedYield("20250102", legacyAmounts, "100000", "10", new JSONObject()));
    }

    @Test
    public void ignoresInvalidMetadataAndNonPositiveClose() throws Exception {
        JSONObject completedYears = new JSONObject();
        completedYears.put("bad-year", completedYear("20260612", "1"));
        completedYears.put("2025", completedYear("bad-date", "1"));
        assertDecimal("0", completedYield("20260612", "10", completedYears));
        assertDecimal("0", completedYield("20260612", "0", new JSONObject()));
        assertDecimal("0", completedYield("20260612", null, new JSONObject()));
    }

    @Test
    public void adjustmentBoundaryKeepsStrictFutureRule() throws Exception {
        DailyVo daily = new DailyVo();
        daily.setTradeDate("20260612");
        daily.setOpen(new BigDecimal("10"));
        daily.setHigh(new BigDecimal("10"));
        daily.setLow(new BigDecimal("10"));
        daily.setClose(new BigDecimal("10"));
        Map<String, BigDecimal> factors = new HashMap<>();
        factors.put("20260612", new BigDecimal("1.1"));
        factors.put("20270101", new BigDecimal("1.2"));

        Method method = StockInfoService.class.getDeclaredMethod(
                "getNewKXian", DailyVo.class, Map.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JSONArray> adjusted = (Map<String, JSONArray>) method.invoke(service, daily, factors, "20260612");

        assertDecimal("11.0000", adjusted.get("dataHfq").getBigDecimal(4));
        assertDecimal("8.3330", adjusted.get("dataQfq").getBigDecimal(4));
    }

    private BigDecimal completedYield(String tradeDate, String close, JSONObject completedYears) throws Exception {
        Method method = StockInfoService.class.getDeclaredMethod(
                "calculateCompletedYearDividend", String.class, BigDecimal.class, JSONObject.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, tradeDate,
                close == null ? null : new BigDecimal(close), completedYears);
    }

    private BigDecimal calculatedYield(String tradeDate, Map<String, JSONArray> legacyAmounts,
                                       String marketCap, String close, JSONObject dailyDividends) throws Exception {
        Method method = StockInfoService.class.getDeclaredMethod(
                "calculateDividend", String.class, Map.class, BigDecimal.class, BigDecimal.class, JSONObject.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, tradeDate, legacyAmounts,
                new BigDecimal(marketCap), new BigDecimal(close), dailyDividends);
    }

    private static JSONObject completedYear(String date, String cashPerShare) {
        JSONObject value = new JSONObject();
        value.put("completionDate", date);
        value.put("cashPerShare", new BigDecimal(cashPerShare));
        return value;
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        Assert.assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
