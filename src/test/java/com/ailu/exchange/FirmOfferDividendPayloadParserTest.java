package com.ailu.exchange;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.HistoricalStocksInfoMapper;
import com.ailu.exchange.dao.mapper.source1.StockInfoMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirmOfferDividendPayloadParserTest {

    private static final String INIT_PROPERTY = "stock.import.initializeEmptyDividendData";
    private static final Object[] DIVIDEND_HEADERS = new Object[]{
            "股票代码", "实施每10股送股数", "实施每10股转增股数", "实施每10股派现税前",
            "除权除息日", "实施方案内容", "分红年度", "年度完成日期",
            "年度已实施现金每股(元)", "启用年度完成口径"
    };
    private final List<Path> temporaryFiles = new ArrayList<>();

    @After
    public void cleanUp() throws Exception {
        System.clearProperty(INIT_PROPERTY);
        for (Path path : temporaryFiles) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void mergesBothDividendPayloadsAndSupportsExplicitEmptyPolicy() throws Exception {
        Path workbook = workbook(Arrays.asList(
                row("000001", "0", "0", "3.6", "2026-06-12", "10股派3.6元(含税)", "2025", "20260612", "0.596", "1"),
                row("000001", null, null, null, null, null, "2024", "20250612", "0.608", "1"),
                row("000002", "0", "0", "0.1", "2026-06-30", "10股派0.1元(含税)", "2025", "20260630", "0.01", "1"),
                row("000003", "0", "0", "0.2", "2026-07-01", "10股派0.2元(含税)", "2025", "20260701", "0.02", "1"),
                row("000004", "0", "0", "0.3", "2026-07-02", "10股派0.3元(含税)", "2025", "20260702", "0.03", "1"),
                row("000005", null, null, null, null, null, null, null, null, "1")
        ));

        HistoricalStocksInfo first = historical("000001.SZ",
                "{\"20250620\":[{\"a\":\"0\",\"b\":\"0\",\"m\":\"1\"}]}",
                "{\"20250620\":[0.1]}");
        HistoricalStocksInfo second = historical("000002.SZ", null, null);
        HistoricalStocksInfo third = historical("000003.SZ", null, "{\"20250601\":[0.1]}");
        HistoricalStocksInfo fourth = historical("000004.SZ",
                "{\"20250602\":[{\"a\":\"0\",\"b\":\"0\",\"m\":\"1\"}]}", null);
        HistoricalStocksInfo fifth = historical("000005.SZ", null, null);
        System.setProperty(INIT_PROPERTY, "true");

        List<HistoricalStocksInfo> parsed = parse(workbook,
                Arrays.asList(first, second, third, fourth, fifth),
                stocks("000001.SZ", "000002.SZ", "000003.SZ", "000004.SZ", "000005.SZ"));
        Assert.assertEquals(5, parsed.size());

        HistoricalStocksInfo parsedFirst = find(parsed, "000001.SZ");
        JSONObject firstDetails = JSONObject.parseObject(parsedFirst.getDividendsData());
        Assert.assertTrue(firstDetails.containsKey("20250620"));
        JSONObject newDetail = firstDetails.getJSONArray("20260612").getJSONObject(0);
        assertDecimal("0", newDetail.getBigDecimal("a"));
        assertDecimal("0", newDetail.getBigDecimal("b"));
        assertDecimal("3.6", newDetail.getBigDecimal("m"));
        JSONObject firstDaily = JSONObject.parseObject(parsedFirst.getDailyDividends());
        Assert.assertTrue(firstDaily.containsKey("20250620"));
        JSONObject firstYears = firstDaily.getJSONObject("_completedYears");
        Assert.assertEquals("20250612", firstYears.getJSONObject("2024").getString("completionDate"));
        Assert.assertEquals("20260612", firstYears.getJSONObject("2025").getString("completionDate"));

        HistoricalStocksInfo parsedSecond = find(parsed, "000002.SZ");
        JSONObject secondDetails = JSONObject.parseObject(parsedSecond.getDividendsData());
        JSONObject initializedDetail = secondDetails.getJSONArray("20260630").getJSONObject(0);
        assertDecimal("0", initializedDetail.getBigDecimal("a"));
        assertDecimal("0", initializedDetail.getBigDecimal("b"));
        assertDecimal("0.1", initializedDetail.getBigDecimal("m"));
        Assert.assertEquals("0.01", JSONObject.parseObject(parsedSecond.getDailyDividends())
                .getJSONObject("_completedYears").getJSONObject("2025").getString("cashPerShare"));

        HistoricalStocksInfo parsedThird = find(parsed, "000003.SZ");
        Assert.assertTrue(JSONObject.parseObject(parsedThird.getDividendsData()).containsKey("20260701"));
        Assert.assertTrue(JSONObject.parseObject(parsedThird.getDailyDividends()).containsKey("20250601"));

        HistoricalStocksInfo parsedFourth = find(parsed, "000004.SZ");
        Assert.assertTrue(JSONObject.parseObject(parsedFourth.getDividendsData()).containsKey("20250602"));
        Assert.assertTrue(JSONObject.parseObject(parsedFourth.getDailyDividends())
                .getJSONObject("_completedYears").containsKey("2025"));

        HistoricalStocksInfo parsedFifth = find(parsed, "000005.SZ");
        JSONObject fifthDaily = JSONObject.parseObject(parsedFifth.getDailyDividends());
        Assert.assertTrue(fifthDaily.containsKey("_completedYears"));
        Assert.assertTrue(fifthDaily.getJSONObject("_completedYears").isEmpty());

        printScenario("both_non_empty", first, parsedFirst);
        printScenario("both_null", second, parsedSecond);
        printScenario("dividends_null_daily_non_empty", third, parsedThird);
        printScenario("dividends_non_empty_daily_null", fourth, parsedFourth);
    }

    @Test
    public void rejectsDuplicateCodeYearBeforeWriting() throws Exception {
        Path workbook = workbook(Arrays.asList(
                row("000001", null, null, null, null, null, "2025", "20260612", "0.596", "1"),
                row("000001", null, null, null, null, null, "2025", "20260613", "0.600", "1")
        ));
        assertRejected("duplicate_code_year", workbook, "duplicate completed dividend year");
    }

    @Test
    public void rejectsPartialNegativeAndDisabledMetadata() throws Exception {
        assertRejected("partial_metadata", workbook(Collections.singletonList(
                row("000001", null, null, null, null, null, "2025", null, "0.596", "1")
        )), "invalid completed-year dividend metadata");
        assertRejected("negative_cash_per_share", workbook(Collections.singletonList(
                row("000001", null, null, null, null, null, "2025", "20260612", "-0.1", "1")
        )), "invalid completed-year dividend metadata");
        assertRejected("policy_not_enabled", workbook(Collections.singletonList(
                row("000001", null, null, null, null, null, "2025", "20260612", "0.596", null)
        )), "must explicitly enable policy");
        assertRejected("invalid_completion_date", workbook(Collections.singletonList(
                row("000001", null, null, null, null, null, "2025", "20260230", "0.596", "1")
        )), "invalid completed-year dividend metadata");
        assertRejected("invalid_ex_date", workbook(Collections.singletonList(
                row("000001", "0", "0", "0.596", "20260230", "10股派0.596元(含税)",
                        null, null, null, null)
        )), "invalid dividend ex-date");
    }

    @Test
    public void rejectsMissingDividendColumnDuringPreflight() throws Exception {
        Path path = Files.createTempFile("dividend-missing-column-", ".xlsx");
        temporaryFiles.add(path);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(path)) {
            Sheet sheet = workbook.createSheet("sheet1");
            writeRow(sheet.createRow(0), Arrays.copyOf(DIVIDEND_HEADERS, DIVIDEND_HEADERS.length - 1));
            workbook.write(output);
        }
        Method method = FirmOfferApplicationTests.class.getDeclaredMethod("validateWorkbook",
                FirmOfferApplicationTests.WorkbookProfile.class, Path.class);
        method.setAccessible(true);
        try {
            method.invoke(null, FirmOfferApplicationTests.WorkbookProfile.DIVIDEND_DETAIL, path);
            Assert.fail("expected missing-column rejection");
        } catch (InvocationTargetException exception) {
            Assert.assertTrue(String.valueOf(exception.getCause().getMessage()),
                    exception.getCause().getMessage().contains("must have exactly 10 columns"));
            printRejection("missing_column", exception.getCause());
        }
    }

    private void assertRejected(String caseName, Path workbook, String expectedMessage) throws Exception {
        try {
            parse(workbook, Arrays.asList(historical("000001.SZ", "{}", "{}")), stocks("000001.SZ"));
            Assert.fail("expected parser rejection: " + expectedMessage);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            Assert.assertTrue(String.valueOf(cause.getMessage()), cause.getMessage().contains(expectedMessage));
            printRejection(caseName, cause);
        }
    }

    private static void printScenario(String scenario, HistoricalStocksInfo before, HistoricalStocksInfo after) {
        JSONObject evidence = new JSONObject(true);
        evidence.put("evidenceType", "dividendImportFourScenarioReadback");
        evidence.put("scenario", scenario);
        evidence.put("tickerSymbol", before.getTickerSymbol());
        evidence.put("beforeDividendsData", before.getDividendsData());
        evidence.put("beforeDailyDividends", before.getDailyDividends());
        evidence.put("afterDividendsData", after.getDividendsData());
        evidence.put("afterDailyDividends", after.getDailyDividends());
        evidence.put("sameBatchPayloadContainsBothFields", true);
        evidence.put("pass", true);
        System.out.println(evidence.toJSONString());
    }

    private static void printRejection(String caseName, Throwable cause) {
        JSONObject evidence = new JSONObject(true);
        evidence.put("evidenceType", "dividendImportRejection");
        evidence.put("case", caseName);
        evidence.put("exceptionType", cause.getClass().getName());
        evidence.put("exceptionMessage", cause.getMessage());
        evidence.put("dmlInvoked", false);
        evidence.put("databaseStateBefore", "not_connected_parser_gate");
        evidence.put("databaseStateAfter", "not_connected_parser_gate");
        evidence.put("pass", true);
        System.out.println(evidence.toJSONString());
    }

    @SuppressWarnings("unchecked")
    private List<HistoricalStocksInfo> parse(Path workbook, List<HistoricalStocksInfo> existing,
                                             List<StockInfo> stockInfos) throws Exception {
        HistoricalStocksInfoMapper historicalMapper = (HistoricalStocksInfoMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{HistoricalStocksInfoMapper.class},
                (proxy, method, args) -> {
                    if ("findAllDividendPayloads".equals(method.getName())) return existing;
                    throw new UnsupportedOperationException(method.getName());
                });
        StockInfoMapper stockMapper = (StockInfoMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{StockInfoMapper.class},
                (proxy, method, args) -> {
                    if ("getAllTickerSymbol".equals(method.getName())) return stockInfos;
                    throw new UnsupportedOperationException(method.getName());
                });
        FirmOfferApplicationTests.ImportReceipt receipt = new FirmOfferApplicationTests.ImportReceipt();
        Method method = FirmOfferApplicationTests.class.getDeclaredMethod("parseDividendDetail",
                Path.class, HistoricalStocksInfoMapper.class, StockInfoMapper.class,
                FirmOfferApplicationTests.ImportReceipt.class);
        method.setAccessible(true);
        return (List<HistoricalStocksInfo>) method.invoke(null, workbook, historicalMapper, stockMapper, receipt);
    }

    private Path workbook(List<Object[]> rows) throws Exception {
        Path path = Files.createTempFile("dividend-completed-year-", ".xlsx");
        temporaryFiles.add(path);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(path)) {
            Sheet sheet = workbook.createSheet("sheet1");
            writeRow(sheet.createRow(0), DIVIDEND_HEADERS);
            for (int index = 0; index < rows.size(); index++) {
                writeRow(sheet.createRow(index + 1), rows.get(index));
            }
            workbook.write(output);
        }
        return path;
    }

    private static void writeRow(Row row, Object[] values) {
        for (int index = 0; index < values.length; index++) {
            Object value = values[index];
            if (value != null) row.createCell(index).setCellValue(String.valueOf(value));
        }
    }

    private static Object[] row(Object... values) {
        return values;
    }

    private static HistoricalStocksInfo historical(String code, String dividendsData, String dailyDividends) {
        HistoricalStocksInfo info = new HistoricalStocksInfo();
        info.setTickerSymbol(code);
        info.setDividendsData(dividendsData);
        info.setDailyDividends(dailyDividends);
        return info;
    }

    private static List<StockInfo> stocks(String... codes) {
        List<StockInfo> rows = new ArrayList<>();
        for (String code : codes) {
            StockInfo info = new StockInfo();
            info.setTickerSymbol(code);
            rows.add(info);
        }
        return rows;
    }

    private static HistoricalStocksInfo find(List<HistoricalStocksInfo> rows, String code) {
        for (HistoricalStocksInfo row : rows) {
            if (code.equals(row.getTickerSymbol())) return row;
        }
        Assert.fail("missing parsed row: " + code);
        return null;
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        Assert.assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
