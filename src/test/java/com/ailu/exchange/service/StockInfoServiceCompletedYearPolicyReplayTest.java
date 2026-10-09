package com.ailu.exchange.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockInfoServiceCompletedYearPolicyReplayTest {

    private static final String POLICY_PROPERTY = "stock.test.completedYearPolicy";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> REQUIRED_CODES = Arrays.asList(
            "601838", "600036", "000001", "601398", "601939",
            "601318", "603259", "002352", "002955");

    @Test
    public void replaysRealPolicyCompletionBoundaries() throws Exception {
        String path = System.getProperty(POLICY_PROPERTY);
        Assert.assertNotNull("missing " + POLICY_PROPERTY, path);
        JSONObject policy = JSON.parseObject(new String(
                Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8));
        JSONArray records = policy.getJSONArray("records");
        Map<String, List<JSONObject>> byCode = new LinkedHashMap<>();
        for (int index = 0; index < records.size(); index++) {
            JSONObject record = records.getJSONObject(index);
            String code = record.getString("code");
            if (REQUIRED_CODES.contains(code)) {
                byCode.computeIfAbsent(code, key -> new ArrayList<>()).add(record);
            }
        }

        Method method = StockInfoService.class.getDeclaredMethod(
                "calculateCompletedYearDividend", String.class, BigDecimal.class, JSONObject.class);
        method.setAccessible(true);
        StockInfoService service = new StockInfoService();
        int assertions = 0;
        for (String code : REQUIRED_CODES) {
            List<JSONObject> codeRecords = byCode.get(code);
            Assert.assertNotNull("missing policy for " + code, codeRecords);
            codeRecords.sort(Comparator.comparing(row -> row.getString("completion_date")));
            JSONObject completedYears = completedYears(codeRecords);

            for (int recordIndex = 0; recordIndex < codeRecords.size(); recordIndex++) {
                JSONObject record = codeRecords.get(recordIndex);
                String completionDate = record.getString("completion_date");
                String priorDate = LocalDate.parse(completionDate, DATE_FORMATTER)
                        .minusDays(1).format(DATE_FORMATTER);
                String previousDayType = recordIndex == 0
                        ? "first_completion_previous_day" : "next_completion_previous_day";
                String completionDayType = recordIndex == 0
                        ? "first_completion_day" : "next_completion_day";
                assertions += assertSelection(service, method, code, priorDate, completedYears, codeRecords,
                        previousDayType, record.getIntValue("year"), completionDate);
                assertions += assertSelection(service, method, code, completionDate, completedYears, codeRecords,
                        completionDayType, record.getIntValue("year"), completionDate);
            }
        }
        Assert.assertEquals("sample count", REQUIRED_CODES.size(), byCode.size());
        Assert.assertTrue("expected broad boundary coverage", assertions >= 50);
        System.out.println("completedYearPolicyReplay samples=" + REQUIRED_CODES.size()
                + " assertions=" + assertions + " mismatches=0");
    }

    private static JSONObject completedYears(List<JSONObject> records) {
        JSONObject result = new JSONObject();
        for (JSONObject record : records) {
            JSONObject value = new JSONObject();
            value.put("completionDate", record.getString("completion_date"));
            value.put("cashPerShare", record.getBigDecimal("annual_cash_per_share"));
            result.put(record.getString("year"), value);
        }
        return result;
    }

    private static int assertSelection(StockInfoService service, Method method, String code, String tradeDate,
                                       JSONObject completedYears, List<JSONObject> records, String boundaryType,
                                       int targetCompletionYear, String targetCompletionDate) throws Exception {
        JSONObject expected = null;
        for (JSONObject record : records) {
            String completionDate = record.getString("completion_date");
            if (completionDate.compareTo(tradeDate) > 0) {
                continue;
            }
            if (expected == null
                    || completionDate.compareTo(expected.getString("completion_date")) > 0
                    || (completionDate.equals(expected.getString("completion_date"))
                    && record.getIntValue("year") > expected.getIntValue("year"))) {
                expected = record;
            }
        }
        BigDecimal actual = (BigDecimal) method.invoke(service, tradeDate, new BigDecimal("10"), completedYears);
        BigDecimal expectedYield = expected == null ? BigDecimal.ZERO
                : expected.getBigDecimal("annual_cash_per_share").divide(new BigDecimal("10"), 6,
                BigDecimal.ROUND_HALF_UP);
        Assert.assertEquals(code + " date=" + tradeDate, 0, expectedYield.compareTo(actual));

        JSONObject evidence = new JSONObject(true);
        evidence.put("code", code);
        evidence.put("tradeDate", tradeDate);
        evidence.put("boundaryType", boundaryType);
        evidence.put("targetCompletionYear", targetCompletionYear);
        evidence.put("targetCompletionDate", targetCompletionDate);
        evidence.put("expectedYear", expected == null ? null : expected.getIntValue("year"));
        evidence.put("expectedYieldAtClose10", expectedYield.toPlainString());
        evidence.put("actualYieldAtClose10", actual.toPlainString());
        evidence.put("pass", true);
        System.out.println(evidence.toJSONString());
        return 1;
    }
}
