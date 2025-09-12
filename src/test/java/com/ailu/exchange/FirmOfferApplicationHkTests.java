//package com.ailu.exchange;
//
//import com.ailu.exchange.dao.domaim.DividendsInfo;
//import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
//import com.ailu.exchange.dao.domaim.StockInfo;
//import com.ailu.exchange.dao.mapper.source1.*;
//import com.ailu.exchange.service.StockInfoService;
//import com.alibaba.fastjson2.JSON;
//import com.alibaba.fastjson2.JSONArray;
//import com.alibaba.fastjson2.JSONObject;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import org.apache.poi.ss.usermodel.Sheet;
//import org.apache.poi.ss.usermodel.Workbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@Slf4j
//public class FirmOfferApplicationHkTests {
//
//
//    private static final String EXCEL_FILE_PATH_1 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2008年-2000年营业收入.xlsx";
//    private static final String EXCEL_FILE_PATH_2 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2018-2009年营业收入.xlsx";
//    private static final String EXCEL_FILE_PATH_3 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2024-2019年营业收入.xlsx";
//
//    private static final String EXCEL_FILE_PATH_4 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2024-2014年营业收入补充.xlsx";
//    private static final String EXCEL_FILE_PATH_5 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2004-2000年营业收入补充.xlsx";
//    private static final String EXCEL_FILE_PATH_6 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2014-2005年营业收入补充.xlsx";
//
////    private static final String EXCEL_FILE_PATH_4 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2024-2014年净利润.xlsx";
////    private static final String EXCEL_FILE_PATH_5 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2013-2009年净利润.xlsx";
////    private static final String EXCEL_FILE_PATH_6 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2008-2000年净利润.xlsx";
//
//    private static final String EXCEL_FILE_PATH_7 = "C:\\Users\\bicoin\\Desktop\\港股\\港股2024-2000年分红总额.xlsx";
//
//
//    private static final String EXCEL_FILE_PATH_11 = "C:\\Users\\bicoin\\Desktop\\港股\\股价最近一年涨跌幅_近五年最高价.xlsx";
//
//
//    //股票信息
//
//
//    private static final String GUPIAO_XINGXI_FILE_PATH = "C:\\Users\\bicoin\\Desktop\\港股\\港股基本信息.xlsx";
//    private static final String GUPIAO_XINGXI_FILE_PATH_1 = "C:\\Users\\bicoin\\Desktop\\股票信息\\股票信息.xlsx";
//
//
//    @Resource
//    HkHistoricalStocksInfoMapper hkHistoricalStocksInfoMapper;
//
//    @Resource
//    HkStockInfoMapper hkStockInfoMapper;
//
//    private static final int BATCH_SIZE = 500;
//
//
//    @Resource
//    DividendsMapper dividendsMapper;
//
//    public void testGetPagedAndSortedStocks() {
//
//    }
//
//    @Test
//    public void Test() {
//
//
//        process_1(EXCEL_FILE_PATH_4);
//        process_1(EXCEL_FILE_PATH_5);
//        process_1(EXCEL_FILE_PATH_6);
//
//        // 设置分页参数
//        int pageNumber = 1; // 第1页
//        int pageSize = 10;  // 每页10条记录
//
//        // 设置排序字段和方向
//        String orderByFields = "market_cap,stock_price";
//
//        String sortDirection = "DESC"; // 按降序排序
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
////        process_6();
////        process_4();
//    }
//
//
//    public void process_1(String url) {
//        List<HistoricalStocksInfo> singleAll = hkHistoricalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock,
//                        (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
////            Map<String, Integer> profitColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("营业收入总额") || header.contains("营业收入")) {
//                    String date = header.trim().replace("营业收入总额(港元)", "")
//                            .replace("营业收入(港元)", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                }
////                else
////                if (header.contains("净利润")) {
////                    String date = header.replace("净利润(元)", "").replace("净利润（元）", "").replace(".", "").trim();
////                    profitColumns.put(date, cell.getColumnIndex());
////                }
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
//                JSONObject grossRevenueJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    grossRevenueJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("grossRevenue", grossRevenueJson);
//
////                JSONObject netProfitJson = new JSONObject();
////                for (Map.Entry<String, Integer> entry : profitColumns.entrySet()) {
////                    netProfitJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())));
////                }
////                dataMap.put("netProfit", netProfitJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol + "_" + tickerName);
//                if (historicalStocksInfo != null) {
//                    String grossRevenue = historicalStocksInfo.getGrossRevenue();
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(grossRevenue)) {
//                        JSONObject grossRevenueOld = JSONObject.parseObject(grossRevenue);
//                        for (String key : grossRevenueJson.keySet()) {
//                            grossRevenueOld.put(key, grossRevenueJson.get(key));
//                        }
//                        dataMap.put("grossRevenue", grossRevenueOld);
//                    }
//
////                    // 净利润 JSON 串
////                    String netProfit = historicalStocksInfo.getNetProfit();
////                    if (StringUtils.isNotEmpty(netProfit)) {
////                        JSONObject netProfitOld = JSONObject.parseObject(netProfit);
////                        // 覆盖旧数据中的相同键值
////                        for (String key : netProfitJson.keySet()) {
////                            netProfitOld.put(key, netProfitJson.get(key));
////                        }
////                        // 将更新后的 netProfitOld 存入 dataMap
////                        dataMap.put("netProfit", netProfitOld);
////                    }
//                }
//                dataList.add(dataMap);
//
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    hkHistoricalStocksInfoMapper.batchInsertOrUpdate(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void process_5() {
//        try (FileInputStream file = new FileInputStream(""); Workbook workbook =
//                new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> dividendDetailColumns = new LinkedHashMap<>();
//            Map<String, Integer> announcementDateColumns = new LinkedHashMap<>();
//            Map<String, Integer> recordDateColumns = new LinkedHashMap<>();
//            Map<String, Integer> exDividendDateColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("分红明细")) {
//                    String date = header.trim().replace("分红明细", "").trim();
//                    dividendDetailColumns.put(date, cell.getColumnIndex());
//                } else if (header.contains("分红实施公告日")) {
//                    String date = header.replace("分红实施公告日", "").trim();
//                    announcementDateColumns.put(date, cell.getColumnIndex());
//                } else if (header.contains("股权登记日")) {
//                    String date = header.replace("股权登记日", "").trim();
//                    recordDateColumns.put(date, cell.getColumnIndex());
//                } else if (header.contains("除权除息日")) {
//                    String date = header.replace("除权除息日", "").trim();
//                    exDividendDateColumns.put(date, cell.getColumnIndex());
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
//                JSONObject dividendDetailJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : dividendDetailColumns.entrySet()) {
//                    dividendDetailJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("dividendDetail", dividendDetailJson);
//
//                JSONObject announcementDateJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : announcementDateColumns.entrySet()) {
//                    announcementDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("announcementDate", announcementDateJson);
//
//                JSONObject recordDateJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : recordDateColumns.entrySet()) {
//                    recordDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("recordDate", recordDateJson);
//
//
//                JSONObject exDividendDateJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : exDividendDateColumns.entrySet()) {
//                    exDividendDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("exDividendDate", exDividendDateJson);
//
//                dataList.add(dataMap);
//
//            }
//            List<DividendsInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    DividendsInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<DividendsInfo>> batches = createDividendsInfoBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<DividendsInfo> batch : batches) {
//                    dividendsMapper.insertBatch(batch);
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
//
//        for (int i = 0; i < totalSize; i += batchSize) {
//            int end = Math.min(totalSize, i + batchSize);
//            batches.add(new ArrayList<>(list.subList(i, end)));
//        }
//
//        return batches;
//    }
//
//    private static List<List<DividendsInfo>> createDividendsInfoBatches(List<DividendsInfo> list, int batchSize) {
//        List<List<DividendsInfo>> batches = new ArrayList<>();
//        int totalSize = list.size();
//
//        for (int i = 0; i < totalSize; i += batchSize) {
//            int end = Math.min(totalSize, i + batchSize);
//            batches.add(new ArrayList<>(list.subList(i, end)));
//        }
//        return batches;
//    }
//
//
//    public void process_2() {
//
//        List<HistoricalStocksInfo> singleAll = hkHistoricalStocksInfoMapper.findAllNew();
//
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock,
//                        (existing, replacement) -> existing));
//
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_7); Workbook workbook =
//                new XSSFWorkbook(file)) {
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
//
//
//                JSONObject totalBonusJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    totalBonusJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())).longValue());
//                }
//                dataMap.put("totalBonus", totalBonusJson);
//
//
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
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                hkHistoricalStocksInfoMapper.batchInsertOrUpdate2(historicalStocksInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_3(String url) {
//
//        List<HistoricalStocksInfo> singleAll = hkHistoricalStocksInfoMapper.findAllNew
//                ();
//
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock,
//                        (existing, replacement) -> existing));
//
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("扣除非经常性损益后的净利润(元)")) {
//                    String date = header.replace("扣除非经常性损益后的净利润(元)", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                }
//            }
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                JSONObject dataMap = new JSONObject();
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//                dataMap.put("tickerSymbol", tickerSymbol);
//                dataMap.put("tickerName", tickerName);
//                JSONObject netProfitDeductionJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    netProfitDeductionJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("netProfitDeduction", netProfitDeductionJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol + "_" + tickerName);
//                if (historicalStocksInfo != null) {
//                    String netProfitDeduction = historicalStocksInfo.getNetProfitDeduction();
//                    if (StringUtils.isNotEmpty(netProfitDeduction)) {
//                        // 营业总收入 JSON 串
//                        JSONObject netProfitDeductionOld = JSONObject.parseObject(netProfitDeduction);
//                        for (String key : netProfitDeductionJson.keySet()) {
//                            netProfitDeductionOld.put(key, netProfitDeductionJson.get(key));
//                        }
//                        dataMap.put("netProfitDeduction", netProfitDeductionOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    hkHistoricalStocksInfoMapper.batchInsertOrUpdate3(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_4() {
//        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FILE_PATH); Workbook workbook = new XSSFWorkbook(file)) {
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
//
//                //最新价(港元)
//                BigDecimal stockPrice = formatNumericCellValue(row.getCell(7));
//                //总股本(股)
//                BigDecimal totalShares = formatNumericCellValue(row.getCell(8));
//
//                //总市值（股价*总股本）
//                BigDecimal marketCap = stockPrice.multiply(totalShares);
//                //每股净资产bps(港元)
//                BigDecimal bookValuePerShare = formatNumericCellValue(row.getCell(9));
//
//                // 市净率（股价/每股净资产）
//                BigDecimal pbRatio = BigDecimal.ZERO;
//                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
//                    pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
//                }
//                //市盈率ttm（总市值/近四个季度净利润之和）
//                BigDecimal peRatioTtm = formatNumericCellValue(row.getCell(3));
//
//                //资产合计(港元)
//                BigDecimal totalAssets = formatNumericCellValue(row.getCell(10));
//
//                //负债合计(港元)
//                BigDecimal totalLiabilities = formatNumericCellValue(row.getCell(11));
//                //权益合计(港元)
//                BigDecimal netAssets = formatNumericCellValue(row.getCell(12));
//                String industryCategory = getCellValue(row.getCell(13));
//
//                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName, marketCap.setScale(2, RoundingMode.HALF_UP),
//                        peRatioTtm.setScale(2, RoundingMode.HALF_UP),
//                        pbRatio.setScale(4, RoundingMode.HALF_UP),
//                        null, stockPrice.setScale(4, RoundingMode.HALF_UP),
//                        totalShares.longValue(), bookValuePerShare.setScale(4, RoundingMode.HALF_UP), null,
//                        null, totalAssets.setScale(2, RoundingMode.HALF_UP),
//                        totalLiabilities.setScale(2, RoundingMode.HALF_UP), netAssets.setScale(2, RoundingMode.HALF_UP),
//                        industryCategory);
//                stockInfos.add(stockInfo);
//            }
//
//            if (stockInfos.size() > 0) {
//                hkStockInfoMapper.batchInsert(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void process_6() {
//        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FILE_PATH_1); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            List<StockInfo> stockInfos = new ArrayList<>();
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String tickerName = getCellValue(row.getCell(1));
//
//                BigDecimal stockPrice = formatNumericCellValue(row.getCell(2));
//                BigDecimal totalShares = formatNumericCellValue(row.getCell(3));
//
//                //总市值（股价*总股本）
//                BigDecimal marketCap = stockPrice.multiply(totalShares);
//
//                BigDecimal bookValuePerShare = formatNumericCellValue(row.getCell(4));
//
//                // 市净率（股价/每股净资产）
//                BigDecimal pbRatio = BigDecimal.ZERO;
//                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
//                    pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
//                }
//                BigDecimal totalAssets = formatNumericCellValue(row.getCell(5));
//                BigDecimal totalLiabilities = formatNumericCellValue(row.getCell(6));
//                BigDecimal netAssets = formatNumericCellValue(row.getCell(7));
//
//                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName, marketCap.setScale(5, RoundingMode.UNNECESSARY),
//                        pbRatio.setScale(6, RoundingMode.UNNECESSARY), stockPrice.setScale(5, RoundingMode.UNNECESSARY),
//                        totalShares.longValue(), bookValuePerShare.setScale(5, RoundingMode.UNNECESSARY), totalAssets.setScale(5,
//                        RoundingMode.UNNECESSARY),
//                        totalLiabilities.setScale(5, RoundingMode.UNNECESSARY), netAssets.setScale(5, RoundingMode.UNNECESSARY));
//                stockInfos.add(stockInfo);
//            }
//            if (stockInfos.size() > 0) {
//                hkStockInfoMapper.batchInsertNew2(stockInfos);
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
//                String tickerName = getCellValue(row.getCell(1));
//
//                BigDecimal annualPriceChange = formatNumericCellValue(row.getCell(2));
//                BigDecimal maxPrice5y = formatNumericCellValue(row.getCell(3));
//
//                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName, annualPriceChange, maxPrice5y);
//                stockInfos.add(stockInfo);
//            }
//            if (stockInfos.size() > 0) {
//                hkStockInfoMapper.batchInsertNew3(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
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