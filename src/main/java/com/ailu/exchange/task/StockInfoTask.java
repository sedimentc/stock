package com.ailu.exchange.task;

import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.HkStockInfoMapper;
import com.ailu.exchange.dao.mapper.source1.StockInfoMapper;
import com.ailu.exchange.dao.mapper.source1.UsStockInfoMapper;
import com.ailu.exchange.rest.api.TuShareApi;
import com.ailu.exchange.service.HkStockInfoService;
import com.ailu.exchange.service.StockInfoService;
import com.ailu.exchange.service.UsStockInfoService;
import com.ailu.exchange.utils.DataProcessorUtil;
import com.ailu.exchange.utils.DateUtils;
import com.ailu.exchange.utils.OkHttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.http.POST;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
public class StockInfoTask {
    @Resource
    StockInfoService stockInfoService;
    @Resource
    HkStockInfoService hkStockInfoService;
    @Resource
    StockInfoMapper stockInfoMapper;
    @Resource
    HkStockInfoMapper hkStockInfoMapper;

    @Resource
    TuShareApi tuShareApi;
    @Resource
    OkHttpUtil okHttpUtil;

    @Resource
    UsStockInfoService usStockInfoService;

    @Resource
    UsStockInfoMapper usStockInfoMapper;

    private static final String TOKEN = "REDACTED_TUSHARE_TOKEN";

    @PostConstruct
    public void initStockInfo() {
        List<StockInfo> allTickerSymbol = stockInfoMapper.getAllTickerSymbol();
        List<StockInfo> hkAllTickerSymbol = hkStockInfoMapper.getAllTickerSymbol();
        List<StockInfo> usAllTickerSymbol = usStockInfoMapper.getAllTickerSymbol();
        setMapInfo(allTickerSymbol, hkAllTickerSymbol, usAllTickerSymbol);

//        setMapInfo(allTickerSymbol, new ArrayList<>(), new ArrayList<>());

        log.info("开始初始化 A股 股票历史信息");
        stockInfoService.cacheHistoricalStocksInfo();
        log.info("结束初始化  A股 股票历史信息");
        log.info("开始初始化 港股 股票历史信息");
        hkStockInfoService.cacheHistoricalStocksInfo();
        log.info("结束初始化  港股 股票历史信息");
        log.info("开始初始化 美股 股票历史信息");
        usStockInfoService.cacheHistoricalStocksInfo();
        log.info("结束初始化 美股 股票历史信息");
    }

    private void setMapInfo(List<StockInfo> allTickerSymbol, List<StockInfo> hkAllTickerSymbol,
                            List<StockInfo> usAllTickerSymbol) {
        Map<String, String> tickerMap = allTickerSymbol.stream()
                .collect(Collectors.toMap(
                        stock -> stock.getTickerSymbol().split("\\.")[0],
                        stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(), (existing, replacement) -> existing
                ));

        Map<String, String> hkTickerHkMap = hkAllTickerSymbol.stream()
                .collect(Collectors.toMap(
                        stock -> stock.getTickerSymbol().split("\\.")[0],
                        StockInfo::getTickerSymbol, (existing, replacement) -> existing
                ));

        Map<String, String> usTickerHkMap = usAllTickerSymbol.stream()
                .collect(Collectors.toMap(
                        StockInfo::getTickerSymbol,
                        StockInfo::getTickerSymbolNew,
                        (existingValue, newValue) -> existingValue
                ));

        Map<String, String> tickerMapNew = allTickerSymbol.stream()
                .collect(Collectors.toMap(
                        StockInfo::getTickerName,
                        stock -> stock.getTickerSymbol() + "_" + stock.getTickerName(),
                        (existing, replacement) -> existing
                ));

        //A股
        StockInfoService.ALL_TICKER_SYMBOL_MAP.putAll(tickerMap);
        StockInfoService.ALL_TICKER_SYMBOL_MAP_2.putAll(tickerMapNew);
        //港股
        HkStockInfoService.ALL_TICKER_SYMBOL_MAP.putAll(hkTickerHkMap);
        //美股
        UsStockInfoService.ALL_TICKER_SYMBOL_MAP.putAll(usTickerHkMap);
    }


    @Scheduled(cron = "0 10 17 * * ?")
//    @Scheduled(initialDelay = 1000L, fixedDelay = 10 * 60 * 60 * 1000L)
    public void updateKxianData() {
        String currentDateStr = DateUtils.getCurrentDateStr();
        long startTime = System.currentTimeMillis();
        String filePath = "/home/tomcat/aShare_data/date.txt";
        log.info("开始同步每日最新的股票价格 开始时间:{},日期:{}", startTime, currentDateStr);
        List<String> list = DateUtils.generateDateRange(currentDateStr, currentDateStr);
        // 使用 HashMap 进行分组
        Map<String, List<JSONArray>> groupedDailyMap = new HashMap<>();
        Map<String, List<JSONArray>> groupedDailyBasicMap = new HashMap<>();
        //设置最新k线数据和最新的股本总数
        getDataInfo(list, groupedDailyMap, groupedDailyBasicMap);
//        //更新历史保存的k线数据
        updateKData(groupedDailyMap);
//        //更新历史的股本总数
        updateDailyBasicData(groupedDailyBasicMap);
//        获取A股现有股票代码
        List<StockInfo> allTickerSymbol = stockInfoMapper.getAllTickerSymbolNew();
        if (groupedDailyMap.size() > 0) {
            log.info("获取的A股股票信息长度:{}", groupedDailyMap.size());
            List<StockInfo> stockInfos = getStockInfos(groupedDailyMap, allTickerSymbol);
            if (stockInfos.size() > 0) {
                stockInfoMapper.batchInsertNew3(stockInfos);
            }
            List<StockInfo> stockInfoMarketCap = getStockInfoMarketCap(groupedDailyBasicMap, allTickerSymbol);
            if (stockInfoMarketCap.size() > 0) {
                stockInfoMapper.batchInsertMarketCap(stockInfoMarketCap);
            }
            log.info("开始更新 A股 股票历史信息");
            stockInfoService.cacheHistoricalStocksInfo();
            log.info("结束更新  A股 股票历史信息");
        }

//        获取港股现有股票代码
        List<StockInfo> hkAllTickerSymbol = hkStockInfoMapper.getAllTickerSymbolNew();
        Map<String, JSONObject> closePriceMap = getClosePriceMap();
        if (closePriceMap.size() > 0) {
            log.info("获取的港股股票信息长度:{}", closePriceMap.size());
            List<StockInfo> hkStockInfos = getHkStockInfos(closePriceMap, hkAllTickerSymbol);
            if (hkStockInfos.size() > 0) {
                hkStockInfoMapper.batchInsertNew3(hkStockInfos);
            }
            log.info("开始更新 港股 股票历史信息");
            hkStockInfoService.cacheHistoricalStocksInfo();
            log.info("结束更新  港股 股票历史信息");
        }
        List<StockInfo> allTickerSymbolNew = usStockInfoMapper.getAllTickerSymbolNew();
        Map<String, JSONObject> usClosePriceMap = getUsClosePriceMap();
        if (usClosePriceMap.size() > 0) {
            log.info("获取的美股股票信息长度:{}", usClosePriceMap.size());
            List<StockInfo> usStockInfos = getUsStockInfos(usClosePriceMap, allTickerSymbolNew);
            if (usStockInfos.size() > 0) {
                usStockInfoMapper.batchInsertNew3(usStockInfos);
            }
            log.info("开始更新 美股 股票历史信息");
            hkStockInfoService.cacheHistoricalStocksInfo();
            log.info("结束更新 美股 股票历史信息");
        }

        log.info("结束同步每日最新的股票价格 结束用时:{}", System.currentTimeMillis() - startTime);
        DataProcessorUtil.writeToFile(filePath, currentDateStr);
    }

    private void getDataInfo(List<String> list, Map<String, List<JSONArray>> groupedDailyMap,
                             Map<String, List<JSONArray>> groupedDailyBasicMap) {
        for (String tradeDate : list) {
            JSONObject jsonObjectDaily = getJsonObject("daily", tradeDate);
            if (jsonObjectDaily.getInteger("code").equals(0)) {
                JSONArray jsonArray = jsonObjectDaily.getJSONObject("data").getJSONArray("items");
                IntStream.range(0, jsonArray.size()).forEach(i -> {
                    JSONArray innerArray = jsonArray.getJSONArray(i);
                    // 假设我们要根据数组中的第一个元素进行分组
                    String key = innerArray.getString(0);
                    // 如果分组尚不存在，则创建一个新的 List<JSONArray>
                    groupedDailyMap.computeIfAbsent(key, k -> new ArrayList<>()).add(innerArray);
                });
            } else {
                log.info("获取daily接口数据出现异常！！{}", tradeDate);
            }
            JSONObject jsonObjectDailyBasic = getJsonObject("daily_basic", tradeDate);
            if (jsonObjectDailyBasic.getInteger("code").equals(0)) {
                JSONArray jsonArray = jsonObjectDailyBasic.getJSONObject("data").getJSONArray("items");
                IntStream.range(0, jsonArray.size()).forEach(i -> {
                    JSONArray innerArray = jsonArray.getJSONArray(i);
                    // 假设我们要根据数组中的第一个元素进行分组
                    String key = innerArray.getString(0);
                    // 如果分组尚不存在，则创建一个新的 List<JSONArray>
                    groupedDailyBasicMap.computeIfAbsent(key, k -> new ArrayList<>()).add(innerArray);
                });
            } else {
                log.info("获取daily_basic接口数据出现异常！！{}", tradeDate);
            }
        }
    }


    public void getTotalShareData(String tradeDate, Map<String, BigDecimal> totalShareDataMap) {
        JSONObject jsonObjectDaily = getJsonObject("hk_daily_adj", tradeDate);
        if (jsonObjectDaily.getInteger("code").equals(0)) {
            JSONArray jsonArray = jsonObjectDaily.getJSONObject("data").getJSONArray("items");
            IntStream.range(0, jsonArray.size()).forEach(i -> {
                JSONArray innerArray = jsonArray.getJSONArray(i);
                // 假设我们要根据数组中的第一个元素进行分组
                String key = innerArray.getString(0).split("\\.")[0];
                BigDecimal totalShare = innerArray.getBigDecimal(15);
                // 如果分组尚不存在，则创建一个新的 List<JSONArray>
                totalShareDataMap.put(processString(key) + ".HK", totalShare);
            });
        } else {
            log.info("获取hk_daily_adj接口数据出现异常！！{}", tradeDate);
        }
    }

    private Map<String, JSONObject> getClosePriceMap() {
        Map<String, JSONObject> closePriceMap = new HashMap<>();
        try {
            String dataStr = okHttpUtil.httpGet("https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2" +
                    ".php/Market_Center.getHKStockData?page=1&sort=symbol&asc=1&node=qbgg_hk&num=3000&_s_r_a=sort", null);
            if (StringUtils.isNotEmpty(dataStr) && !dataStr.equalsIgnoreCase("null")) {
                JSONArray jsonArray = JSONArray.parseArray(dataStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String symbol = jsonObject.getString("symbol");
                    //当前股价
                    BigDecimal lastRade = jsonObject.getBigDecimal("lasttrade");
                    //当前总股本
                    Long stocksSum = jsonObject.getLong("stocks_sum");
                    JSONObject data = new JSONObject();
                    data.put("stockPrice", lastRade);
                    data.put("totalShares", stocksSum);
                    closePriceMap.put(processString(symbol) + ".HK", data);
                }
            }
        } catch (Exception e) {
            log.error("获取港股当前价格出现异常！！", e);
        }
        return closePriceMap;
    }


    private static final Random random = new Random();

    private Map<String, JSONObject> getUsClosePriceMap() {
        Map<String, JSONObject> closePriceMap = new HashMap<>();
        try {
            for (int j = 1; j < 130; j++) {
                String dataStr = okHttpUtil.httpGetNew(
                        "https://stock.finance.sina.com.cn/usstock/api/jsonp.php/IO.XSRV2.CallbackList['f0j3ltzVzdo2Fo4p']" +
                                "/US_CategoryService.getList?page=" + j + "&num=60&sort=&asc=0&market=&id=", null);
                int sleepTime = random.nextInt(1000); // 生成 0 到 999 毫秒的随机数
                Thread.sleep(sleepTime);
                if (StringUtils.isNotEmpty(dataStr) && !dataStr.equalsIgnoreCase("null")) {
                    JSONObject jsonObject1 = JSONObject.parseObject(dataStr);
                    JSONArray jsonArray = jsonObject1.getJSONArray("data");
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String symbol = jsonObject.getString("symbol");
                        //当前股价
                        BigDecimal lastRade = jsonObject.getBigDecimal("price");
                        //当前总市值
                        Long mktCap = jsonObject.getLong("mktcap");
                        JSONObject data = new JSONObject();
                        data.put("stockPrice", lastRade);
                        //总市值除以当前价格等于总股本
                        data.put("totalShares", new BigDecimal(mktCap).divide(lastRade, 4, RoundingMode.HALF_UP));
                        closePriceMap.put(UsStockInfoService.ALL_TICKER_SYMBOL_MAP.get(symbol), data);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取港股当前价格出现异常！！", e);
        }
        return closePriceMap;
    }


    private static String processString(String str) {
        // 如果字符串长度等于五位且第一位是零，则去除开头的零
        if (str.length() == 5 && str.charAt(0) == '0') {
            str = str.substring(1);
        }
        return str;
    }

    /**
     * 补零 如果长度少于五位前面默认补零
     *
     * @param str
     * @return
     */
    private static String padToFiveDigits(String str) {
        return String.format("%05d", Integer.parseInt(str));
    }

    private static List<StockInfo> getStockInfos(Map<String, List<JSONArray>> groupedDailyMap, List<StockInfo> allTickerSymbol) {
        List<StockInfo> stockInfos = new ArrayList<>();
        for (StockInfo stockInfo : allTickerSymbol) {
            String tickerSymbol = stockInfo.getTickerSymbol();
            BigDecimal maxPrice5y = stockInfo.getMaxPrice5y();
            List<JSONArray> jsonArrays = groupedDailyMap.get(tickerSymbol);
            BigDecimal stockPrice = new BigDecimal("0");
            if (jsonArrays != null && jsonArrays.size() > 0) {
                stockPrice = jsonArrays.get(jsonArrays.size() - 1).getBigDecimal(5);
            }
            BigDecimal annualPriceChange = BigDecimal.ZERO;
            if (maxPrice5y != null && maxPrice5y.compareTo(BigDecimal.ZERO) != 0) {
                annualPriceChange =
                        stockPrice.subtract(maxPrice5y).divide(maxPrice5y, 4, RoundingMode.HALF_UP).multiply(new BigDecimal
                                (100));
            }
            StockInfo stockInfo1 = new StockInfo();
            stockInfo1.setTickerSymbol(tickerSymbol);
            stockInfo1.setStockPrice(stockPrice);
            stockInfo1.setAnnualPriceChange(annualPriceChange);
            stockInfos.add(stockInfo1);
        }
        return stockInfos;
    }

    private static List<StockInfo> getStockInfoMarketCap(Map<String, List<JSONArray>> groupedDailyBasicMap,
                                                         List<StockInfo> allTickerSymbol) {
        List<StockInfo> stockInfos = new ArrayList<>();
        for (StockInfo stockInfo : allTickerSymbol) {
            String tickerSymbol = stockInfo.getTickerSymbol();
            List<JSONArray> jsonArrays = groupedDailyBasicMap.get(tickerSymbol);
            BigDecimal marketCap = new BigDecimal("0");
            BigDecimal totalShare = new BigDecimal("0");
            if (jsonArrays != null && jsonArrays.size() > 0) {
                marketCap = jsonArrays.get(jsonArrays.size() - 1).getBigDecimal(16);
                totalShare = jsonArrays.get(jsonArrays.size() - 1).getBigDecimal(13);
            }
            StockInfo stockInfoNew = new StockInfo();
            BigDecimal totalShareNew = totalShare.multiply(new BigDecimal(10000));
            BigDecimal marketCapNew = marketCap.multiply(new BigDecimal(10000));
            stockInfoNew.setTickerSymbol(tickerSymbol);
            stockInfoNew.setMarketCap(marketCapNew);
            stockInfoNew.setTotalShares(totalShareNew.longValue());
            stockInfos.add(stockInfoNew);
        }
        return stockInfos;
    }

    private static List<StockInfo> getHkStockInfoMarketCap(JSONObject groupedDailyBasicMap,
                                                           List<StockInfo> allTickerSymbol) {
        List<StockInfo> stockInfos = new ArrayList<>();
        for (StockInfo stockInfo : allTickerSymbol) {
            String tickerSymbol = stockInfo.getTickerSymbol();
            BigDecimal stockPrice = stockInfo.getStockPrice();
            BigDecimal totalShare = groupedDailyBasicMap.getBigDecimal(padToFiveDigits(tickerSymbol.split("\\.")[0]) + ".HK");
            if (totalShare == null) {
                totalShare = new BigDecimal("0");
            }
            StockInfo stockInfoNew = new StockInfo();

            stockInfoNew.setTickerSymbol(tickerSymbol);
            stockInfoNew.setMarketCap(stockPrice.multiply(totalShare));
            stockInfoNew.setTotalShares(totalShare.longValue());
            stockInfos.add(stockInfoNew);
        }
        return stockInfos;
    }

    private static List<StockInfo> getHkStockInfos(Map<String, JSONObject> closingPriceMap,
                                                   List<StockInfo> hkAllTickerSymbol) {
        List<StockInfo> stockInfos = new ArrayList<>();
        for (StockInfo stockInfo : hkAllTickerSymbol) {
            String tickerSymbol = stockInfo.getTickerSymbol();
            BigDecimal maxPrice5y = stockInfo.getMaxPrice5y();
            JSONObject data = closingPriceMap.get(tickerSymbol);

            BigDecimal stockPrice = Optional.ofNullable(data)
                    .map(d -> d.getBigDecimal("stockPrice"))
                    .orElse(stockInfo.getStockPrice());

            Long totalShares = Optional.ofNullable(data)
                    .map(d -> d.getLong("totalShares"))
                    .orElse(stockInfo.getTotalShares());

            BigDecimal annualPriceChange = calculateAnnualPriceChange(stockPrice, maxPrice5y);

            BigDecimal totalMarketValue = stockPrice.multiply(new BigDecimal(totalShares));

            stockInfos.add(new StockInfo(tickerSymbol, stockPrice, annualPriceChange, totalMarketValue, totalShares));
        }
        return stockInfos;
    }

    private static List<StockInfo> getUsStockInfos(Map<String, JSONObject> closingPriceMap,
                                                   List<StockInfo> hkAllTickerSymbol) {
        List<StockInfo> stockInfos = new ArrayList<>();
        for (StockInfo stockInfo : hkAllTickerSymbol) {
            try {
                String tickerSymbol = stockInfo.getTickerSymbolNew();
                BigDecimal maxPrice5y = stockInfo.getMaxPrice5y();
                JSONObject data = closingPriceMap.get(tickerSymbol);
                BigDecimal stockPrice = Optional.ofNullable(data)
                        .map(d -> d.getBigDecimal("stockPrice"))
                        .orElse(stockInfo.getStockPrice());

                Long totalShares = Optional.ofNullable(data)
                        .map(d -> d.getLong("totalShares"))
                        .orElse(stockInfo.getTotalShares());

                BigDecimal annualPriceChange = calculateAnnualPriceChange(stockPrice, maxPrice5y);

                BigDecimal totalMarketValue = stockPrice.multiply(new BigDecimal(totalShares));

                stockInfos.add(new StockInfo(tickerSymbol, stockPrice, annualPriceChange, totalMarketValue, totalShares));
            } catch (Exception e) {
                log.error("处理数据出现异常！！", e);
            }
        }
        return stockInfos;
    }

    private static BigDecimal calculateAnnualPriceChange(BigDecimal stockPrice, BigDecimal maxPrice5y) {
        if (maxPrice5y == null || maxPrice5y.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return stockPrice.subtract(maxPrice5y)
                .divide(maxPrice5y, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
    }

    private static void updateKData(Map<String, List<JSONArray>> groupedDailyMap) {
        for (Map.Entry<String, List<JSONArray>> stringListEntry : groupedDailyMap.entrySet()) {
            String tickerSymbol = stringListEntry.getKey();
            List<JSONArray> value = stringListEntry.getValue();
            // 排序
            value.sort((o1, o2) -> {
                // 提取下标为 1 的元素并进行比较
                Comparable element1 = (Comparable) o1.get(1);
                Comparable element2 = (Comparable) o2.get(1);
                return element2.compareTo(element1); // 降序
            });
            try {
                String zipFilePath = "/home/tomcat/aShare_data/Agu_day_K/";
                String fileNameToReplace = tickerSymbol + "_day_K.zip";
                String fileNameInZip = tickerSymbol + "_day_K.txt";
                String fullZipPath = zipFilePath + fileNameToReplace;
                File zipFile = new File(fullZipPath);
                JSONObject jsonObject;
                // 检查文件是否存在，不存在则初始化
                if (!zipFile.exists()) {
                    log.info("K线文件不存在，将创建新文件: {}", fullZipPath);

                    // 确保父目录存在
                    File parentDir = zipFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("无法创建父目录: " + parentDir.getAbsolutePath());
                    }

                    // 初始化基础JSON结构
                    jsonObject = new JSONObject();
                    JSONObject data = new JSONObject();
                    JSONArray items = new JSONArray();
                    data.put("items", items);
                    jsonObject.put("data", data);
                } else {
                    // 文件存在时读取现有内容
                    jsonObject = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(fullZipPath));
                }

                // 确保JSON结构完整性（防止空指针）
                JSONObject data = jsonObject.getJSONObject("data");
                if (data == null) {
                    data = new JSONObject();
                    jsonObject.put("data", data);
                }
                JSONArray jsonArray = data.getJSONArray("items");
                if (jsonArray == null) {
                    jsonArray = new JSONArray();
                    data.put("items", jsonArray);
                }
                // 添加新数据
                jsonArray.addAll(0, value);

                // 保存更新后的数据
                String dataStr = jsonObject.toJSONString();
                DataProcessorUtil.createUpdatedZipFile(fullZipPath, fileNameInZip, dataStr);

            } catch (FileNotFoundException e) {
                log.error("K线文件未找到，已尝试自动创建: {}", tickerSymbol, e);
            } catch (IOException e) {
                log.error("处理股票K线信息出现异常: {}", tickerSymbol, e);
            }
        }
    }

    private static void updateDailyBasicData(Map<String, List<JSONArray>> groupedDailyBasicMap) {
        for (Map.Entry<String, List<JSONArray>> stringListEntry : groupedDailyBasicMap.entrySet()) {
            String tickerSymbol = stringListEntry.getKey();
            List<JSONArray> value = stringListEntry.getValue();
            // 排序
            value.sort((o1, o2) -> {
                // 提取下标为 1 的元素并进行比较
                Comparable element1 = (Comparable) o1.get(1);
                Comparable element2 = (Comparable) o2.get(1);
                return element2.compareTo(element1); // 降序
            });
            try {
                String zipFilePath = "/home/tomcat/aShare_data/Agu_daily_basic/";
                String fileNameToReplace = tickerSymbol + "_daily_basic.zip";
                String fileNameInZip = tickerSymbol + "_daily_basic.txt";
                String fullZipPath = zipFilePath + fileNameToReplace;
                File zipFile = new File(fullZipPath);
                JSONObject jsonObject;
                // 检查文件是否存在
                if (!zipFile.exists()) {
                    log.info("文件不存在，将创建新文件: {}", fullZipPath);
                    // 确保父目录存在
                    File parentDir = zipFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("无法创建父目录: " + parentDir.getAbsolutePath());
                    }
                    // 初始化基础JSON结构
                    jsonObject = new JSONObject();
                    JSONObject data = new JSONObject();
                    JSONArray items = new JSONArray();
                    data.put("items", items);
                    jsonObject.put("data", data);
                } else {
                    // 文件存在，读取现有内容
                    jsonObject = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(fullZipPath));
                }
                // 获取items数组，不存在则创建
                JSONObject data = jsonObject.getJSONObject("data");
                if (data == null) {
                    data = new JSONObject();
                    jsonObject.put("data", data);
                }
                JSONArray jsonArray = data.getJSONArray("items");
                if (jsonArray == null) {
                    jsonArray = new JSONArray();
                    data.put("items", jsonArray);
                }
                // 添加新数据
                jsonArray.addAll(0, value);
                // 保存更新后的数据
                String dataStr = jsonObject.toJSONString();
                DataProcessorUtil.createUpdatedZipFile(fullZipPath, fileNameInZip, dataStr);

            } catch (FileNotFoundException e) {
                // 理论上不会走到这里，因为前面已经做了存在性检查
                log.error("文件未找到，已尝试自动创建: {}", tickerSymbol, e);
            } catch (IOException e) {
                log.error("处理股票k线信息出现异常: {}", tickerSymbol, e);
            }
        }
    }


    private JSONObject getJsonObject(String apiName, String tradeDate) {
        try {
            JSONObject paramsJson = new JSONObject();
            paramsJson.put("api_name", apiName);
            paramsJson.put("token", TOKEN);
            JSONObject params = new JSONObject();
            params.put("trade_date", tradeDate);
            paramsJson.put("params", params);
            return tuShareApi.genericMethods(paramsJson).execute().body();
        } catch (IOException e) {
            log.error("请求接口获取数据出现异常！！接口名称:{},股票代码:{}", apiName, tradeDate, e);
        }
        return new JSONObject();
    }

    public static void main(String[] args) {
        String currentDateStr = DateUtils.getCurrentDateStr();
        List<String> list = DateUtils.generateDateRange("20250818", currentDateStr);

        System.out.println(currentDateStr);
        System.out.println(list);
    }

}
