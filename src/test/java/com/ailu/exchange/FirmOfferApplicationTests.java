package com.ailu.exchange;

import com.ailu.exchange.config.DataSource1Config;
import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.HistoricalStocksInfoMapper;
import com.ailu.exchange.dao.mapper.source1.StockInfoMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class FirmOfferApplicationTests {

    private static final int BATCH_SIZE = 500;
    private static final String CONFIRM_PROPERTY = "stock.import.confirm";
    private static final String CONFIRM_VALUE = "APPLY_DB1_JUDGE_MUTI";
    private static final String EXPECTED_BUNDLE_PROPERTY = "stock.import.expectedBundleSha256";
    private static final String BACKUP_MANIFEST_PROPERTY = "stock.import.backupManifest";
    private static final String FAIL_AFTER_BATCHES_PROPERTY = "stock.import.failAfterBatches";
    private static final String INITIALIZE_EMPTY_DIVIDEND_PROPERTY =
            "stock.import.initializeEmptyDividendData";
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.CHINA);
    private static final Pattern SHA256_PATTERN = Pattern.compile("[a-fA-F0-9]{64}");
    private static final DateTimeFormatter STRICT_DATE_KEY_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static int completedBatches;

    @Test
    public void validateSevenWorkbooks() throws Exception {
        SevenWorkbookPreflight preflight = preflightSevenWorkbooks();
        printValidationReceipt(preflight);
    }

    @Test
    public void importSevenWorkbooks() throws Exception {
        completedBatches = 0;
        ImportGate gate = ImportGate.failClosedFromSystemProperties();
        SevenWorkbookPreflight preflight = preflightSevenWorkbooks();
        gate.requireExpectedBundle(preflight.getSourceBundleSha256());
        preflight.requireFilesUnchanged();

        ImportReceipt receipt = new ImportReceipt();
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Source1ImportContext.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run()) {
            HistoricalStocksInfoMapper historicalMapper = context.getBean(HistoricalStocksInfoMapper.class);
            StockInfoMapper stockMapper = context.getBean(StockInfoMapper.class);
            DataSource dataSource = context.getBean("source1DataSource", DataSource.class);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            PlatformTransactionManager transactionManager = context.getBean("test1TransactionManager",
                    PlatformTransactionManager.class);
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    try {
                        assertDb1ImportGuard(jdbcTemplate, gate.manifest);
                        importSevenWorkbooksInDb1Transaction(preflight, historicalMapper, stockMapper, receipt);
                    } catch (Exception ex) {
                        throw new IllegalStateException("stock excel import failed; DB1 transaction must roll back", ex);
                    }
                    return null;
                }
            });
            receipt.transactionStatus = "COMMITTED";
            printImportReceipt(preflight, receipt);
        } catch (RuntimeException ex) {
            receipt.transactionStatus = "ROLLED_BACK";
            printImportReceipt(preflight, receipt);
            throw ex;
        }
    }

    @Configuration
    @EnableConfigurationProperties
    @Import(DataSource1Config.class)
    static class Source1ImportContext {
    }

    private static SevenWorkbookPreflight preflightSevenWorkbooks() throws Exception {
        Map<WorkbookProfile, WorkbookProfileResult> results = new LinkedHashMap<>();
        for (WorkbookProfile profile : WorkbookProfile.values()) {
            Path path = requireWorkbookPath(profile);
            results.put(profile, validateWorkbook(profile, path));
        }
        return new SevenWorkbookPreflight(results, sourceBundleSha256(results));
    }

    private static Path requireWorkbookPath(WorkbookProfile profile) {
        String configured = System.getProperty(profile.propertyName);
        if (StringUtils.isBlank(configured)) {
            throw new IllegalStateException("missing required system property: " + profile.propertyName);
        }
        Path path = Paths.get(configured);
        File file = path.toFile();
        if (!file.isFile()) {
            throw new IllegalStateException(profile.propertyName + " must point to a real xlsx file");
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalStateException(profile.propertyName + " must point to an .xlsx file");
        }
        return path;
    }

    private static WorkbookProfileResult validateWorkbook(WorkbookProfile profile, Path path) throws Exception {
        String fileSha256 = sha256(path);
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() < 1) {
                throw new IllegalStateException(profile.label + " has no sheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalStateException(profile.label + " missing header row");
            }
            validateHeader(profile, header);
            int dataRows = countDataRows(sheet, 1);
            if (dataRows <= 0) {
                throw new IllegalStateException(profile.label + " has no data row");
            }
            return new WorkbookProfileResult(profile, fileSha256, dataRows);
        }
    }

    private static void validateHeader(WorkbookProfile profile, Row header) {
        int lastCellNum = Math.max(header.getLastCellNum(), 0);
        if (profile.fixedColumnCount > 0 && lastCellNum != profile.fixedColumnCount) {
            throw new IllegalStateException(profile.label + " must have exactly " + profile.fixedColumnCount
                    + " columns, actual=" + lastCellNum);
        }
        Set<String> headerTexts = new LinkedHashSet<>();
        int upperBound = profile.fixedColumnCount > 0 ? profile.fixedColumnCount : lastCellNum;
        for (int i = 0; i < upperBound; i++) {
            String text = cellText(header.getCell(i));
            if (StringUtils.isBlank(text)) {
                throw new IllegalStateException(profile.label + " has blank header at column " + i);
            }
            if (profile.fixedColumnCount > 0 && profile.requiredHeaderTokens.size() == profile.fixedColumnCount) {
                String expectedToken = profile.requiredHeaderTokens.get(i);
                if (!text.contains(expectedToken)) {
                    throw new IllegalStateException(profile.label + " header column " + i
                            + " must contain " + expectedToken + ", actual=" + text);
                }
            }
            headerTexts.add(text);
        }
        if (profile.fixedColumnCount > 0 && profile.requiredHeaderTokens.size() == profile.fixedColumnCount) {
            return;
        }
        for (String requiredToken : profile.requiredHeaderTokens) {
            boolean found = false;
            for (String headerText : headerTexts) {
                if (headerText.contains(requiredToken)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException(profile.label + " missing header token: " + requiredToken);
            }
        }
        switch (profile) {
            case REVENUE_PROFIT:
                collectColumns(header, "营业总收入");
                collectColumns(header, "净利润");
                break;
            case BVPS:
                collectColumns(header, "每股净资产");
                break;
            case NET_ASSETS:
                collectColumns(header, "净资产");
                break;
            case ANNUAL_DIVIDEND:
                collectYearColumns(header, "现金分红总额");
                break;
            case DISCLOSURE_DATE:
                collectColumns(header, "实际披露日期");
                break;
            default:
                break;
        }
    }

    private static int countDataRows(Sheet sheet, int startRow) {
        int count = 0;
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && !rowIsBlank(row)) {
                count++;
            }
        }
        return count;
    }

    private static boolean rowIsBlank(Row row) {
        for (int i = row.getFirstCellNum(); i >= 0 && i < row.getLastCellNum(); i++) {
            if (StringUtils.isNotBlank(cellText(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private static void importSevenWorkbooksInDb1Transaction(SevenWorkbookPreflight preflight,
                                                            HistoricalStocksInfoMapper historicalMapper,
                                                            StockInfoMapper stockMapper,
                                                            ImportReceipt receipt) throws Exception {
        writeHistoricalBatches(WorkbookProfile.REVENUE_PROFIT,
                parseRevenueProfit(profilePath(preflight, WorkbookProfile.REVENUE_PROFIT), historicalMapper),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdate(batch);
                    }
                }, receipt);

        writeHistoricalBatches(WorkbookProfile.BVPS,
                parseBvps(profilePath(preflight, WorkbookProfile.BVPS), historicalMapper),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdateByBvpsData(batch);
                    }
                }, receipt);

        writeHistoricalBatches(WorkbookProfile.NET_ASSETS,
                parseNetAssets(profilePath(preflight, WorkbookProfile.NET_ASSETS), historicalMapper),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdateByNetAssets(batch);
                    }
                }, receipt);

        writeHistoricalBatches(WorkbookProfile.ANNUAL_DIVIDEND,
                parseAnnualDividend(profilePath(preflight, WorkbookProfile.ANNUAL_DIVIDEND), historicalMapper),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdate2(batch);
                    }
                }, receipt);

        writeStockBatches(WorkbookProfile.STOCK_BASIC,
                parseStockBasic(profilePath(preflight, WorkbookProfile.STOCK_BASIC), stockMapper),
                new StockBatchWriter() {
                    @Override
                    public void write(List<StockInfo> batch) {
                        stockMapper.batchInsertOrUpdate(batch);
                    }
                }, receipt);

        writeHistoricalBatches(WorkbookProfile.DISCLOSURE_DATE,
                parseDisclosureDate(profilePath(preflight, WorkbookProfile.DISCLOSURE_DATE), historicalMapper),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdate4(batch);
                    }
                }, receipt);

        writeHistoricalBatches(WorkbookProfile.DIVIDEND_DETAIL,
                parseDividendDetail(profilePath(preflight, WorkbookProfile.DIVIDEND_DETAIL), historicalMapper, stockMapper,
                        receipt),
                new HistoricalBatchWriter() {
                    @Override
                    public void write(List<HistoricalStocksInfo> batch) {
                        historicalMapper.batchInsertOrUpdateDividendPayloads(batch);
                    }
                }, receipt);
    }

    private static Path profilePath(SevenWorkbookPreflight preflight, WorkbookProfile profile) {
        return preflight.getResult(profile).path;
    }

    private static List<HistoricalStocksInfo> parseRevenueProfit(Path path,
                                                                HistoricalStocksInfoMapper historicalMapper)
            throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllNew());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderColumns revenueColumns = collectColumns(sheet.getRow(0), "营业总收入");
            HeaderColumns profitColumns = collectColumns(sheet.getRow(0), "净利润");
            List<HistoricalStocksInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol)) {
                        return;
                    }
                    JSONObject revenue = jsonFromNumericColumns(row, revenueColumns, false);
                    JSONObject profit = jsonFromNumericColumns(row, profitColumns, false);
                    HistoricalStocksInfo old = existing.get(tickerSymbol);
                    HistoricalStocksInfo info = new HistoricalStocksInfo();
                    info.setTickerSymbol(tickerSymbol);
                    info.setTickerName(tickerName);
                    info.setGrossRevenue(mergeJson(old == null ? null : old.getGrossRevenue(), revenue).toJSONString());
                    info.setNetProfit(mergeJson(old == null ? null : old.getNetProfit(), profit).toJSONString());
                    rows.add(info);
                }
            });
            return rows;
        }
    }

    private static List<HistoricalStocksInfo> parseBvps(Path path,
                                                       HistoricalStocksInfoMapper historicalMapper) throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllBvpsData());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderColumns columns = collectColumns(sheet.getRow(0), "每股净资产");
            List<HistoricalStocksInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol)) {
                        return;
                    }
                    JSONObject bvps = jsonFromNumericColumns(row, columns, false);
                    HistoricalStocksInfo old = existing.get(tickerSymbol);
                    HistoricalStocksInfo info = new HistoricalStocksInfo();
                    info.setTickerSymbol(tickerSymbol);
                    info.setTickerName(tickerName);
                    info.setBvpsData(mergeJson(old == null ? null : old.getBvpsData(), bvps).toJSONString());
                    rows.add(info);
                }
            });
            return rows;
        }
    }

    private static List<HistoricalStocksInfo> parseNetAssets(Path path,
                                                            HistoricalStocksInfoMapper historicalMapper) throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllNetAssets());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderColumns columns = collectColumns(sheet.getRow(0), "净资产");
            List<HistoricalStocksInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol)) {
                        return;
                    }
                    JSONObject netAssets = jsonFromNumericColumns(row, columns, false);
                    HistoricalStocksInfo old = existing.get(tickerSymbol);
                    HistoricalStocksInfo info = new HistoricalStocksInfo();
                    info.setTickerSymbol(tickerSymbol);
                    info.setTickerName(tickerName);
                    info.setNetAssets(mergeJson(old == null ? null : old.getNetAssets(), netAssets).toJSONString());
                    rows.add(info);
                }
            });
            return rows;
        }
    }

    private static List<HistoricalStocksInfo> parseAnnualDividend(Path path,
                                                                 HistoricalStocksInfoMapper historicalMapper)
            throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllNew());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderColumns columns = collectYearColumns(sheet.getRow(0), "现金分红总额");
            List<HistoricalStocksInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol)) {
                        return;
                    }
                    JSONObject totalBonus = jsonFromNumericColumns(row, columns, true);
                    HistoricalStocksInfo old = existing.get(tickerSymbol);
                    HistoricalStocksInfo info = new HistoricalStocksInfo();
                    info.setTickerSymbol(tickerSymbol);
                    info.setTickerName(tickerName);
                    info.setTotalBonus(mergeJson(old == null ? null : old.getTotalBonus(), totalBonus).toJSONString());
                    rows.add(info);
                }
            });
            return rows;
        }
    }

    private static List<StockInfo> parseStockBasic(Path path, StockInfoMapper stockMapper) throws Exception {
        Map<String, StockInfo> existing = mapStockInfoByTickerSymbol(stockMapper.findAllForImportPreserve());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<StockInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol) || tickerSymbol.contains("undefined")
                            || tickerName.contains("tickerName")) {
                        return;
                    }
                    StockInfo old = existing.get(tickerSymbol);
                    BigDecimal stockPrice = nullableDecimalCell(row.getCell(2));
                    if (stockPrice == null && old != null) {
                        stockPrice = old.getStockPrice();
                    }
                    BigDecimal totalShares = nullableDecimalCell(row.getCell(3));
                    if (totalShares == null && old != null && old.getTotalShares() != null) {
                        totalShares = BigDecimal.valueOf(old.getTotalShares());
                    }
                    BigDecimal marketCap = stockPrice != null && totalShares != null
                            ? stockPrice.multiply(totalShares)
                            : old == null ? null : old.getMarketCap();
                    BigDecimal bookValuePerShare = nullableDecimalCell(row.getCell(4));
                    if (bookValuePerShare == null && old != null) {
                        bookValuePerShare = old.getBookValuePerShare();
                    }
                    BigDecimal pbRatio = BigDecimal.ZERO;
                    if (bookValuePerShare != null && stockPrice != null
                            && bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
                        pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
                    } else if (old != null) {
                        pbRatio = old.getPbRatio();
                    }
                    BigDecimal totalAssets = nullableDecimalCell(row.getCell(5));
                    if (totalAssets == null && old != null) {
                        totalAssets = old.getTotalAssets();
                    }
                    BigDecimal totalLiabilities = nullableDecimalCell(row.getCell(6));
                    if (totalLiabilities == null && old != null) {
                        totalLiabilities = old.getTotalLiabilities();
                    }
                    BigDecimal netAssets = nullableDecimalCell(row.getCell(7));
                    if (netAssets == null && old != null) {
                        netAssets = old.getNetAssets();
                    }
                    BigDecimal priceToEarningsRatio = nullableDecimalCell(row.getCell(8));
                    if (priceToEarningsRatio == null && old != null) {
                        priceToEarningsRatio = old.getPriceToEarningsRatio();
                    }
                    String industryCategory = cellText(row.getCell(9));
                    if (StringUtils.isBlank(industryCategory) && old != null) {
                        industryCategory = old.getIndustryCategory();
                    }
                    BigDecimal avgDividendYield3yr = old == null ? null : old.getAvgDividendYield3yr();
                    String capitalChanges = old == null ? null : old.getCapitalChanges();
                    StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName,
                            marketCap == null ? null : marketCap.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            pbRatio == null ? null : pbRatio.setScale(4, RoundingMode.HALF_UP),
                            avgDividendYield3yr,
                            stockPrice == null ? null : stockPrice.setScale(4, RoundingMode.HALF_UP),
                            totalShares == null ? null : totalShares.longValue(),
                            bookValuePerShare == null ? null : bookValuePerShare.setScale(4, RoundingMode.HALF_UP),
                            capitalChanges,
                            priceToEarningsRatio == null ? null : priceToEarningsRatio.setScale(4, RoundingMode.HALF_UP),
                            totalAssets == null ? null : totalAssets.setScale(2, RoundingMode.HALF_UP),
                            totalLiabilities == null ? null : totalLiabilities.setScale(2, RoundingMode.HALF_UP),
                            netAssets == null ? null : netAssets.setScale(2, RoundingMode.HALF_UP),
                            industryCategory);
                    rows.add(stockInfo);
                }
            });
            return rows;
        }
    }

    private static List<HistoricalStocksInfo> parseDisclosureDate(Path path,
                                                                 HistoricalStocksInfoMapper historicalMapper)
            throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllNew());
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderColumns columns = collectColumns(sheet.getRow(0), "实际披露日期");
            List<HistoricalStocksInfo> rows = new ArrayList<>();
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String tickerSymbol = cellText(row.getCell(0));
                    String tickerName = cellText(row.getCell(1));
                    if (StringUtils.isBlank(tickerSymbol)) {
                        return;
                    }
                    JSONObject announceDate = new JSONObject();
                    for (Map.Entry<String, Integer> entry : columns.columns.entrySet()) {
                        String date = dateCell(row.getCell(entry.getValue()));
                        if (StringUtils.isNotBlank(date)) {
                            announceDate.put(entry.getKey(), date);
                        }
                    }
                    HistoricalStocksInfo old = existing.get(tickerSymbol);
                    HistoricalStocksInfo info = new HistoricalStocksInfo();
                    info.setTickerSymbol(tickerSymbol);
                    info.setTickerName(tickerName);
                    info.setAnnounceDate(mergeJson(old == null ? null : old.getAnnounceDate(), announceDate).toJSONString());
                    rows.add(info);
                }
            });
            return rows;
        }
    }

    private static List<HistoricalStocksInfo> parseDividendDetail(Path path,
                                                                 HistoricalStocksInfoMapper historicalMapper,
                                                                 StockInfoMapper stockMapper,
                                                                 ImportReceipt receipt) throws Exception {
        Map<String, HistoricalStocksInfo> existing = mapByTickerSymbol(historicalMapper.findAllDividendPayloads());
        Map<String, String> fullCodeByBareCode = buildFullCodeByBareCode(stockMapper.getAllTickerSymbol());
        Map<String, Map<String, JSONArray>> newDetailByCode = new LinkedHashMap<>();
        Map<String, JSONObject> completedYearsByCode = new LinkedHashMap<>();
        Set<String> completedYearPolicyEnabledCodes = new LinkedHashSet<>();
        try (InputStream inputStream = new FileInputStream(path.toFile()); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            forEachDataRow(sheet, new RowConsumer() {
                @Override
                public void accept(Row row) {
                    String bareCode = bareSixDigitCode(cellText(row.getCell(0)));
                    String fullCode = fullCodeByBareCode.get(bareCode);
                    if (StringUtils.isBlank(fullCode)) {
                        receipt.skippedDividendCode++;
                        return;
                    }

                    String dividendYear = cellText(row.getCell(6)).replaceAll("[^0-9]", "");
                    String completionDate = dateCell(row.getCell(7));
                    BigDecimal annualCashPerShare = nullableDecimalCell(row.getCell(8));
                    String policyEnabledText = cellText(row.getCell(9));
                    boolean completedYearPolicyEnabled = "1".equals(policyEnabledText)
                            || "true".equalsIgnoreCase(policyEnabledText)
                            || "yes".equalsIgnoreCase(policyEnabledText)
                            || "是".equals(policyEnabledText);
                    boolean hasCompletedYearMetadata = StringUtils.isNotBlank(dividendYear)
                            || StringUtils.isNotBlank(completionDate) || annualCashPerShare != null;
                    if (hasCompletedYearMetadata) {
                        if (!completedYearPolicyEnabled) {
                            throw new IllegalStateException("completed-year metadata must explicitly enable policy at row "
                                    + (row.getRowNum() + 1));
                        }
                        if (dividendYear.length() != 4 || !isValidDateKey(completionDate)
                                || annualCashPerShare == null || annualCashPerShare.compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalStateException("invalid completed-year dividend metadata at row "
                                    + (row.getRowNum() + 1));
                        }
                        JSONObject completedYear = new JSONObject();
                        completedYear.put("completionDate", completionDate);
                        completedYear.put("cashPerShare", plainDecimal(annualCashPerShare));
                        JSONObject completedYears = completedYearsByCode.computeIfAbsent(fullCode,
                                key -> new JSONObject());
                        if (completedYears.put(dividendYear, completedYear) != null) {
                            receipt.duplicateCompletedDividendYear++;
                            throw new IllegalStateException("duplicate completed dividend year for " + fullCode
                                    + ": " + dividendYear);
                        }
                        receipt.completedDividendYearRows++;
                    }
                    if (completedYearPolicyEnabled) {
                        completedYearPolicyEnabledCodes.add(fullCode);
                        receipt.completedDividendPolicyEnabledRows++;
                    }

                    boolean hasDividendDetail = StringUtils.isNotBlank(cellText(row.getCell(1)))
                            || StringUtils.isNotBlank(cellText(row.getCell(2)))
                            || StringUtils.isNotBlank(cellText(row.getCell(3)))
                            || StringUtils.isNotBlank(cellText(row.getCell(4)))
                            || StringUtils.isNotBlank(cellText(row.getCell(5)));
                    if (!hasDividendDetail) {
                        if (hasCompletedYearMetadata || completedYearPolicyEnabled) {
                            receipt.completedDividendYearOnlyRows++;
                        }
                        return;
                    }
                    String exDate = dateCell(row.getCell(4));
                    if (StringUtils.isBlank(exDate)) {
                        receipt.skippedDividendDate++;
                        return;
                    }
                    if (!isValidDateKey(exDate)) {
                        throw new IllegalStateException("invalid dividend ex-date at row "
                                + (row.getRowNum() + 1));
                    }
                    Map<String, JSONArray> detailByDate = newDetailByCode.computeIfAbsent(fullCode,
                            key -> new LinkedHashMap<>());
                    if (detailByDate.containsKey(exDate)) {
                        receipt.duplicateDividendDate++;
                        return;
                    }
                    JSONObject detail = new JSONObject();
                    BigDecimal shareGift = nullableDecimalCell(row.getCell(1));
                    BigDecimal shareTransfer = nullableDecimalCell(row.getCell(2));
                    BigDecimal cashDividend = nullableDecimalCell(row.getCell(3));
                    // 送股/转增为空时按 0 写入，保证 StockInfoService 的复权计算不会对 null 调用 compareTo。
                    detail.put("a", plainDecimal(shareGift == null ? BigDecimal.ZERO : shareGift));
                    detail.put("b", plainDecimal(shareTransfer == null ? BigDecimal.ZERO : shareTransfer));
                    if (cashDividend != null) {
                        detail.put("m", plainDecimal(cashDividend));
                    }
                    JSONArray details = new JSONArray();
                    details.add(detail);
                    detailByDate.put(exDate, details);
                }
            });
        }

        List<HistoricalStocksInfo> rows = new ArrayList<>();
        boolean initializeEmptyDividendData = Boolean.parseBoolean(
                System.getProperty(INITIALIZE_EMPTY_DIVIDEND_PROPERTY, "false"));
        Set<String> affectedTickerSymbols = new LinkedHashSet<>();
        affectedTickerSymbols.addAll(newDetailByCode.keySet());
        affectedTickerSymbols.addAll(completedYearsByCode.keySet());
        affectedTickerSymbols.addAll(completedYearPolicyEnabledCodes);
        for (String tickerSymbol : affectedTickerSymbols) {
            HistoricalStocksInfo old = existing.get(tickerSymbol);
            if (old == null) {
                receipt.skippedOldEmptyDividendsData++;
                continue;
            }
            Map<String, JSONArray> newDetails = newDetailByCode.get(tickerSymbol);
            String mergedDividendsData = old.getDividendsData();
            if (newDetails != null && !newDetails.isEmpty()) {
                boolean oldDividendsEmpty = StringUtils.isEmpty(old.getDividendsData());
                if (oldDividendsEmpty && !initializeEmptyDividendData) {
                    receipt.skippedOldEmptyDividendsData++;
                } else {
                    if (oldDividendsEmpty) {
                        receipt.initializedEmptyDividendsData++;
                    }
                    JSONObject merged = oldDividendsEmpty
                            ? new JSONObject()
                            : mergeJson(old.getDividendsData(), new JSONObject());
                    for (Map.Entry<String, JSONArray> dateEntry : newDetails.entrySet()) {
                        merged.put(dateEntry.getKey(), dateEntry.getValue());
                    }
                    mergedDividendsData = merged.toJSONString();
                }
            }

            String mergedDailyDividends = old.getDailyDividends();
            JSONObject newCompletedYears = completedYearsByCode.get(tickerSymbol);
            if (completedYearPolicyEnabledCodes.contains(tickerSymbol)) {
                JSONObject dailyDividends = mergeJson(old.getDailyDividends(), new JSONObject());
                JSONObject completedYears = dailyDividends.getJSONObject("_completedYears");
                if (completedYears == null) {
                    completedYears = new JSONObject();
                }
                if (newCompletedYears != null) {
                    completedYears.putAll(newCompletedYears);
                }
                dailyDividends.put("_completedYears", completedYears);
                mergedDailyDividends = dailyDividends.toJSONString();
            }
            HistoricalStocksInfo info = new HistoricalStocksInfo();
            info.setTickerSymbol(tickerSymbol);
            info.setDividendsData(mergedDividendsData);
            info.setDailyDividends(mergedDailyDividends);
            rows.add(info);
        }
        return rows;
    }

    private static Map<String, String> buildFullCodeByBareCode(List<StockInfo> allTickerSymbols) {
        Map<String, String> result = new LinkedHashMap<>();
        for (StockInfo stockInfo : allTickerSymbols) {
            String fullCode = stockInfo.getTickerSymbol();
            String bareCode = bareSixDigitCode(fullCode);
            if (StringUtils.isNotBlank(bareCode) && StringUtils.isNotBlank(fullCode)) {
                result.putIfAbsent(bareCode, fullCode);
            }
        }
        return result;
    }

    private static Map<String, HistoricalStocksInfo> mapByTickerSymbol(List<HistoricalStocksInfo> rows) {
        Map<String, HistoricalStocksInfo> result = new LinkedHashMap<>();
        for (HistoricalStocksInfo row : rows) {
            if (StringUtils.isNotBlank(row.getTickerSymbol())) {
                result.putIfAbsent(row.getTickerSymbol(), row);
            }
        }
        return result;
    }

    private static Map<String, StockInfo> mapStockInfoByTickerSymbol(List<StockInfo> rows) {
        Map<String, StockInfo> result = new LinkedHashMap<>();
        for (StockInfo row : rows) {
            if (StringUtils.isNotBlank(row.getTickerSymbol())) {
                result.putIfAbsent(row.getTickerSymbol(), row);
            }
        }
        return result;
    }

    private static void assertDb1ImportGuard(JdbcTemplate jdbcTemplate, BackupManifest manifest) {
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (!manifest.database.equals(database)) {
            throw new IllegalStateException("DB1 guard failed: unexpected database");
        }
        String serverUuid = jdbcTemplate.queryForObject("SELECT @@server_uuid", String.class);
        if (!manifest.serverUuid.equalsIgnoreCase(StringUtils.defaultString(serverUuid))) {
            throw new IllegalStateException("DB1 guard failed: unexpected server_uuid");
        }
        for (String tableName : manifest.tables.keySet()) {
            BackupTable table = manifest.tables.get(tableName);
            assertDb1TableGuard(jdbcTemplate, manifest.database, table);
        }
    }

    private static void assertDb1TableGuard(JdbcTemplate jdbcTemplate, String database, BackupTable table) {
        long count = queryTableCount(jdbcTemplate, table.name);
        if (count != table.rowCount) {
            throw new IllegalStateException("DB1 guard failed: row count mismatch for " + table.name);
        }
        String engine = jdbcTemplate.queryForObject(
                "SELECT ENGINE FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                new Object[]{database, table.name}, String.class);
        if (!"InnoDB".equals(engine)) {
            throw new IllegalStateException("DB1 guard failed: table engine mismatch for " + table.name);
        }
        Integer uniqueTickerIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics "
                        + "WHERE table_schema = ? AND table_name = ? AND column_name = 'ticker_symbol' AND non_unique = 0",
                new Object[]{database, table.name}, Integer.class);
        if (uniqueTickerIndexCount == null || uniqueTickerIndexCount <= 0) {
            throw new IllegalStateException("DB1 guard failed: missing unique ticker_symbol index for " + table.name);
        }
    }

    private static long queryTableCount(JdbcTemplate jdbcTemplate, String tableName) {
        if (!"stock_info".equals(tableName) && !"historical_stocks_info".equals(tableName)) {
            throw new IllegalStateException("DB1 guard refused unexpected table: " + tableName);
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? -1L : count;
    }

    private static void requireInteger(JSONObject object, String field, int expected) {
        if (object == null) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " missing JSON root");
        }
        Integer value = object.getInteger(field);
        if (value == null || value != expected) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " " + field + " must be " + expected);
        }
    }

    private static String requireString(JSONObject object, String field) {
        if (object == null) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " missing JSON object for " + field);
        }
        String value = object.getString(field);
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " missing string field: " + field);
        }
        return value.trim();
    }

    private static String requireSha256(JSONObject object, String field) {
        String value = requireString(object, field);
        if (!SHA256_PATTERN.matcher(value).matches()) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " " + field + " must be SHA-256");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static long requirePositiveLong(JSONObject object, String field) {
        if (object == null) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " missing JSON object for " + field);
        }
        Long value = object.getLong(field);
        if (value == null || value <= 0L) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " " + field + " must be > 0");
        }
        return value;
    }

    private static void validateUuid(String value) {
        try {
            UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " serverUuid must be UUID", ex);
        }
    }

    private static void validateCreatedAt(String value) {
        Instant createdAt;
        try {
            createdAt = Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " createdAt must be ISO-8601", ex);
        }
        Instant now = Instant.now();
        if (createdAt.isBefore(now.minus(Duration.ofHours(24))) || createdAt.isAfter(now.plus(Duration.ofMinutes(5)))) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " createdAt is outside allowed time window");
        }
    }

    private static void validateDump(JSONObject dump) throws Exception {
        if (dump == null) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " missing dump object");
        }
        Path dumpPath = Paths.get(requireString(dump, "path"));
        File dumpFile = dumpPath.toFile();
        if (!dumpFile.isFile()) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " dump.path must be a real ordinary file");
        }
        if (!dumpFile.getName().toLowerCase(Locale.ROOT).endsWith(".gz")) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " dump.path must be a .gz file");
        }
        long actualSize = Files.size(dumpPath);
        long expectedSize = requirePositiveLong(dump, "sizeBytes");
        if (actualSize != expectedSize) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " dump.sizeBytes does not match actual file size");
        }
        String expectedSha256 = requireSha256(dump, "sha256");
        String actualSha256 = sha256(dumpPath);
        if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " dump.sha256 does not match actual file SHA-256");
        }
        assertGzipReadableToEof(dumpPath);
    }

    private static void assertGzipReadableToEof(Path dumpPath) throws IOException {
        try (InputStream inputStream = new FileInputStream(dumpPath.toFile());
             GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[8192];
            while (gzipInputStream.read(buffer) != -1) {
                // GZIPInputStream reaching EOF proves the dump is decompressible.
            }
        }
    }

    private static Map<String, BackupTable> validateTables(JSONArray tables) {
        if (tables == null || tables.size() != 2) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY
                    + " tables must contain exactly stock_info and historical_stocks_info");
        }
        Map<String, BackupTable> result = new LinkedHashMap<>();
        for (int i = 0; i < tables.size(); i++) {
            JSONObject table = tables.getJSONObject(i);
            String name = requireString(table, "name");
            if (!"stock_info".equals(name) && !"historical_stocks_info".equals(name)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " unexpected table: " + name);
            }
            if (result.containsKey(name)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " duplicate table: " + name);
            }
            long rowCount = requirePositiveLong(table, "rowCount");
            String engine = requireString(table, "engine");
            if (!"InnoDB".equals(engine)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " table engine must be InnoDB for " + name);
            }
            result.put(name, new BackupTable(name, rowCount, engine));
        }
        if (!result.containsKey("stock_info") || !result.containsKey("historical_stocks_info")) {
            throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY
                    + " tables must contain exactly stock_info and historical_stocks_info");
        }
        return result;
    }

    private static HeaderColumns collectColumns(Row headerRow, String token) {
        HeaderColumns columns = new HeaderColumns();
        for (Cell cell : headerRow) {
            String header = cellText(cell);
            if (header.contains(token)) {
                String key = dateKeyFromHeader(header, token);
                if (columns.columns.put(key, cell.getColumnIndex()) != null) {
                    throw new IllegalStateException("duplicate header date key for " + token + ": " + key);
                }
            }
        }
        if (columns.columns.isEmpty()) {
            throw new IllegalStateException("missing header columns for " + token);
        }
        return columns;
    }

    private static HeaderColumns collectYearColumns(Row headerRow, String token) {
        HeaderColumns columns = new HeaderColumns();
        for (Cell cell : headerRow) {
            String header = cellText(cell);
            if (header.contains(token)) {
                String key = yearKeyFromHeader(header, token);
                if (columns.columns.put(key, cell.getColumnIndex()) != null) {
                    throw new IllegalStateException("duplicate annual header year key for " + token + ": " + key);
                }
            }
        }
        if (columns.columns.isEmpty()) {
            throw new IllegalStateException("missing annual header columns for " + token);
        }
        return columns;
    }

    private static JSONObject jsonFromNumericColumns(Row row, HeaderColumns columns, boolean longValue) {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, Integer> entry : columns.columns.entrySet()) {
            BigDecimal value = nullableDecimalCell(row.getCell(entry.getValue()));
            if (value != null) {
                json.put(entry.getKey(), longValue ? value.longValue() : value);
            }
        }
        return json;
    }

    private static JSONObject mergeJson(String oldJson, JSONObject newJson) {
        JSONObject merged = new JSONObject();
        if (StringUtils.isNotEmpty(oldJson)) {
            merged.putAll(JSONObject.parseObject(oldJson));
        }
        merged.putAll(newJson);
        return merged;
    }

    private static void forEachDataRow(Sheet sheet, RowConsumer consumer) throws Exception {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || rowIsBlank(row)) {
                continue;
            }
            consumer.accept(row);
        }
    }

    private static BigDecimal decimalCell(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    return parseDecimal(cell.getStringCellValue());
                case FORMULA:
                    return parseDecimal(DATA_FORMATTER.formatCellValue(cell));
                default:
                    return BigDecimal.ZERO;
            }
        } catch (RuntimeException ex) {
            return BigDecimal.ZERO;
        }
    }

    /** Returns null for a missing value so import profiles can preserve the production value. */
    private static BigDecimal nullableDecimalCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        String raw = cellText(cell);
        if (StringUtils.isBlank(raw) || "--".equals(raw.trim()) || "—".equals(raw.trim())) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    return parseNullableDecimal(cell.getStringCellValue());
                case FORMULA:
                    return parseNullableDecimal(DATA_FORMATTER.formatCellValue(cell));
                default:
                    return null;
            }
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static BigDecimal parseNullableDecimal(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String normalized = raw.trim()
                .replace(",", "")
                .replace("%", "")
                .replace("—", "")
                .replace("--", "");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String raw) {
        if (StringUtils.isBlank(raw)) {
            return BigDecimal.ZERO;
        }
        String normalized = raw.trim()
                .replace(",", "")
                .replace("%", "")
                .replace("—", "")
                .replace("--", "");
        if (StringUtils.isBlank(normalized)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String dateCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return new SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(date);
        }
        String text = cellText(cell);
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            return digits;
        }
        if (digits.length() > 8) {
            return digits.substring(0, 8);
        }
        return digits;
    }

    private static boolean isValidDateKey(String value) {
        if (value == null || value.length() != 8) {
            return false;
        }
        try {
            LocalDate.parse(value, STRICT_DATE_KEY_FORMATTER);
            return true;
        } catch (DateTimeParseException invalidDate) {
            return false;
        }
    }

    private static String dateKeyFromHeader(String header, String token) {
        String withoutToken = header.replace(token, "")
                .replace("(元)", "")
                .replace("（元）", "")
                .replace(".", "")
                .replace("-", "")
                .replace("/", "")
                .trim();
        String digits = withoutToken.replaceAll("[^0-9]", "");
        if (digits.length() != 8) {
            throw new IllegalStateException("header date key must be yyyyMMdd: " + header);
        }
        return digits;
    }

    private static String yearKeyFromHeader(String header, String token) {
        String withoutToken = header.replace(token, "")
                .replace("(元)", "")
                .replace("（元）", "")
                .trim();
        String digits = withoutToken.replaceAll("[^0-9]", "");
        if (digits.length() != 4) {
            throw new IllegalStateException("annual header year key must be yyyy: " + header);
        }
        return digits;
    }

    private static String cellText(Cell cell) {
        return cell == null ? "" : DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private static String bareSixDigitCode(String code) {
        if (StringUtils.isBlank(code)) {
            return "";
        }
        String digits = code.replaceAll("[^0-9]", "");
        return digits.length() >= 6 ? digits.substring(0, 6) : digits;
    }

    private static String plainDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static void writeHistoricalBatches(WorkbookProfile profile,
                                               List<HistoricalStocksInfo> rows,
                                               HistoricalBatchWriter writer,
                                               ImportReceipt receipt) {
        List<List<HistoricalStocksInfo>> batches = createBatches(rows, BATCH_SIZE);
        for (List<HistoricalStocksInfo> batch : batches) {
            writer.write(batch);
            afterBatch(profile);
        }
        receipt.add(profile, rows.size(), batches.size());
    }

    private static void writeStockBatches(WorkbookProfile profile,
                                          List<StockInfo> rows,
                                          StockBatchWriter writer,
                                          ImportReceipt receipt) {
        List<List<StockInfo>> batches = createBatches(rows, BATCH_SIZE);
        for (List<StockInfo> batch : batches) {
            writer.write(batch);
            afterBatch(profile);
        }
        receipt.add(profile, rows.size(), batches.size());
    }

    private static <T> List<List<T>> createBatches(List<T> rows, int batchSize) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> batches = new ArrayList<>();
        for (int start = 0; start < rows.size(); start += batchSize) {
            int end = Math.min(rows.size(), start + batchSize);
            batches.add(new ArrayList<>(rows.subList(start, end)));
        }
        return batches;
    }

    /** Test-only hook. The production application has no dependency on this property. */
    private static void afterBatch(WorkbookProfile profile) {
        completedBatches++;
        String configured = System.getProperty(FAIL_AFTER_BATCHES_PROPERTY);
        if (StringUtils.isBlank(configured)) {
            return;
        }
        final int failAfter;
        try {
            failAfter = Integer.parseInt(configured.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(FAIL_AFTER_BATCHES_PROPERTY + " must be a positive integer", ex);
        }
        if (failAfter <= 0) {
            throw new IllegalStateException(FAIL_AFTER_BATCHES_PROPERTY + " must be a positive integer");
        }
        if (completedBatches == failAfter) {
            throw new IllegalStateException("故障注入在第 " + completedBatches
                    + " 个批次后触发，profile=" + profile.name());
        }
    }

    private static String sourceBundleSha256(Map<WorkbookProfile, WorkbookProfileResult> results)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (WorkbookProfile profile : WorkbookProfile.values()) {
            WorkbookProfileResult result = results.get(profile);
            digest.update(profile.name().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(result.sha256.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        return hex(digest.digest());
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new FileInputStream(path.toFile());
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer) != -1) {
                // DigestInputStream updates the digest.
            }
        }
        return hex(digest.digest());
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
    }

    private static void printValidationReceipt(SevenWorkbookPreflight preflight) {
        System.out.println("stockExcelUpload.validate sourceBundleSha256=" + preflight.getSourceBundleSha256());
        for (WorkbookProfileResult result : preflight.results.values()) {
            System.out.println(result.profile.propertyName + " rows=" + result.dataRows + " sha256=" + result.sha256);
        }
    }

    private static void printImportReceipt(SevenWorkbookPreflight preflight, ImportReceipt receipt) {
        System.out.println("stockExcelUpload.import sourceBundleSha256=" + preflight.getSourceBundleSha256());
        for (WorkbookProfileResult result : preflight.results.values()) {
            System.out.println(result.profile.propertyName + " rows=" + result.dataRows + " sha256=" + result.sha256
                    + " writeRows=" + receipt.writeRows.getOrDefault(result.profile, 0)
                    + " batches=" + receipt.batchCounts.getOrDefault(result.profile, 0));
        }
        System.out.println("stockExcelUpload.import skippedDividendCode=" + receipt.skippedDividendCode
                + " skippedDividendDate=" + receipt.skippedDividendDate
                + " duplicateDividendDate=" + receipt.duplicateDividendDate
                + " completedDividendYearRows=" + receipt.completedDividendYearRows
                + " completedDividendYearOnlyRows=" + receipt.completedDividendYearOnlyRows
                + " duplicateCompletedDividendYear=" + receipt.duplicateCompletedDividendYear
                + " completedDividendPolicyEnabledRows=" + receipt.completedDividendPolicyEnabledRows
                + " skippedOldEmptyDividendsData=" + receipt.skippedOldEmptyDividendsData
                + " initializedEmptyDividendsData=" + receipt.initializedEmptyDividendsData
                + " initializeEmptyDividendData="
                + Boolean.parseBoolean(System.getProperty(INITIALIZE_EMPTY_DIVIDEND_PROPERTY, "false"))
                + " transactionStatus=" + receipt.transactionStatus);
    }

    enum WorkbookProfile {
        REVENUE_PROFIT("stock.import.revenueProfit", "营业收入净利润", 0, "营业总收入", "净利润"),
        BVPS("stock.import.bvps", "BVPS", 0, "每股净资产"),
        NET_ASSETS("stock.import.netAssets", "净资产", 0, "净资产"),
        ANNUAL_DIVIDEND("stock.import.annualDividend", "年度分红", 0, "现金分红总额"),
        STOCK_BASIC("stock.import.stockBasic", "股票基础资料", 10, "股票代码", "股票简称", "现价", "总股本",
                "每股净资产", "资产总计", "负债合计", "净资产", "市盈率", "行业"),
        DISCLOSURE_DATE("stock.import.disclosureDate", "披露日期", 0, "实际披露日期"),
        DIVIDEND_DETAIL("stock.import.dividendDetail", "分红明细与年度完成事件", 10, "股票代码", "每10股送股数",
                "每10股转增股数", "每10股派现", "除权除息日", "实施方案内容", "分红年度",
                "年度完成日期", "年度已实施现金每股", "启用年度完成口径");

        final String propertyName;
        final String label;
        final int fixedColumnCount;
        final List<String> requiredHeaderTokens;

        WorkbookProfile(String propertyName, String label, int fixedColumnCount, String... requiredHeaderTokens) {
            this.propertyName = propertyName;
            this.label = label;
            this.fixedColumnCount = fixedColumnCount;
            this.requiredHeaderTokens = Arrays.asList(requiredHeaderTokens);
        }
    }

    static class SevenWorkbookPreflight {
        final Map<WorkbookProfile, WorkbookProfileResult> results;
        final String sourceBundleSha256;

        SevenWorkbookPreflight(Map<WorkbookProfile, WorkbookProfileResult> results, String sourceBundleSha256) {
            this.results = results;
            this.sourceBundleSha256 = sourceBundleSha256;
        }

        WorkbookProfileResult getResult(WorkbookProfile profile) {
            return results.get(profile);
        }

        String getSourceBundleSha256() {
            return sourceBundleSha256;
        }

        void requireFilesUnchanged() throws Exception {
            for (WorkbookProfileResult result : results.values()) {
                String currentSha256 = sha256(result.path);
                if (!result.sha256.equalsIgnoreCase(currentSha256)) {
                    throw new IllegalStateException(result.profile.label + " changed after preflight");
                }
            }
        }
    }

    static class WorkbookProfileResult {
        final WorkbookProfile profile;
        final Path path;
        final String sha256;
        final int dataRows;

        WorkbookProfileResult(WorkbookProfile profile, String sha256, int dataRows) {
            this.profile = profile;
            this.path = Paths.get(System.getProperty(profile.propertyName));
            this.sha256 = sha256;
            this.dataRows = dataRows;
        }
    }

    static class ImportGate {
        final String expectedBundleSha256;
        final BackupManifest manifest;

        ImportGate(String expectedBundleSha256, BackupManifest manifest) {
            this.expectedBundleSha256 = expectedBundleSha256;
            this.manifest = manifest;
        }

        static ImportGate failClosedFromSystemProperties() throws Exception {
            String confirm = System.getProperty(CONFIRM_PROPERTY);
            if (!CONFIRM_VALUE.equals(confirm)) {
                throw new IllegalStateException(CONFIRM_PROPERTY + " must equal " + CONFIRM_VALUE);
            }
            String expectedBundleSha256 = System.getProperty(EXPECTED_BUNDLE_PROPERTY);
            if (!SHA256_PATTERN.matcher(StringUtils.defaultString(expectedBundleSha256)).matches()) {
                throw new IllegalStateException(EXPECTED_BUNDLE_PROPERTY + " must be a SHA-256 value");
            }
            String manifestPath = System.getProperty(BACKUP_MANIFEST_PROPERTY);
            if (StringUtils.isBlank(manifestPath)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " is required");
            }
            Path manifest = Paths.get(manifestPath);
            if (!manifest.toFile().isFile()) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " must point to a real ordinary file");
            }
            BackupManifest backupManifest = validateBackupManifest(manifest);
            if (!backupManifest.sourceBundleSha256.equalsIgnoreCase(expectedBundleSha256)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY
                        + " sourceBundleSha256 does not match " + EXPECTED_BUNDLE_PROPERTY);
            }
            return new ImportGate(expectedBundleSha256, backupManifest);
        }

        void requireExpectedBundle(String actualBundleSha256) {
            if (!expectedBundleSha256.equalsIgnoreCase(actualBundleSha256)) {
                throw new IllegalStateException(EXPECTED_BUNDLE_PROPERTY + " does not match actual sourceBundleSha256");
            }
            if (!manifest.sourceBundleSha256.equalsIgnoreCase(actualBundleSha256)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY
                        + " sourceBundleSha256 does not match actual sourceBundleSha256");
            }
        }

        private static BackupManifest validateBackupManifest(Path manifest) throws Exception {
            JSONObject root;
            try {
                root = JSON.parseObject(new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8));
            } catch (RuntimeException ex) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " must be strict JSON", ex);
            }
            requireInteger(root, "schemaVersion", 1);
            String database = requireString(root, "database");
            if (!"judge_muti".equals(database)) {
                throw new IllegalStateException(BACKUP_MANIFEST_PROPERTY + " database must be judge_muti");
            }
            String serverUuid = requireString(root, "serverUuid");
            validateUuid(serverUuid);
            String sourceBundleSha256 = requireSha256(root, "sourceBundleSha256");
            validateCreatedAt(requireString(root, "createdAt"));
            validateDump(root.getJSONObject("dump"));
            Map<String, BackupTable> tables = validateTables(root.getJSONArray("tables"));
            return new BackupManifest(database, serverUuid, sourceBundleSha256, tables);
        }
    }

    static class BackupManifest {
        final String database;
        final String serverUuid;
        final String sourceBundleSha256;
        final Map<String, BackupTable> tables;

        BackupManifest(String database, String serverUuid, String sourceBundleSha256,
                       Map<String, BackupTable> tables) {
            this.database = database;
            this.serverUuid = serverUuid;
            this.sourceBundleSha256 = sourceBundleSha256;
            this.tables = tables;
        }
    }

    static class BackupTable {
        final String name;
        final long rowCount;
        final String engine;

        BackupTable(String name, long rowCount, String engine) {
            this.name = name;
            this.rowCount = rowCount;
            this.engine = engine;
        }
    }

    static class ImportReceipt {
        final Map<WorkbookProfile, Integer> writeRows = new TreeMap<>();
        final Map<WorkbookProfile, Integer> batchCounts = new TreeMap<>();
        int skippedDividendCode;
        int skippedDividendDate;
        int duplicateDividendDate;
        int completedDividendYearRows;
        int completedDividendYearOnlyRows;
        int duplicateCompletedDividendYear;
        int completedDividendPolicyEnabledRows;
        int skippedOldEmptyDividendsData;
        int initializedEmptyDividendsData;
        String transactionStatus = "NOT_STARTED";

        void add(WorkbookProfile profile, int rows, int batches) {
            writeRows.put(profile, rows);
            batchCounts.put(profile, batches);
        }
    }

    static class HeaderColumns {
        final Map<String, Integer> columns = new LinkedHashMap<>();
    }

    interface RowConsumer {
        void accept(Row row) throws Exception;
    }

    interface HistoricalBatchWriter {
        void write(List<HistoricalStocksInfo> batch);
    }

    interface StockBatchWriter {
        void write(List<StockInfo> batch);
    }
}
