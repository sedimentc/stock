package com.ailu.exchange.service;

import com.ailu.exchange.dao.vo.DailyVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockInfoServiceAdjustmentRegressionTest {

    private static final String AUDIT_DIR_PROPERTY = "stock.test.adjustmentAuditDir";

    @Test
    public void matchesCapturedBankAndInsuranceAdjustmentSeries() throws Exception {
        String configured = System.getProperty(AUDIT_DIR_PROPERTY);
        Assert.assertNotNull("missing " + AUDIT_DIR_PROPERTY, configured);
        Path auditDir = Paths.get(configured);
        JSONObject dividendsByCode = readObject(auditDir.resolve("dividends-db.json"));

        List<String> codes = new ArrayList<>(dividendsByCode.keySet());
        Collections.sort(codes);
        Assert.assertTrue("expected at least 10 bank/insurance samples", codes.size() >= 10);

        Method fictionalKLine = StockInfoService.class.getDeclaredMethod(
                "fictionalKLine", JSONObject.class, Map.class, List.class);
        fictionalKLine.setAccessible(true);
        Method factorMethod = StockInfoService.class.getDeclaredMethod(
                "getDividendsDataJson", List.class, JSONObject.class);
        factorMethod.setAccessible(true);
        Method adjustmentMethod = StockInfoService.class.getDeclaredMethod(
                "getNewKXian", DailyVo.class, Map.class, String.class);
        adjustmentMethod.setAccessible(true);

        StockInfoService service = new StockInfoService();
        int totalRows = 0;
        for (String code : codes) {
            JSONObject response = readObject(auditDir.resolve(code + ".json"));
            JSONObject data = response.getJSONObject("data");
            Assert.assertNotNull("missing API data for " + code, data);

            List<DailyVo> dailyRows = dailyRows(data.getJSONArray("list"));
            Map<String, DailyVo> groupedByDate = new HashMap<>();
            for (DailyVo row : dailyRows) {
                groupedByDate.putIfAbsent(row.getTradeDate(), row);
            }
            JSONObject dividends = dividendsByCode.getJSONObject(code);
            List<String> dividendDates = new ArrayList<>(dividends.keySet());
            Collections.sort(dividendDates);
            @SuppressWarnings("unchecked")
            List<DailyVo> fictionalRows = (List<DailyVo>) fictionalKLine.invoke(
                    null, dividends, groupedByDate, dividendDates);
            dailyRows.addAll(fictionalRows);
            dailyRows.sort(Comparator.comparing(DailyVo::getTradeDate).reversed());

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> factors = (Map<String, BigDecimal>) factorMethod.invoke(
                    service, dailyRows, dividends);
            Map<String, JSONArray> expectedQfq = byDate(data.getJSONArray("preAdjustedPrice"));
            Map<String, JSONArray> expectedHfq = byDate(data.getJSONArray("postAdjustedPrice"));
            Assert.assertEquals("QFQ row count for " + code, dailyRows.size(), expectedQfq.size());
            Assert.assertEquals("HFQ row count for " + code, dailyRows.size(), expectedHfq.size());

            for (DailyVo row : dailyRows) {
                @SuppressWarnings("unchecked")
                Map<String, JSONArray> actual = (Map<String, JSONArray>) adjustmentMethod.invoke(
                        service, row, factors, row.getTradeDate());
                String date = hyphenated(row.getTradeDate());
                assertPriceRow(code + " QFQ " + date, expectedQfq.get(date), actual.get("dataQfq"));
                assertPriceRow(code + " HFQ " + date, expectedHfq.get(date), actual.get("dataHfq"));
                totalRows++;
            }
            if ("600036".equals(code)) {
                printKeyPointEvidence(service, adjustmentMethod, dailyRows, factors, dividends,
                        expectedQfq, expectedHfq, "20250711", "20260116", "20260710");
            }
            System.out.println("adjustmentRegression code=" + code
                    + " rows=" + dailyRows.size() + " factors=" + factors.size() + " mismatches=0");
        }
        System.out.println("adjustmentRegression samples=" + codes.size()
                + " totalRows=" + totalRows + " mismatches=0");
    }

    @SuppressWarnings("unchecked")
    private static void printKeyPointEvidence(StockInfoService service, Method adjustmentMethod,
                                              List<DailyVo> dailyRows, Map<String, BigDecimal> factors,
                                              JSONObject dividends, Map<String, JSONArray> expectedQfq,
                                              Map<String, JSONArray> expectedHfq, String... dates) throws Exception {
        for (String date : dates) {
            int rowIndex = -1;
            for (int index = 0; index < dailyRows.size(); index++) {
                if (date.equals(dailyRows.get(index).getTradeDate())) {
                    rowIndex = index;
                    break;
                }
            }
            Assert.assertTrue("missing key ex-date row " + date, rowIndex >= 0);
            Assert.assertTrue("missing previous close for " + date, rowIndex + 1 < dailyRows.size());
            DailyVo row = dailyRows.get(rowIndex);
            BigDecimal previousClose = dailyRows.get(rowIndex + 1).getClose();
            JSONArray detailRows = dividends.getJSONArray(date);
            Assert.assertNotNull("missing dividend detail " + date, detailRows);
            BigDecimal totalA = BigDecimal.ZERO;
            BigDecimal totalB = BigDecimal.ZERO;
            BigDecimal totalM = BigDecimal.ZERO;
            for (int index = 0; index < detailRows.size(); index++) {
                JSONObject detail = detailRows.getJSONObject(index);
                totalA = totalA.add(detail.getBigDecimal("a"));
                totalB = totalB.add(detail.getBigDecimal("b"));
                totalM = totalM.add(detail.getBigDecimal("m"));
            }
            BigDecimal ten = BigDecimal.TEN;
            BigDecimal formulaFactor = previousClose.divide(
                    previousClose.subtract(totalM.divide(ten, 4, RoundingMode.HALF_UP)),
                    4, RoundingMode.HALF_UP);
            if (totalA.compareTo(BigDecimal.ZERO) != 0 || totalB.compareTo(BigDecimal.ZERO) != 0) {
                formulaFactor = formulaFactor.multiply(
                        ten.add(totalA).add(totalB).divide(ten, 4, RoundingMode.HALF_UP));
            }
            Assert.assertEquals("factor formula " + date, 0, formulaFactor.compareTo(factors.get(date)));

            Map<String, JSONArray> actual = (Map<String, JSONArray>) adjustmentMethod.invoke(
                    service, row, factors, date);
            String hyphenatedDate = hyphenated(date);
            assertPriceRow("600036 key QFQ " + date, expectedQfq.get(hyphenatedDate), actual.get("dataQfq"));
            assertPriceRow("600036 key HFQ " + date, expectedHfq.get(hyphenatedDate), actual.get("dataHfq"));

            JSONObject evidence = new JSONObject(true);
            evidence.put("evidenceType", "adjustmentKeyPoint");
            evidence.put("code", "600036");
            evidence.put("exDate", date);
            evidence.put("previousClose", previousClose.toPlainString());
            evidence.put("aPer10", totalA.toPlainString());
            evidence.put("bPer10", totalB.toPlainString());
            evidence.put("cashPer10", totalM.toPlainString());
            evidence.put("formulaFactor", formulaFactor.toPlainString());
            evidence.put("serviceFactor", factors.get(date).toPlainString());
            evidence.put("rawOhlc", priceArray(row));
            evidence.put("expectedQfqOhlc", expectedQfq.get(hyphenatedDate));
            evidence.put("actualQfqOhlc", actual.get("dataQfq"));
            evidence.put("expectedHfqOhlc", expectedHfq.get(hyphenatedDate));
            evidence.put("actualHfqOhlc", actual.get("dataHfq"));
            evidence.put("pass", true);
            System.out.println(evidence.toJSONString());
        }
    }

    private static JSONArray priceArray(DailyVo row) {
        JSONArray result = new JSONArray();
        result.add(row.getOpen());
        result.add(row.getHigh());
        result.add(row.getLow());
        result.add(row.getClose());
        return result;
    }

    private static JSONObject readObject(Path path) throws Exception {
        return JSON.parseObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static List<DailyVo> dailyRows(JSONArray rows) {
        List<DailyVo> result = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            JSONArray row = rows.getJSONArray(index);
            String tradeDate = row.getString(0).replace("-", "");
            result.add(new DailyVo(row.getBigDecimal(1), row.getBigDecimal(2), row.getBigDecimal(3),
                    row.getBigDecimal(4), tradeDate));
        }
        result.sort(Comparator.comparing(DailyVo::getTradeDate).reversed());
        return result;
    }

    private static Map<String, JSONArray> byDate(JSONArray rows) {
        Map<String, JSONArray> result = new LinkedHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            JSONArray row = rows.getJSONArray(index);
            result.put(row.getString(0), row);
        }
        return result;
    }

    private static void assertPriceRow(String label, JSONArray expected, JSONArray actual) {
        Assert.assertNotNull("missing expected row " + label, expected);
        Assert.assertNotNull("missing actual row " + label, actual);
        for (int column = 1; column <= 4; column++) {
            Assert.assertEquals(label + " column=" + column, 0,
                    expected.getBigDecimal(column).compareTo(actual.getBigDecimal(column)));
        }
    }

    private static String hyphenated(String value) {
        return value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8);
    }
}
