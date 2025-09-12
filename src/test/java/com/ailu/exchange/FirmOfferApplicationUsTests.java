//package com.ailu.exchange;
//
//import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
//import com.ailu.exchange.dao.domaim.StockInfo;
//import com.ailu.exchange.dao.mapper.source1.UsHistoricalStocksInfoMapper;
//import com.ailu.exchange.dao.mapper.source1.UsStockInfoMapper;
//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONArray;
//import com.alibaba.fastjson2.JSONObject;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.text.DecimalFormat;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@Slf4j
//public class FirmOfferApplicationUsTests {
//
//
//    private static final String EXCEL_FILE_PATH_4 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\美股-单季度营业收入（2019-2026）.xlsx";
//
//    private static final String EXCEL_FILE_PATH_PROCESS_NETPROFIT = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\美股-单季度净利润（2019-2026）.xlsx";
//
//    private static final String EXCEL_FILE_PATH_5 = "C:\\Users\\bicoin\\Desktop\\美股\\2017-2011年营业收入.xlsx";
//    private static final String EXCEL_FILE_PATH_6 = "C:\\Users\\bicoin\\Desktop\\美股\\2024-2018年营业收入.xlsx";
//    private static final String EXCEL_FILE_PATH_8 = "C:\\Users\\bicoin\\Desktop\\美股\\营业总收入2025.xlsx";
//
////    private static final String EXCEL_FILE_PATH_4 = "C:\\Users\\bicoin\\Desktop\\美股\\2008-2001净利润.xlsx";
////    private static final String EXCEL_FILE_PATH_5 = "C:\\Users\\bicoin\\Desktop\\美股\\2016-2009净利润.xlsx";
////    private static final String EXCEL_FILE_PATH_6 = "C:\\Users\\bicoin\\Desktop\\美股\\2025-2017净利润.xlsx";
//
////    private static final String EXCEL_FILE_PATH_8 = "C:\\Users\\bicoin\\Desktop\\美股\\2025净利润.xlsx";
//
//    private static final String EXCEL_FILE_PATH_7 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2024-2000年分红总额.xlsx";
//
//    private static final String EXCEL_FILE_PATH_11 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\最近5年最高低价（前复权）.xlsx";
//
//
//    private static final String EXCEL_FILE_PATH_12 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\分红明细.xlsx";
//
//    private static final String EXCEL_FILE_PATH_13 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\分红金额.xlsx";
//
//    private static final String EXCEL_FILE_PATH_15 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\每股净资产.xlsx";
//
//    //股票信息
//    private static final String GUPIAO_XINGXI_FILE_PATH_1 = "C:\\Users\\KKK\\Desktop\\股票详情\\美股\\美股基础信息（新）.xlsx";
//
//
//    @Resource
//    UsHistoricalStocksInfoMapper usHistoricalStocksInfoMapper;
//
//    @Resource
//    UsStockInfoMapper usStockInfoMapper;
//
//    private static final int BATCH_SIZE = 500;
//
//
//    @Test
//    public void Test() {
//
//        System.out.println(11111);
////        process_(EXCEL_FILE_PATH_15);
//
////        process_6();
//
////        process_7();
//
////        process_8();
//
////        process_1(EXCEL_FILE_PATH_4);
////        process_netProfit(EXCEL_FILE_PATH_PROCESS_NETPROFIT);
////        process_1(EXCEL_FILE_PATH_5);
////        process_1(EXCEL_FILE_PATH_6);
////        process_1(EXCEL_FILE_PATH_8);
//
//
//        // 调用服务方法
////        List<StockInfo> stocks = stockInfoService.getPagedAndSortedStocks(pageNumber, pageSize, orderByFields, sortDirection);
////
////        // 打印结果
////        for (StockInfo stock : stocks) {
////            System.out.println(stock);
////        }
//
//
////        process_2();
////        process_5();
//
////        process_3(EXCEL_FILE_PATH_6);
////        process_3(EXCEL_FILE_PATH_7);
////        process_3(EXCEL_FILE_PATH_8);
////        process_3(EXCEL_FILE_PATH_10);
//
////        process_7();
//
////        process_8();
////        process_6();
////        process_4();
//    }
//
//
//    public void process_1(String url) {
//        List<HistoricalStocksInfo> singleAll = usHistoricalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream().collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock, (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("美股")) {
//                    String date = header.trim().replace("美股", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                }
//            }
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                JSONObject dataMap = new JSONObject();
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//
//                dataMap.put("tickerSymbol", tickerSymbol);
//                dataMap.put("tickerName", tickerName);
//                JSONObject grossRevenueJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    grossRevenueJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())).longValue());
//                }
//                dataMap.put("grossRevenue", grossRevenueJson);
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol + "_" + tickerName);
//                if (historicalStocksInfo != null) {
//                    String grossRevenue = historicalStocksInfo.getGrossRevenue();
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(grossRevenue) && !grossRevenue.equals("null")) {
//                        JSONObject grossRevenueOld = JSONObject.parseObject(grossRevenue);
//                        for (String key : grossRevenueJson.keySet()) {
//                            grossRevenueOld.put(key, grossRevenueJson.get(key));
//                        }
//                        dataMap.put("grossRevenue", grossRevenueOld);
//                    }
//
//                }
//                dataList.add(dataMap);
//
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList), HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    usHistoricalStocksInfoMapper.batchInsertOrUpdateGrossRevenue(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_(String url) {
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//
//
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("美股")) {
//                    String date = header.trim().replace("美股", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                }
//            }
//
//
//            List<StockInfo> stockInfos = new ArrayList<>();
//
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//                Map<String, BigDecimal> grossRevenueJson = new LinkedHashMap<>();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    BigDecimal bigDecimal = formatNumericCellValue(row.getCell(entry.getValue()));
//                    grossRevenueJson.put(entry.getKey(), bigDecimal);
//                }
//
//                if (tickerSymbol.equalsIgnoreCase("WB.O")) {
//                    System.out.println(1111);
//                }
//
//                BigDecimal bookValuePerShare = BigDecimal.ZERO;
//                for (Map.Entry<String, BigDecimal> stringBigDecimalEntry : grossRevenueJson.entrySet()) {
//                    BigDecimal value = stringBigDecimalEntry.getValue();
//                    if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
//                        bookValuePerShare = value;
//                        break;
//                    }
//                }
//                stockInfos.add(new StockInfo(tickerSymbol, bookValuePerShare));
//            }
//            if (!stockInfos.isEmpty()) {
//                usStockInfoMapper.batchUpdateBookValuePerShare(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_netProfit(String url) {
//        List<HistoricalStocksInfo> singleAll = usHistoricalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream().collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock, (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//
//            Map<String, Integer> profitColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("美股")) {
//                    String date = header.trim().replace("美股", "").replace(".", "").trim();
//                    profitColumns.put(date, cell.getColumnIndex());
//                }
//            }
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                JSONObject dataMap = new JSONObject();
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//
//                dataMap.put("tickerSymbol", tickerSymbol);
//                dataMap.put("tickerName", tickerName);
//
//
//                JSONObject netProfitJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : profitColumns.entrySet()) {
//                    netProfitJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())).longValue());
//                }
//                dataMap.put("netProfit", netProfitJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol + "_" + tickerName);
//                if (historicalStocksInfo != null) {
////                     净利润 JSON 串
//                    String netProfit = historicalStocksInfo.getNetProfit();
//                    if (StringUtils.isNotEmpty(netProfit) && !netProfit.equals("null")) {
//                        JSONObject netProfitOld = JSONObject.parseObject(netProfit);
//                        // 覆盖旧数据中的相同键值
//                        for (String key : netProfitJson.keySet()) {
//                            netProfitOld.put(key, netProfitJson.get(key));
//                        }
//                        // 将更新后的 netProfitOld 存入 dataMap
//                        dataMap.put("netProfit", netProfitOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList), HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    usHistoricalStocksInfoMapper.batchInsertOrUpdateNetProfit(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private static List<List<HistoricalStocksInfo>> createBatches(List<HistoricalStocksInfo> list, int batchSize) {
//        List<List<HistoricalStocksInfo>> batches = new ArrayList<>();
//        int totalSize = list.size();
//        for (int i = 0; i < totalSize; i += batchSize) {
//            int end = Math.min(totalSize, i + batchSize);
//            batches.add(new ArrayList<>(list.subList(i, end)));
//        }
//        return batches;
//    }
//
//
//    public void process_2() {
//        List<HistoricalStocksInfo> singleAll = usHistoricalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream().collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock, (existing, replacement) -> existing));
//
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_7); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("年度分红总额")) {
//                    String date = header.replace("年度分红总额(港元)", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                }
//            }
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                JSONObject dataMap = new JSONObject();
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//
//                dataMap.put("tickerSymbol", tickerSymbol);
//                dataMap.put("tickerName", tickerName);
//                JSONObject totalBonusJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    totalBonusJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())).longValue());
//                }
//                dataMap.put("totalBonus", totalBonusJson);
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol + "_" + tickerName);
//
//                if (historicalStocksInfo != null) {
//                    String totalBonus = historicalStocksInfo.getTotalBonus();
//                    if (StringUtils.isNotEmpty(totalBonus)) {
//                        // 分红总额 JSON 串
//                        JSONObject totalBonusOld = JSONObject.parseObject(totalBonus);
//                        totalBonusJson.putAll(totalBonusOld);
//                        dataMap.put("totalBonus", totalBonusJson);
//                    }
//                }
//                dataList.add(dataMap);
//
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList), HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                usHistoricalStocksInfoMapper.batchInsertOrUpdate2(historicalStocksInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_6() {
//        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FILE_PATH_1); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            List<StockInfo> stockInfos = new ArrayList<>();
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                //股票代码
//                String tickerSymbol = getCellValue(row.getCell(0));
//                //股票简称
//                String tickerName = getCellValue(row.getCell(1));
//                //最新价格
//                BigDecimal stockPrice = formatNumericCellValue(row.getCell(2));
//                //总股本
//                BigDecimal totalShares = formatNumericCellValue(row.getCell(3));
//                //总市值（股价*总股本）
//                BigDecimal marketCap = stockPrice.multiply(totalShares);
//                //每股净资产
//                BigDecimal bookValuePerShare = formatNumericCellValue(row.getCell(4));
//                //同花顺全球行业
//                String industryCategory = getCellValue(row.getCell(5));
//                // 市净率（股价/每股净资产）
//                BigDecimal pbRatio = BigDecimal.ZERO;
//                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
//                    pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
//                }
//                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName,
//                        marketCap.setScale(4, RoundingMode.HALF_UP),
//                        pbRatio.setScale(4, RoundingMode.HALF_UP),
//                        stockPrice.setScale(4, RoundingMode.HALF_UP),
//                        totalShares.longValue(),
//                        bookValuePerShare.setScale(5, RoundingMode.HALF_UP),
//                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, industryCategory);
//                stockInfos.add(stockInfo);
//            }
//            if (stockInfos.size() > 0) {
//                usStockInfoMapper.batchInsertNew2(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void process_7() {
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_11); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            List<StockInfo> stockInfos = new ArrayList<>();
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String tickerSymbol = getCellValue(row.getCell(0));
//                BigDecimal stockPrice = formatNumericCellValue(row.getCell(1));
//                BigDecimal maxPrice5y = formatNumericCellValue(row.getCell(2));
//
//                BigDecimal annualPriceChange = BigDecimal.ZERO;
//                if (maxPrice5y.compareTo(BigDecimal.ZERO) != 0) {
//                    annualPriceChange = stockPrice.subtract(maxPrice5y).divide(maxPrice5y, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
//                }
//                StockInfo stockInfo = new StockInfo(tickerSymbol, stockPrice, annualPriceChange, maxPrice5y);
//                stockInfos.add(stockInfo);
//            }
//            if (stockInfos.size() > 0) {
//                usStockInfoMapper.batchUpdateIfExists(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void process_8() {
//
//        List<StockInfo> allTickerSymbol = usStockInfoMapper.getAllTickerSymbolNew();
//        Map<String, StockInfo> usTickerHkMap = allTickerSymbol.stream().collect(Collectors.toMap(StockInfo::getTickerSymbol, stockInfo -> stockInfo, (existing, replacement) -> existing));
//
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_12); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            List<Abonus> abonusList = new ArrayList<>();
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String name = getCellValue(row.getCell(0));
//                BigDecimal amount = formatNumericCellValue(row.getCell(1));
//                String date = getCellValue2(row.getCell(2));
//                if (StringUtils.isNotEmpty(date) && !date.equalsIgnoreCase("null")) {
//                    abonusList.add(new Abonus(name, amount, date));
//                }
//            }
//            Map<String, List<Abonus>> collect = abonusList.stream().collect(Collectors.groupingBy(Abonus::getName));
//            Map<String, BigDecimal> calculateTotalAmountMap = calculateTotalAmount(collect);
//            //获取23年分红总金额
//            Map<String, String> yearFenHongData = get23YearFenHongData();
//            List<StockInfo> stockInfos = new ArrayList<>();
//            for (Map.Entry<String, BigDecimal> stringBigDecimalEntry : calculateTotalAmountMap.entrySet()) {
//                BigDecimal value = stringBigDecimalEntry.getValue();
//                String key = stringBigDecimalEntry.getKey();
//                StockInfo stockInfo = usTickerHkMap.get(key);
//                if (stockInfo != null) {
//                    //总市值
//                    BigDecimal marketCap = stockInfo.getMarketCap();
//                    if (marketCap == null) {
//                        continue;
//                    }
//                    //总股本
//                    Long totalShares = stockInfo.getTotalShares();
//                    //原始股票代码
//                    String tickerSymbolNew = stockInfo.getTickerSymbolNew();
//                    //23年总分红金额
//                    String amount = yearFenHongData.get(tickerSymbolNew);
//                    if (StringUtils.isEmpty(amount)) {
//                        continue;
//                    }
//                    BigDecimal amount23 = null;
//                    BigDecimal amount24 = null;
//                    try {
//                        amount23 = new BigDecimal(amount.split("_")[0]);
//                        amount24 = new BigDecimal(amount.split("_")[1]);
//                    } catch (Exception e) {
//                        log.error("1111111", e);
//                    }
//
////                    //分红总金额
////                    BigDecimal amount24 = BigDecimal.valueOf(totalShares).multiply(value);
//
//                    //2025年股息率=2024年分红总额/总市值
//                    //2024年股息率=2023年分红总额/总市值
//                    //2023年股息率=2022年分红总额/总市值
//
//                    //24年股息率
//                    BigDecimal lastYearDividendRate24 = BigDecimal.ZERO;
//                    if (marketCap.compareTo(BigDecimal.ZERO) != 0) {
//                        lastYearDividendRate24 = amount24.divide(marketCap, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
//                    }
//                    //23年股息率
//                    BigDecimal lastYearDividendRate23 = BigDecimal.ZERO;
//                    if (amount23 != null && marketCap.compareTo(BigDecimal.ZERO) != 0) {
//                        lastYearDividendRate23 = amount23.divide(marketCap, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
//                    }
//                    stockInfos.add(new StockInfo(tickerSymbolNew, lastYearDividendRate24, lastYearDividendRate23, BigDecimal.ZERO, amount24));
//                }
//            }
//            if (stockInfos.size() > 0) {
//                usStockInfoMapper.batchUpdateLastYearDividendRate(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public Map<String, String> get23YearFenHongData() {
//        Map<String, String> dataMap = new HashMap<>();
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_13); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String name = getCellValue(row.getCell(0));
//                BigDecimal amount24 = formatNumericCellValue(row.getCell(1));
//                BigDecimal amount23 = formatNumericCellValue(row.getCell(2));
//                dataMap.put(name, amount24 + "_" + amount23);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return dataMap;
//    }
//
//    public Map<String, BigDecimal> calculateTotalAmount(Map<String, List<Abonus>> collect) {
//        LocalDate currentDate = LocalDate.now();
//        LocalDate thresholdDate = currentDate.minusMonths(13);
//        Map<String, BigDecimal> fenHongAmountMap = new HashMap<>();
//
//        for (Map.Entry<String, List<Abonus>> entry : collect.entrySet()) {
//            BigDecimal totalAmount = BigDecimal.ZERO;
//            for (Abonus abonus : entry.getValue()) {
//                try {
//                    LocalDate abonusDate = LocalDate.parse(abonus.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//                    if (abonusDate.isAfter(thresholdDate) && abonusDate.isBefore(currentDate.plusDays(1))) {
//                        totalAmount = totalAmount.add(abonus.getAmount());
//                    }
//                } catch (Exception e) {
//                    log.error("出现异常1！", e);
//                }
//            }
//            fenHongAmountMap.put(entry.getKey(), totalAmount);
//        }
//        return fenHongAmountMap;
//    }
//
//    @Data
//    static class Abonus {
//        private String name;
//        private BigDecimal amount;
//        private String date;
//
//        public Abonus() {
//        }
//
//        public Abonus(String name, BigDecimal amount, String date) {
//            this.name = name;
//            this.amount = amount;
//            this.date = date;
//        }
//
//    }
//
//
//    private static String getCellValue(Cell cell) {
//        if (cell == null) return null;
//        switch (cell.getCellType()) {
//            case STRING:
//                return cell.getStringCellValue();
//            case NUMERIC:
//                return String.valueOf(cell.getNumericCellValue());
//            default:
//                return null;
//        }
//    }
//
//    static DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.CHINESE);
//    static DateTimeFormatter outputFormatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    private static String getCellValue2(Cell cell) {
//        if (cell == null) return null;
//        switch (cell.getCellType()) {
//            case STRING:
//                return cell.getStringCellValue();
//            case NUMERIC:
//                // 解析输入日期
//                LocalDate date = LocalDate.parse(cell.toString(), inputFormatter);
//                return date.format(outputFormatter1);
//            default:
//                return null;
//        }
//    }
//
//
//    private static BigDecimal formatNumericCellValue(Cell cell) {
//        if (cell == null) return BigDecimal.ZERO;
//        switch (cell.getCellType()) {
//            case NUMERIC:
//                DecimalFormat df = new DecimalFormat("0.00000000");
//                String format = df.format(cell.getNumericCellValue());
//                return new BigDecimal(format);
//            case STRING:
//                try {
//                    return new BigDecimal(cell.getStringCellValue());
//                } catch (NumberFormatException e) {
//                    return BigDecimal.ZERO;
//                }
//            default:
//                return BigDecimal.ZERO;
//        }
//    }
//
//
//}