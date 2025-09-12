//package com.ailu.exchange;
//
//import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
//import com.ailu.exchange.dao.domaim.StockInfo;
//import com.ailu.exchange.dao.mapper.source1.HistoricalStocksInfoMapper;
//import com.ailu.exchange.dao.mapper.source1.StockInfoMapper;
//import com.ailu.exchange.service.StockInfoService;
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
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@Slf4j
//public class FirmOfferApplicationTests {
//
//
//    private static final String EXCEL_FILE_PATH_1 = "C:\\Users\\KKK\\Desktop\\股票详情\\营业收入和净利润\\（A股）2025年一季度+中报营业收入和净利润.xlsx";
//
//
//    private static final String EXCEL_FILE_PATH_9 = "C:\\Users\\bicoin\\Desktop\\营业收入和净利润\\2017-2012年公司披露年报季报的时间.xlsx";
//    private static final String EXCEL_FILE_PATH_12 = "C:\\Users\\bicoin\\Desktop\\营业收入和净利润\\2024-2017年公司披露年报季报的时间.xlsx";
//
//    private static final String EXCEL_FILE_PATH_15 = "C:\\Users\\KKK\\Desktop\\股票详情\\营业收入和净利润\\A股财报披露时间（8.30）.xlsx";
//
//    private static final String EXCEL_FILE_PATH_11 = "C:\\Users\\bicoin\\Desktop\\营业收入和净利润\\最近一年股价涨跌幅.xlsx";
//
//    private static final String EXCEL_FILE_PATH_10 = "C:\\Users\\bicoin\\Desktop\\营业收入和净利润\\2024年中报，一季度 营业收入，净利润，归母净利润.xlsx";
//
//
//    private static final String EXCEL_FILE_PATH_6 = "C:\\Users\\bicoin\\Desktop\\扣除非经常损益的利润\\2003-2000年扣非净利润.xlsx";
//    private static final String EXCEL_FILE_PATH_7 = "C:\\Users\\bicoin\\Desktop\\扣除非经常损益的利润\\2013-2004年扣非净利润.xlsx";
//    private static final String EXCEL_FILE_PATH_8 = "C:\\Users\\bicoin\\Desktop\\扣除非经常损益的利润\\2024-2014年扣非净利润.xlsx";
//
//    //股票信息
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH = "C:\\Users\\bicoin\\Desktop\\分红\\2000-2023年分红总额.xlsx";
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH_2024 = "C:\\Users\\bicoin\\Desktop\\分红\\2024年分红金额.xlsx";
//
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH_2024_NEW = "C:\\Users\\KKK\\Desktop\\股票详情\\分红\\2024年分红总额.xlsx";
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH_2022_2024_NEW = "C:\\Users\\KKK\\Desktop\\股票详情\\分红\\全部A股2022-2024年分红总额.xlsx";
//
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH_6_16_NEW = "C:\\Users\\KKK\\Desktop\\股票详情\\分红\\分红总额全部A股 (6月16).xlsx";
//
//    private static final String GUPIAO_XINGXI_FENGHONG_FILE_PATH_2 = "C:\\Users\\bicoin\\Desktop\\分红\\2000年至2023年分红明细及分红实施公告" +
//            ".xlsx";
//
//    private static final String GUPIAO_XINGXI_FILE_PATH = "C:\\Users\\KKK\\Desktop\\股票详情\\股票信息\\A股基本信息（8.30-新）.xlsx";
//    private static final String GUPIAO_XINGXI_FILE_PATH_1 = "C:\\Users\\bicoin\\Desktop\\股票信息\\股票信息.xlsx";
//
//    private static final String GUPIAO_XINGXI_FILE_PATH_BVPS_DATA = "C:\\Users\\KKK\\Desktop\\股票详情\\股票信息\\A股每股净资产（8.30）.xlsx";
//
//
//    private static final String GUPIAO_XINGXI_FILE_PATH_NET_ASSETS = "C:\\Users\\KKK\\Desktop\\股票详情\\股票信息\\A股每个季度净资产（8.30）.xlsx";
//
//    private static final String EXCEL_FILE_PATH_DIVIDENDS_DATA = "C:\\Users\\bicoin\\Desktop\\股票信息\\A股分红方案（新）1 - 副本.xlsx";
//
//
//    private static final String EXCEL_FILE_PATH_DAILY_DIVIDENDS = "C:\\Users\\KKK\\Desktop\\股票详情\\分红\\A股分红明细（8.30）.xlsx";
//
//
//    @Resource
//    HistoricalStocksInfoMapper historicalStocksInfoMapper;
//
//    @Resource
//    StockInfoMapper stockInfoMapper;
//
//    private static final int BATCH_SIZE = 500;
//
//
//    @Resource
//    private StockInfoService stockInfoService;
//
//
//    public void testGetPagedAndSortedStocks() {
//
//    }
//
//    @Test
//    public void Test() {
//
//        //导入分红方案
//        process_dividendsData();
//
//        //导入A股财报披露时间
////        process_8(EXCEL_FILE_PATH_15);
//        //导入每股净资产
////        processBvpsData(GUPIAO_XINGXI_FILE_PATH_BVPS_DATA);
//
////        process_4();
//
//        //A股每个季度净资产
////        processNetAssets(GUPIAO_XINGXI_FILE_PATH_NET_ASSETS);
//
////        process_2();
//
//        //（A股）2025年一季度+中报营业收入和净利润
////        process_1(EXCEL_FILE_PATH_1);
//
////        process_1(EXCEL_FILE_PATH_10);
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
//
////        process_7();
//
////        process_6();
//
//    }
//
//
//    public void process_1(String url) {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            Map<String, Integer> profitColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("营业总收入")) {
//                    String date = header.trim().replace("营业总收入(元)", "").replace(".", "").trim();
//                    revenueColumns.put(date, cell.getColumnIndex());
//                } else if (header.contains("净利润")) {
//                    String date = header.replace("净利润(元)", "").replace("净利润（元）", "").replace(".", "").trim();
//                    profitColumns.put(date, cell.getColumnIndex());
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
//
//
//                JSONObject grossRevenueJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    grossRevenueJson.put(entry.getKey(), new BigDecimal(formatNumericCellValue(row.getCell(entry.getValue())).stripTrailingZeros().toPlainString()));
//                }
//                dataMap.put("grossRevenue", grossRevenueJson);
//
//                JSONObject netProfitJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : profitColumns.entrySet()) {
//                    netProfitJson.put(entry.getKey(), new BigDecimal(formatNumericCellValue(row.getCell(entry.getValue())).stripTrailingZeros().toPlainString()));
//                }
//                dataMap.put("netProfit", netProfitJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//                if (historicalStocksInfo != null) {
//                    String grossRevenue = historicalStocksInfo.getGrossRevenue();
//
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(grossRevenue)) {
//                        JSONObject grossRevenueOld = JSONObject.parseObject(grossRevenue);
//                        for (String key : grossRevenueJson.keySet()) {
//                            grossRevenueOld.put(key, grossRevenueJson.get(key));
//                        }
//                        dataMap.put("grossRevenue", grossRevenueOld);
//                    }
//                    // 净利润 JSON 串
//                    String netProfit = historicalStocksInfo.getNetProfit();
//                    if (StringUtils.isNotEmpty(netProfit)) {
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
//
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    historicalStocksInfoMapper.batchInsertOrUpdate(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void processBvpsData(String url) {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllBvpsData();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("每股净资产")) {
//                    String date = header.trim().replace("每股净资产(元)", "").replace(".", "").trim();
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
//
//                JSONObject bvpsDataJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    bvpsDataJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("bvpsData", bvpsDataJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//                if (historicalStocksInfo != null) {
//                    String bvpsData = historicalStocksInfo.getBvpsData();
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(bvpsData)) {
//                        JSONObject bvpsDataOld = JSONObject.parseObject(bvpsData);
//                        for (String key : bvpsDataJson.keySet()) {
//                            bvpsDataOld.put(key, bvpsDataJson.get(key));
//                        }
//                        dataMap.put("bvpsData", bvpsDataOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (!historicalStocksInfos.isEmpty()) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    historicalStocksInfoMapper.batchInsertOrUpdateByBvpsData(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void processNetAssets(String url) {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllNetAssets();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("净资产")) {
//                    String date = header.trim().replace("净资产(元)", "").replace(".", "").trim();
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
//
//                JSONObject netAssetsJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    netAssetsJson.put(entry.getKey(), new BigDecimal(formatNumericCellValue(row.getCell(entry.getValue())).stripTrailingZeros().toPlainString()));
//                }
//                dataMap.put("netAssets", netAssetsJson);
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//                if (historicalStocksInfo != null) {
//                    String NetAssets = historicalStocksInfo.getNetAssets();
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(NetAssets)) {
//                        JSONObject netAssetsOld = JSONObject.parseObject(NetAssets);
//                        for (String key : netAssetsJson.keySet()) {
//                            netAssetsOld.put(key, netAssetsJson.get(key));
//                        }
//                        dataMap.put("netAssets", netAssetsOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (!historicalStocksInfos.isEmpty()) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    historicalStocksInfoMapper.batchInsertOrUpdateByNetAssets(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
////    public void process_5() {
////        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FENGHONG_FILE_PATH_2); Workbook workbook =
////                new XSSFWorkbook(file)) {
////            Sheet sheet = workbook.getSheetAt(0);
////            List<JSONObject> dataList = new ArrayList<>();
////            Map<String, Integer> dividendDetailColumns = new LinkedHashMap<>();
////            Map<String, Integer> announcementDateColumns = new LinkedHashMap<>();
////            Map<String, Integer> recordDateColumns = new LinkedHashMap<>();
////            Map<String, Integer> exDividendDateColumns = new LinkedHashMap<>();
////            int startRow = 1;
////            Row headerRow = sheet.getRow(0);
////            for (Cell cell : headerRow) {
////                String header = cell.getStringCellValue();
////                if (header.contains("分红明细")) {
////                    String date = header.trim().replace("分红明细", "").trim();
////                    dividendDetailColumns.put(date, cell.getColumnIndex());
////                } else if (header.contains("分红实施公告日")) {
////                    String date = header.replace("分红实施公告日", "").trim();
////                    announcementDateColumns.put(date, cell.getColumnIndex());
////                } else if (header.contains("股权登记日")) {
////                    String date = header.replace("股权登记日", "").trim();
////                    recordDateColumns.put(date, cell.getColumnIndex());
////                } else if (header.contains("除权除息日")) {
////                    String date = header.replace("除权除息日", "").trim();
////                    exDividendDateColumns.put(date, cell.getColumnIndex());
////                }
////            }
////            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
////                Row row = sheet.getRow(i);
////                if (row == null) continue;
////                JSONObject dataMap = new JSONObject();
////                String tickerSymbol = getCellValue(row.getCell(0));
////                String tickerName = getCellValue(row.getCell(1));
////
////                dataMap.put("tickerSymbol", tickerSymbol);
////                dataMap.put("tickerName", tickerName);
////
////                JSONObject dividendDetailJson = new JSONObject();
////                for (Map.Entry<String, Integer> entry : dividendDetailColumns.entrySet()) {
////                    dividendDetailJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
////                }
////                dataMap.put("dividendDetail", dividendDetailJson);
////
////                JSONObject announcementDateJson = new JSONObject();
////                for (Map.Entry<String, Integer> entry : announcementDateColumns.entrySet()) {
////                    announcementDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
////                }
////                dataMap.put("announcementDate", announcementDateJson);
////
////                JSONObject recordDateJson = new JSONObject();
////                for (Map.Entry<String, Integer> entry : recordDateColumns.entrySet()) {
////                    recordDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
////                }
////                dataMap.put("recordDate", recordDateJson);
////
////
////                JSONObject exDividendDateJson = new JSONObject();
////                for (Map.Entry<String, Integer> entry : exDividendDateColumns.entrySet()) {
////                    exDividendDateJson.put(entry.getKey(), getCellValue(row.getCell(entry.getValue())));
////                }
////                dataMap.put("exDividendDate", exDividendDateJson);
////
////                dataList.add(dataMap);
////
////            }
////            List<DividendsInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
////                    DividendsInfo.class);
////            if (historicalStocksInfos.size() > 0) {
////                List<List<DividendsInfo>> batches = createDividendsInfoBatches(historicalStocksInfos, BATCH_SIZE);
////                for (List<DividendsInfo> batch : batches) {
////                    dividendsMapper.insertBatch(batch);
////                }
////            }
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
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
////    private static List<List<DividendsInfo>> createDividendsInfoBatches(List<DividendsInfo> list, int batchSize) {
////        List<List<DividendsInfo>> batches = new ArrayList<>();
////        int totalSize = list.size();
////
////        for (int i = 0; i < totalSize; i += batchSize) {
////            int end = Math.min(totalSize, i + batchSize);
////            batches.add(new ArrayList<>(list.subList(i, end)));
////        }
////        return batches;
////    }
//
//
//    public void process_2() {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//
//        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FENGHONG_FILE_PATH_6_16_NEW); Workbook workbook =
//                new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("现金分红总额")) {
//                    String date = header.replace("现金分红总额(元)", "").replace(".", "").trim();
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
//
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//                if (historicalStocksInfo != null) {
//                    String totalBonus = historicalStocksInfo.getTotalBonus();
//                    // 营业总收入 JSON 串
//                    if (StringUtils.isNotEmpty(totalBonus)) {
//                        JSONObject totalBonusOld = JSONObject.parseObject(totalBonus);
//                        for (String key : totalBonusJson.keySet()) {
//                            totalBonusOld.put(key, totalBonusJson.get(key));
//                        }
//                        dataMap.put("totalBonus", totalBonusOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                historicalStocksInfoMapper.batchInsertOrUpdate2(historicalStocksInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public void process_3(String url) {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllNew
//                ();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), stock -> stock,
//                        (existing, replacement) -> existing));
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
//                    historicalStocksInfoMapper.batchInsertOrUpdate3(batch);
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
//                if (tickerSymbol.contains("undefined") || tickerName.contains("tickerName")) {
//                    continue;
//                }
//                //现价
//                BigDecimal stockPrice = formatNumericCellValue(row.getCell(2));
//
//                //总股本(股)
//                BigDecimal totalShares = formatNumericCellValue(row.getCell(3));
//
//                //总市值（股价*总股本）
//                BigDecimal marketCap = stockPrice.multiply(totalShares);
//
//                //每股净资产(元)
//                BigDecimal bookValuePerShare = formatNumericCellValue(row.getCell(4));
//
//                // 市净率（股价/每股净资产）
//                BigDecimal pbRatio = BigDecimal.ZERO;
//                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
//                    pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
//                }
//                //市盈率(TTM)
//                BigDecimal peRatioTtm = BigDecimal.ZERO;
//
//                //近三年平均分红率
//                BigDecimal avgDividendYield3yr = BigDecimal.ZERO;
//
//                //股本变化
//                String capitalChanges = null;
//
//                //资产总计
//                BigDecimal totalAssets = formatNumericCellValue(row.getCell(5));
//                //负债合计
//                BigDecimal totalLiabilities = formatNumericCellValue(row.getCell(6));
//                //净资产
//                BigDecimal netAssets = formatNumericCellValue(row.getCell(7));
//                //市盈率(pe)
//                BigDecimal priceToEarningsRatio = formatNumericCellValue(row.getCell(8));
//                //所属同花顺行业
//                String industryCategory = getCellValue(row.getCell(9));
//
//                StockInfo stockInfo = new StockInfo(tickerSymbol,
//                        tickerName,
//                        marketCap.setScale(2, RoundingMode.HALF_UP),
//                        peRatioTtm.setScale(2, RoundingMode.HALF_UP),
//                        pbRatio.setScale(4, RoundingMode.HALF_UP),
//                        avgDividendYield3yr.setScale(2, RoundingMode.HALF_UP),
//                        stockPrice.setScale(4, RoundingMode.HALF_UP),
//                        totalShares.longValue(),
//                        bookValuePerShare.setScale(4, RoundingMode.HALF_UP),
//                        capitalChanges,
//                        priceToEarningsRatio.setScale(4, RoundingMode.HALF_UP),
//                        totalAssets.setScale(2, RoundingMode.HALF_UP),
//                        totalLiabilities.setScale(2, RoundingMode.HALF_UP),
//                        netAssets.setScale(2, RoundingMode.HALF_UP),
//                        industryCategory);
//                stockInfos.add(stockInfo);
//            }
//
//            if (stockInfos.size() > 0) {
//                stockInfoMapper.batchInsertOrUpdate(stockInfos);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
////    public void process_6() {
////        try (FileInputStream file = new FileInputStream(GUPIAO_XINGXI_FILE_PATH_1); Workbook workbook = new XSSFWorkbook(file)) {
////            Sheet sheet = workbook.getSheetAt(0);
////            int startRow = 1;
////            List<StockInfo> stockInfos = new ArrayList<>();
////            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
////                Row row = sheet.getRow(i);
////                if (row == null) continue;
////                String tickerSymbol = getCellValue(row.getCell(0));
////                String tickerName = getCellValue(row.getCell(1));
////
////                BigDecimal stockPrice = formatNumericCellValue(row.getCell(2));
////                BigDecimal totalShares = formatNumericCellValue(row.getCell(3));
////
////                //总市值（股价*总股本）
////                BigDecimal marketCap = stockPrice.multiply(totalShares);
////
////                BigDecimal bookValuePerShare = formatNumericCellValue(row.getCell(4));
////
////                // 市净率（股价/每股净资产）
////                BigDecimal pbRatio = BigDecimal.ZERO;
////                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
////                    pbRatio = stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP);
////                }
////                BigDecimal totalAssets = formatNumericCellValue(row.getCell(5));
////                BigDecimal totalLiabilities = formatNumericCellValue(row.getCell(6));
////                BigDecimal netAssets = formatNumericCellValue(row.getCell(7));
////
////                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName, marketCap.setScale(5, RoundingMode.UNNECESSARY),
////                        pbRatio.setScale(6, RoundingMode.UNNECESSARY), stockPrice.setScale(5, RoundingMode.UNNECESSARY),
////                        totalShares.longValue(), bookValuePerShare.setScale(5, RoundingMode.UNNECESSARY), totalAssets.setScale(5,
////                        RoundingMode.UNNECESSARY),
////                        totalLiabilities.setScale(5, RoundingMode.UNNECESSARY), netAssets.setScale(5, RoundingMode.UNNECESSARY));
////                stockInfos.add(stockInfo);
////            }
////            if (stockInfos.size() > 0) {
////                stockInfoMapper.batchInsertNew2(stockInfos);
////            }
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
//
////    public void process_7() {
////        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_11); Workbook workbook = new XSSFWorkbook(file)) {
////            Sheet sheet = workbook.getSheetAt(0);
////            int startRow = 1;
////            List<StockInfo> stockInfos = new ArrayList<>();
////            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
////                Row row = sheet.getRow(i);
////                if (row == null) continue;
////                String tickerSymbol = getCellValue(row.getCell(0));
////                String tickerName = getCellValue(row.getCell(1));
////
////                BigDecimal annualPriceChange = formatNumericCellValue(row.getCell(2));
////                BigDecimal maxPrice5y = formatNumericCellValue(row.getCell(3));
////
////                StockInfo stockInfo = new StockInfo(tickerSymbol, tickerName, annualPriceChange, maxPrice5y);
////                stockInfos.add(stockInfo);
////            }
////            if (stockInfos.size() > 0) {
////                stockInfoMapper.batchInsertNew3(stockInfos);
////            }
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
//
//
//    public void process_8(String url) {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllNew();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//        try (FileInputStream file = new FileInputStream(url); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            List<JSONObject> dataList = new ArrayList<>();
//            Map<String, Integer> revenueColumns = new LinkedHashMap<>();
//            int startRow = 1;
//            Row headerRow = sheet.getRow(0);
//            for (Cell cell : headerRow) {
//                String header = cell.getStringCellValue();
//                if (header.contains("实际披露日期")) {
//                    String date = header.replace("实际披露日期", "").replace(".", "").trim();
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
//                JSONObject announceDateJson = new JSONObject();
//                for (Map.Entry<String, Integer> entry : revenueColumns.entrySet()) {
//                    announceDateJson.put(entry.getKey(), formatNumericCellValue(row.getCell(entry.getValue())));
//                }
//                dataMap.put("announceDate", announceDateJson);
//                HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//                if (historicalStocksInfo != null) {
//                    String announceDate = historicalStocksInfo.getAnnounceDate();
//                    if (StringUtils.isNotEmpty(announceDate)) {
//                        JSONObject announceDateOld = JSONObject.parseObject(announceDate);
//                        for (String key : announceDateJson.keySet()) {
//                            announceDateOld.put(key, announceDateJson.get(key));
//                        }
//                        dataMap.put("announceDate", announceDateOld);
//                    }
//                }
//                dataList.add(dataMap);
//            }
//            List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList),
//                    HistoricalStocksInfo.class);
//            if (historicalStocksInfos.size() > 0) {
//                List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//                for (List<HistoricalStocksInfo> batch : batches) {
//                    historicalStocksInfoMapper.batchInsertOrUpdate4(batch);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void process_dividendsData() {
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllDividendsData();
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//        List<Test3> testList = new ArrayList<>();
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_DAILY_DIVIDENDS); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String dataStr = StockInfoService.ALL_TICKER_SYMBOL_MAP.get(tickerSymbol);
//                if (StringUtils.isNotEmpty(dataStr)) {
//                    String tickerSymbolNew = dataStr.split("_")[0];
//                    BigDecimal a = formatNumericCellValue(row.getCell(1));
//                    BigDecimal b = formatNumericCellValue(row.getCell(2));
//                    BigDecimal m = formatNumericCellValue(row.getCell(3));
//                    String date = getCellValue(row.getCell(4));
//                    if (StringUtils.isNotEmpty(date)) {
//                        testList.add(new Test3(tickerSymbolNew, a, b, m, date.replace("-", "")));
//                    }
//                } else {
//                    log.info("{}", tickerSymbol);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Map<String, List<Test3>> groupedMap2 = testList.stream().collect(Collectors.groupingBy(Test3::getTickerSymbol));
//
//        Map<String, Map<String, List<JSONObject>>> dateToAmountsNewMap = new HashMap<>();
//
//        for (Map.Entry<String, List<Test3>> stringListEntry : groupedMap2.entrySet()) {
//            List<Test3> value = stringListEntry.getValue();
//            // 遍历 value 列表并将数据添加到 map 中
//            Map<String, List<JSONObject>> dateToAmountsMap = new HashMap<>();
//            for (Test3 test : value) {
//                String date = test.getDate();
//                // 如果 map 中已经包含该时间点，获取对应的金额列表
//                List<JSONObject> amounts = dateToAmountsMap.getOrDefault(date, new ArrayList<>());
//                BigDecimal a = test.getA();
//                BigDecimal b = test.getB();
//                BigDecimal m = test.getM();
//                JSONObject data = new JSONObject();
//                data.put("a", a.stripTrailingZeros().toPlainString());
//                data.put("b", b.stripTrailingZeros().toPlainString());
//                data.put("m", m.stripTrailingZeros().toPlainString());
//                if (amounts.size() == 0) {
//                    amounts.add(data);
//                    dateToAmountsMap.put(date, amounts);
//                } else {
//                    System.out.println(1111);
//                }
//            }
//            dateToAmountsNewMap.put(stringListEntry.getKey(), dateToAmountsMap);
//        }
//
//        List<JSONObject> dataList = new ArrayList<>();
//        for (Map.Entry<String, Map<String, List<JSONObject>>> stringMapEntry : dateToAmountsNewMap.entrySet()) {
//            JSONObject dividendsDataJson = new JSONObject();
//            String tickerSymbol = stringMapEntry.getKey();
//            dividendsDataJson.put("tickerSymbol", tickerSymbol);
//            Map<String, List<JSONObject>> value = stringMapEntry.getValue();
//            HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//            String dividendsData = historicalStocksInfo.getDividendsData();
//            if (StringUtils.isNotEmpty(dividendsData)) {
//                JSONObject dividendsDataOld = JSONObject.parseObject(dividendsData);
//                for (String key : value.keySet()) {
//                    dividendsDataOld.put(key, value.get(key));
//                }
//                dividendsDataJson.put("dividendsData", dividendsDataOld);
//            }
//            dataList.add(dividendsDataJson);
//        }
//        List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList), HistoricalStocksInfo.class);
//
//
//        if (historicalStocksInfos.size() > 0) {
//            List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//            for (List<HistoricalStocksInfo> batch : batches) {
//                historicalStocksInfoMapper.batchInsertOrUpdateDividendsData(batch);
//            }
//        }
//    }
//
//    public void process_dailyDividends() {
//        List<Test2> testList = new ArrayList<>();
//
//        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAllDailyDividends();
//
//        Map<String, HistoricalStocksInfo> uniqueMap = singleAll.stream()
//                .collect(Collectors.toMap(HistoricalStocksInfo::getTickerSymbol, stock -> stock,
//                        (existing, replacement) -> existing));
//
//
//        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH_DAILY_DIVIDENDS); Workbook workbook = new XSSFWorkbook(file)) {
//            Sheet sheet = workbook.getSheetAt(0);
//            int startRow = 1;
//            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//                String tickerSymbol = getCellValue(row.getCell(0));
//                String dataStr = StockInfoService.ALL_TICKER_SYMBOL_MAP.get(tickerSymbol);
//                if (StringUtils.isNotEmpty(dataStr)) {
//                    String tickerSymbolNew = dataStr.split("_")[0];
//                    BigDecimal amount = formatNumericCellValue(row.getCell(2));
//                    String date = getCellValue(row.getCell(3));
//
//                    if (StringUtils.isNotEmpty(date)) {
//                        testList.add(new Test2(tickerSymbolNew, amount, date.replace("-", "")));
//                    }
//                } else {
//                    log.info("{}", tickerSymbol);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Map<String, List<Test2>> groupedMap2 = testList.stream().collect(Collectors.groupingBy(Test2::getTickerSymbol));
//
//
//        Map<String, Map<String, List<BigDecimal>>> dateToAmountsNewMap = new HashMap<>();
//
//        for (Map.Entry<String, List<Test2>> stringListEntry : groupedMap2.entrySet()) {
//            List<Test2> value = stringListEntry.getValue();
//
//            Map<String, List<BigDecimal>> dateToAmountsMap = new HashMap<>();
//            for (Test2 test : value) {
//                String date = test.getDate();
//                BigDecimal amount = test.getAmount();
//                // 如果 map 中已经包含该时间点，获取对应的金额列表
//                List<BigDecimal> amounts = dateToAmountsMap.getOrDefault(date, new ArrayList<>());
//                // 将当前金额添加到列表中
//                amounts.add(amount);
//                // 将更新后的列表放回到 map 中
//                dateToAmountsMap.put(date, amounts);
//            }
//            dateToAmountsNewMap.put(stringListEntry.getKey(), dateToAmountsMap);
//        }
//
//        List<JSONObject> dataList = new ArrayList<>();
//
//        for (Map.Entry<String, Map<String, List<BigDecimal>>> stringMapEntry : dateToAmountsNewMap.entrySet()) {
//            JSONObject dailyDividendsJson = new JSONObject();
//            String tickerSymbol = stringMapEntry.getKey();
//            dailyDividendsJson.put("tickerSymbol", tickerSymbol);
//            Map<String, List<BigDecimal>> value = stringMapEntry.getValue();
//            HistoricalStocksInfo historicalStocksInfo = uniqueMap.get(tickerSymbol);
//            String dailyDividends = historicalStocksInfo.getDailyDividends();
//            if (StringUtils.isNotEmpty(dailyDividends)) {
//                JSONObject dailyDividendsOld = JSONObject.parseObject(dailyDividends);
//                for (String key : value.keySet()) {
//                    dailyDividendsOld.put(key, value.get(key));
//                }
//                dailyDividendsJson.put("dailyDividends", dailyDividendsOld);
//            }
//            dataList.add(dailyDividendsJson);
//        }
//        List<HistoricalStocksInfo> historicalStocksInfos = JSONArray.parseArray(JSON.toJSONString(dataList), HistoricalStocksInfo.class);
//
//        if (historicalStocksInfos.size() > 0) {
//            List<List<HistoricalStocksInfo>> batches = createBatches(historicalStocksInfos, BATCH_SIZE);
//            for (List<HistoricalStocksInfo> batch : batches) {
//                historicalStocksInfoMapper.batchInsertOrUpdateDailyDividends(batch);
//            }
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
//    @Data
//    static class Test2 {
//        private String tickerSymbol;
//        private BigDecimal amount;
//        private String date;
//
//        public Test2(String tickerSymbol, BigDecimal amount, String date) {
//            this.tickerSymbol = tickerSymbol;
//            this.amount = amount;
//            this.date = date;
//        }
//
//        public Test2() {
//        }
//    }
//
//    @Data
//    static class Test3 {
//        private String tickerSymbol;
//        private BigDecimal a;
//        private BigDecimal b;
//        private BigDecimal m;
//        private String date;
//
//        public Test3(String tickerSymbol, BigDecimal a, BigDecimal b, BigDecimal m, String date) {
//            this.tickerSymbol = tickerSymbol;
//            this.a = a;
//            this.b = b;
//            this.m = m;
//            this.date = date;
//        }
//
//        public Test3() {
//        }
//    }
//
//
//    public static String parseOrZero(String str) {
//        if (str == null || str.isEmpty()) {
//            return "0";
//        }
//        try {
//            // 尝试解析为浮点数
//            Double.parseDouble(str);
//            return str;
//        } catch (NumberFormatException e) {
//            return "0";
//        }
//    }
//
//
//}