package com.ailu.exchange.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class StockInfoServiceSpecialCashPolicyTest {

    private static final String POLICY_PROPERTY = "stock.test.completedYearPolicy";
    private static final String SPECIAL_EVIDENCE_PROPERTY = "stock.test.specialCashEvidence";

    @Test
    public void p2799CashEventsAreIncludedButDoNotCompleteFiscalYear() throws Exception {
        String policyPath = System.getProperty(POLICY_PROPERTY);
        String evidencePath = System.getProperty(SPECIAL_EVIDENCE_PROPERTY);
        Assert.assertNotNull("missing " + POLICY_PROPERTY, policyPath);
        Assert.assertNotNull("missing " + SPECIAL_EVIDENCE_PROPERTY, evidencePath);

        JSONObject policy = read(policyPath);
        JSONObject specialEvidence = read(evidencePath);
        Map<String, JSONObject> policyByCodeYear = new HashMap<>();
        JSONArray records = policy.getJSONArray("records");
        for (int index = 0; index < records.size(); index++) {
            JSONObject record = records.getJSONObject(index);
            policyByCodeYear.put(record.getString("code") + "|" + record.getIntValue("year"), record);
        }

        JSONArray rows = specialEvidence.getJSONArray("rows");
        Assert.assertEquals("verified P2799 cash event count", 14, rows.size());
        int assertions = 0;
        for (int index = 0; index < rows.size(); index++) {
            JSONObject row = rows.getJSONObject(index);
            Assert.assertEquals("P2799", row.getString("term"));
            Assert.assertEquals("implemented_cash_special_verified", row.getString("status"));
            Assert.assertEquals("count_in_source_dividend_year_cash_total", row.getString("fiscal_treatment"));
            Assert.assertEquals("not_a_year_completion_trigger", row.getString("completion_treatment"));

            String code = row.getString("code");
            int year = row.getIntValue("year");
            String exDate = row.getString("ex").replace("-", "");
            BigDecimal cashPer10 = row.getBigDecimal("m");
            JSONObject policyRecord = policyByCodeYear.get(code + "|" + year);
            Assert.assertNotNull("missing completed-year policy " + code + " " + year, policyRecord);

            JSONArray events = policyRecord.getJSONArray("implemented_cash_events");
            JSONObject matchedSpecial = null;
            BigDecimal annualSumPerShare = BigDecimal.ZERO;
            for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
                JSONObject event = events.getJSONObject(eventIndex);
                annualSumPerShare = annualSumPerShare.add(event.getBigDecimal("cash_per_share"));
                if ("特别/临时分红".equals(event.getString("period"))
                        && exDate.equals(event.getString("ex_date"))
                        && cashPer10.compareTo(event.getBigDecimal("cash_per_10")) == 0) {
                    matchedSpecial = event;
                }
            }
            Assert.assertNotNull("special event not included " + code + " " + year, matchedSpecial);
            Assert.assertEquals("annual cash sum " + code + " " + year, 0,
                    annualSumPerShare.compareTo(policyRecord.getBigDecimal("annual_cash_per_share")));
            String completionType = policyRecord.getString("completion_type");
            Assert.assertTrue("special event must not complete year " + code + " " + year,
                    "annual_implementation".equals(completionType)
                            || "shareholder_resolution_no_final_distribution".equals(completionType));

            JSONObject output = new JSONObject(true);
            output.put("evidenceType", "p2799SpecialCashPolicy");
            output.put("code", code);
            output.put("year", year);
            output.put("specialExDate", exDate);
            output.put("specialCashPer10", cashPer10.toPlainString());
            output.put("includedInAnnualCash", true);
            output.put("annualCashPerShare", annualSumPerShare.toPlainString());
            output.put("completionDate", policyRecord.getString("completion_date"));
            output.put("completionType", completionType);
            output.put("specialDoesNotDefineCompletion", true);
            output.put("pass", true);
            System.out.println(output.toJSONString());
            assertions++;
        }
        System.out.println("p2799SpecialCashPolicy events=" + rows.size()
                + " assertions=" + assertions + " mismatches=0");
    }

    private static JSONObject read(String path) throws Exception {
        return JSON.parseObject(new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8));
    }
}
