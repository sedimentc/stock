package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.HistoricalStocksInfoMapper;
import com.ailu.exchange.dao.mapper.source1.StockInfoMapper;
import com.ailu.exchange.dao.vo.ConditionLogVo;
import com.ailu.exchange.dao.vo.DailyVo;
import com.ailu.exchange.rest.api.TuShareApi;
import com.ailu.exchange.service.redis.ObjectRedisService;
import com.ailu.exchange.utils.DataProcessorUtil;
import com.ailu.exchange.utils.DateUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.scripts.JS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.ailu.exchange.utils.FilterUtils.applyFilter;
import static com.ailu.exchange.utils.FilterUtils.getComparator;

@Service
@Slf4j
public class StockInfoService {

    @Resource
    StockInfoMapper stockInfoMapper;

    @Resource
    HistoricalStocksInfoMapper historicalStocksInfoMapper;

    /**
     * 2000-2024年净利润
     */
    public final static Map<String, Map<String, Map<String, Object>>> NET_PROFIT_CACHE_MAP = new HashMap<>();

    /**
     * 2000-2024年营业收入
     */
    public final static Map<String, Map<String, Map<String, Object>>> GROSS_REVENUE_CACHE_MAP = new HashMap<>();
    /**
     * 2000-2023年 分红金额
     */
    public final static Map<String, JSONObject> TOTAL_BONUS_CACHE_MAP = new HashMap<>();
    /**
     * 2000-2023年 实际披露日期
     */
    public final static Map<String, JSONObject> ANNOUNCE_DATE_CACHE_MAP = new HashMap<>();
    /**
     * 2000-2023年 每日分红
     */
    public final static Map<String, JSONObject> DAILY_DIVIDENDS_CACHE_MAP = new HashMap<>();
    /**
     * 2000-2023年 净资产
     */
    public final static Map<String, JSONObject> BVPS_DATA_CACHE_MAP = new HashMap<>();
    /**
     * 派息日对应的 派息日期的派息详情
     */
    public final static Map<String, JSONObject> DIVIDENDS_DATA_CACHE_MAP = new HashMap<>();
    /**
     * A股基础信息全局缓存
     */
    public final static List<StockInfo> STOCK_INFO_LIST_GLOBAL_CACHE = new ArrayList<>();
    /**
     * 保存 取点后的股票代码 获取对应的股票代码和股票简称
     */
    public final static Map<String, String> ALL_TICKER_SYMBOL_MAP = new HashMap<>();

    /**
     * 保存 股票简称 获取对应的股票代码和股票简称
     */
    public final static Map<String, String> ALL_TICKER_SYMBOL_MAP_2 = new HashMap<>();

    private static final String TOKEN = "0bdf3baa700b2560364af43c4e6fff06fccf601ce4320bf390314f52";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");


    @Resource
    TuShareApi tuShareApi;


    @Resource
    ObjectRedisService objectRedisService;


    private static final Long LIMIT_VALUE = 10L;

    private static final String CONDITION_LOG_KEY = "CONDITION_LOG_KEY";


    /**
     * @param pageNumber    页码
     * @param pageSize      当前页数
     * @param orderByFields 排序字段
     * @param sortDirection 排序规则
     * @return 查询结果
     */
    public JSONObject getPagedAndSortedStocks(Integer pageNumber, Integer pageSize, String orderByFields, String sortDirection,
                                              JSONArray filterList) {
        if (filterList != null && filterList.size() > 0) {
            String newLog = JSON.toJSONString(new ConditionLogVo(filterList, System.currentTimeMillis() / 1000));
            // 检查是否已经存在相同的记录
            boolean exists = objectRedisService.isMemberOfSortSet(CONDITION_LOG_KEY, newLog);
            if (!exists) {
                long score = System.currentTimeMillis();
                objectRedisService.setSortSet(CONDITION_LOG_KEY, newLog, (double) score);
                try {
                    Long sizeKey = objectRedisService.getSetLength(CONDITION_LOG_KEY);
                    if (sizeKey > LIMIT_VALUE) {
                        long delPcs = sizeKey - LIMIT_VALUE;
                        //从后面数据开始删除
                        Long aLong = objectRedisService.removeSortSetByIndex(CONDITION_LOG_KEY, 0L, delPcs - 1);
                        if (aLong > 0) {
                            log.warn("{清理历史数据},key值:{},清理数量:{},集合长度:{}", LIMIT_VALUE, aLong, sizeKey);
                        }
                    }
                } catch (Exception e) {
                    log.error("清理redis缓存出现异常！！", e);
                }
            }
        }
        // 应用筛选条件
        List<StockInfo> filteredStockList = STOCK_INFO_LIST_GLOBAL_CACHE.stream().filter(stockInfo -> applyFilter(stockInfo,
                filterList)).collect(Collectors.toList());
        // 计算经过筛选后的总记录数
        long totalRecords = filteredStockList.size();
        // 分页和排序
        List<StockInfo> paginatedAndSortedStockList = filteredStockList.stream().sorted(getComparator(orderByFields,
                sortDirection)).skip((long) (pageNumber - 1) * pageSize).limit(pageSize).collect(Collectors.toList());
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        // 组装返回结果
        JSONObject data = new JSONObject();
        data.put("list", paginatedAndSortedStockList);
        data.put("totalRecords", totalRecords);
        data.put("totalPages", totalPages);
        return data;
    }


    public JSONArray getConditionLogKeyRedis() {
        Set<Object> desc = objectRedisService.getSortSetLimit("desc", CONDITION_LOG_KEY, 10);
        JSONArray conditionLogsNew = new JSONArray();
        if (desc.size() > 0) {
            // 假设 desc 是包含 JSON 字符串的列表
            List<String> collect = desc.stream().map(String::valueOf).collect(Collectors.toList());
            // 将 JSON 字符串列表转换为 JSON 数组
            String jsonArrayString = JSONArray.toJSONString(collect);
            // 解析 JSON 数组字符串为 JSONArray
            JSONArray conditionLogs = JSONArray.parseArray(jsonArrayString);
            for (int i = 0; i < conditionLogs.size(); i++) {
                JSONObject jsonObject = conditionLogs.getJSONObject(i);
                JSONArray data = jsonObject.getJSONArray("data");
                Long ctime = jsonObject.getLong("ctime");
                JSONObject dataNew = new JSONObject();
                dataNew.put("data", data.toJSONString());
                dataNew.put("ctime", ctime);
                conditionLogsNew.add(dataNew);

            }
        }
        return conditionLogsNew;
    }


    public JSONObject getAShareKData(String tickerSymbol, String datas, String type) throws Exception {
        String tickerSymbolNew = datas.split("_")[0];
        String tickerName = datas.split("_")[1];
        return getKDataInfo(tickerSymbolNew, type, tickerSymbol, tickerName);
    }


    /**
     * 缓存全局的股票数据
     *
     * @param all
     */
    private List<StockInfo> getStockInfoGlobalCache(List<StockInfo> all) {
        for (StockInfo stockInfo : all) {
            String tickerSymbol = stockInfo.getTickerSymbol();

            try {
                Map<String, Map<String, Object>> grossRevenueMap = GROSS_REVENUE_CACHE_MAP.get(tickerSymbol);
                Map<String, Map<String, Object>> netProfitMap = NET_PROFIT_CACHE_MAP.get(tickerSymbol);
                JSONObject grossRevenueJson = JSONObject.parseObject(JSON.toJSONString(grossRevenueMap));
                stockInfo.setGrossRevenueMap(grossRevenueMap);
                JSONObject netProfitJson = JSONObject.parseObject(JSON.toJSONString(netProfitMap));
                stockInfo.setNetProfitMap(netProfitMap);
                //总市值
                BigDecimal marketCap = stockInfo.getMarketCap();
                //当前年
                String currentYear = DateUtils.getCurrentYear();
                JSONObject totalBonus = TOTAL_BONUS_CACHE_MAP.get(tickerSymbol);
                //去年
                String lastYear = DateUtils.getLastYear(1);
                JSONObject currentYearJson = netProfitJson.getJSONObject(currentYear);
                //获取去年的四个季度的近利润
                JSONObject lastYearJson = netProfitJson.getJSONObject(lastYear);
                if (currentYearJson != null && lastYearJson != null) {
                    BigDecimal netProfitTotal = getNetProfitTotal(currentYearJson, lastYearJson);
                    //总市值/近四个季度净利润之和
                    stockInfo.setPeRatioTtm(marketCap.divide(netProfitTotal, 4, RoundingMode.HALF_UP));
                }
                //当前价格
                BigDecimal stockPrice = stockInfo.getStockPrice();
                //每股净资产
                BigDecimal bookValuePerShare = stockInfo.getBookValuePerShare();
                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
                    stockInfo.setPbRatio(stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP));
                }
                //获取去年的四个季度的近利润
                if (currentYearJson != null && lastYearJson != null) {
                    //重新设置当前年的营业收入最新一年的增长率
                    BigDecimal currentYearGrowthRate = getCurrentYearGrowthRate(currentYearJson, lastYearJson);
                    if (currentYearGrowthRate != null) {
                        stockInfo.getNetProfitMap().get(currentYear).put("gr", currentYearGrowthRate);
                    }
                }
                JSONObject currentYearGrossRevenueJson = grossRevenueJson.getJSONObject(currentYear);
                JSONObject lastYearGrossRevenueJson = grossRevenueJson.getJSONObject(lastYear);
                if (currentYearGrossRevenueJson != null && lastYearGrossRevenueJson != null) {
                    //重新设置当前年的净利润最新一年的增长率
                    BigDecimal currentYearGrossRevenueGrowthRate = getCurrentYearGrowthRate(currentYearGrossRevenueJson,
                            lastYearGrossRevenueJson);
                    if (currentYearGrossRevenueGrowthRate != null) {
                        stockInfo.getGrossRevenueMap().get(currentYear).put("gr", currentYearGrossRevenueGrowthRate);
                    }
                }
                if (totalBonus != null) {
                    String lastYearNew = DateUtils.getLastYear(1);
                    String theYear = DateUtils.getLastYear(2);
                    //当前年的分红总量
                    BigDecimal currentYearTotalBonus = totalBonus.getBigDecimal(lastYearNew);
                    //去年的分红率
                    BigDecimal lastYearTotalBonus = totalBonus.getBigDecimal(theYear);


                    String[] pastYearsIncludingCurrent = DateUtils.getPastYearsIncludingCurrent(3);
                    BigDecimal totalAll = BigDecimal.ZERO;
                    for (String years : pastYearsIncludingCurrent) {
                        BigDecimal total = totalBonus.getBigDecimal(years);
                        if (total != null) {
                            totalAll = totalAll.add(total);
                        }
                    }
                    //2025年股息率=2024年分红总额/总市值
                    //2024年股息率=2023年分红总额/总市值
                    //2023年股息率=2022年分红总额/总市值
                    //获取当前年的四个季度的近利润
                    if (marketCap.compareTo(BigDecimal.ZERO) != 0 && currentYearTotalBonus != null) {
                        //2023年的分红率=2024年分红总额/总市值
                        stockInfo.setLastYearDividendRate(currentYearTotalBonus.divide(marketCap, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
                    }
                    if (marketCap.compareTo(BigDecimal.ZERO) != 0 && lastYearTotalBonus != null) {
                        //2022年的分红率=2023年分红总额/总市值
                        stockInfo.setLastYearDividendRate2(lastYearTotalBonus.divide(marketCap, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
                    }
                    if (marketCap.compareTo(BigDecimal.ZERO) != 0) {
                        //近三年分红率=2024年2023年2022年分红金额之和/3/总市值
                        stockInfo.setAvgDividendYield3yr(totalAll.divide(new BigDecimal("3"), 6, RoundingMode.HALF_EVEN).divide(marketCap, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
                    }
                }

                //收入预测增长率
                BigDecimal forecastGrossRevenueGrowthRate =
                        getForecastGrowthRateInfo(JSONObject.parseObject(JSON.toJSONString(stockInfo.getGrossRevenueMap())));
                //利润预测增长率
                BigDecimal forecastNetProfitGrowthRate =
                        getForecastGrowthRateInfo(JSONObject.parseObject(JSON.toJSONString(stockInfo.getNetProfitMap())));

                stockInfo.setForecastRevenueGrowthRate(forecastGrossRevenueGrowthRate);
                stockInfo.setForecastProfitGrowthRate(forecastNetProfitGrowthRate);

                BigDecimal peRatioTtm = stockInfo.getPeRatioTtm();
                //预测3年后pe
                BigDecimal forecastPe3Years = BigDecimal.ZERO;
                if (peRatioTtm != null) {
                    forecastPe3Years = calculatePEForecast(peRatioTtm, forecastGrossRevenueGrowthRate,
                            forecastNetProfitGrowthRate);
                }
                stockInfo.setForecastPe3Years(forecastPe3Years);


                JSONObject netProfitMapJson = JSONObject.parseObject(JSON.toJSONString(stockInfo.getNetProfitMap()));

                BigDecimal forecastNetProfitGrowthRateNew =
                        getForecastGrowthRateInfoNew(netProfitMapJson);


                JSONObject grossRevenueMapJson = JSONObject.parseObject(JSON.toJSONString(stockInfo.getGrossRevenueMap()));


                BigDecimal forecastGrossRevenueGrowthRateNew =
                        getForecastGrowthRateInfoNew(grossRevenueMapJson);
                stockInfo.setRevGrowthRateNew2(forecastGrossRevenueGrowthRateNew);
                try {
                    BigDecimal q2_2025 = netProfitMapJson.getJSONObject("2025").getBigDecimal("q2");
                    BigDecimal q2_2024 = netProfitMapJson.getJSONObject("2024").getBigDecimal("q2");
                    //25年2季度-24年2季度/24年2季度*100
                    stockInfo.setRevGrowthRateNew((q2_2025.subtract(q2_2024)).divide(q2_2024, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
                } catch (Exception e) {
                    log.error("计算出现异常！！异常股票:{}", tickerSymbol, e);
                }

                stockInfo.setProfitGrowthRateNew(forecastNetProfitGrowthRateNew);

                //计算5年内涨幅百分比
                BigDecimal maxPrice5y = stockInfo.getMaxPrice5y();
                if (maxPrice5y != null && maxPrice5y.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal annualPriceChange =
                            stockPrice.subtract(maxPrice5y).divide(maxPrice5y, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
                    stockInfo.setAnnualPriceChange(annualPriceChange);
                }

                //预测3年后pe
                BigDecimal peForecastNew = BigDecimal.ZERO;
                if (peRatioTtm != null) {
                    peForecastNew = calculatePEForecast(peRatioTtm, forecastGrossRevenueGrowthRateNew,
                            forecastNetProfitGrowthRateNew);
                }
                stockInfo.setPeForecastNew(peForecastNew);
            } catch (Exception e) {
                log.error("处理股票信息出现异常！！异常股票:{}", tickerSymbol, e);
            }
        }
        return all;
    }


    private static BigDecimal calculatePEForecast(BigDecimal peRatioTtm, BigDecimal incomeForecastGrowthRate,
                                                  BigDecimal profitForecastGrowthRate) {
        BigDecimal x = incomeForecastGrowthRate.max(profitForecastGrowthRate);
        // 将 x 除以 100，恢复到原始的增长率值
        x = x.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
        BigDecimal add = BigDecimal.ONE.add(x).pow(3);
        if (add.compareTo(BigDecimal.ZERO) != 0) {
            return peRatioTtm.divide(add, 10, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getForecastGrowthRateInfo(JSONObject data) {
        try {
            //当前年
            String currentYear = DateUtils.getCurrentYear();
            //去年
            String lastYear = DateUtils.getLastYear(1);
            //获取不包括当前年和去年的年份
            String[] pastYearsExcludingCurrentAndPrevious = DateUtils.getPastYearsExcludingCurrentAndPrevious(4);
            JSONObject dataB = data.getJSONObject(lastYear);
            JSONObject dataC = data.getJSONObject(currentYear);
            BigDecimal a = new BigDecimal(0);
            for (String yearsExcludingCurrentAndPrevious : pastYearsExcludingCurrentAndPrevious) {
                JSONObject jsonObject = data.getJSONObject(yearsExcludingCurrentAndPrevious);
                if (jsonObject != null) {
                    BigDecimal gr = jsonObject.getBigDecimal("gr");
                    if (gr == null) {
                        gr = BigDecimal.ZERO;
                    }
                    if (gr.compareTo(BigDecimal.ZERO) < 0) {
                        a = a.add(gr).multiply(new BigDecimal("1.5"));
                    } else {
                        a = a.add(gr);
                    }
                }
            }
            a = a.divide(new BigDecimal(4), 6, RoundingMode.HALF_EVEN);
            BigDecimal b = new BigDecimal(0);
            if (dataB != null) {
                b = dataB.getBigDecimal("gr");
                if (b == null) {
                    b = BigDecimal.ZERO;
                }
            }
            BigDecimal c = new BigDecimal(0);
            if (dataC != null) {
                c = dataC.getBigDecimal("gr");
                if (c == null) {
                    c = BigDecimal.ZERO;
                }
            }
            return calculateFinalGrowthRate(a, b, c);
        } catch (Exception e) {
            log.error("计算出现异常！", e);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal getForecastGrowthRateInfoNew(JSONObject data) {
        try {
            // 获取不包括当前年和去年的年份
            String[] pastYearsExcludingCurrentAndPrevious = DateUtils.getPastYearsIncludingCurrentNew(6);
            BigDecimal sum = new BigDecimal(0);
            int count = 0; // 计数器，用于计算有效数据的数量
            BigDecimal previousYearGr = null; // 记录上一年的增长率
            boolean isZeroSequence = false; // 标志变量，表示是否在连续零年的序列中
            for (String year : pastYearsExcludingCurrentAndPrevious) {
                JSONObject jsonObject = data.getJSONObject(year);
                if (jsonObject != null) {
                    BigDecimal gr = jsonObject.getBigDecimal("gr");
                    if (gr == null) {
                        gr = BigDecimal.ZERO;
                    }
                    if (gr.compareTo(BigDecimal.valueOf(60)) > 0) {
                        gr = BigDecimal.valueOf(60);
                    }
                    if (previousYearGr != null && previousYearGr.compareTo(BigDecimal.ZERO) == 0 && gr.compareTo(BigDecimal.ZERO) == 0) {
                        // 如果当前年和上一年的增长率都是0，则标记为零年序列
                        isZeroSequence = true;
                    } else {
                        // 如果之前有零年序列，则跳过这些零年
                        if (isZeroSequence) {
                            isZeroSequence = false;
                            // 跳过连续的零年
                            previousYearGr = gr;
                            continue;
                        }
                        // 处理正常情况
                        sum = sum.add(gr);
                        count++;
                    }
                    previousYearGr = gr;
                }
            }
            // 如果没有有效数据，则返回 0
            if (count == 0) {
                return BigDecimal.ZERO;
            }
//            if (count < 6) {
//                log.info("次数:{}", count);
//            }
            // 使用有效数据的数量来计算平均值
            return sum.divide(new BigDecimal(count), 6, RoundingMode.HALF_EVEN);
        } catch (Exception e) {
            log.error("计算出现异常！", e);
        }

        return BigDecimal.ZERO;
    }


    private static BigDecimal calculateFinalGrowthRate(BigDecimal a, BigDecimal b, BigDecimal c) {
        if (a.compareTo(b) > 0 && b.compareTo(c) > 0) {
            return c.subtract(b.subtract(c).divide(new BigDecimal(2), 6, RoundingMode.HALF_EVEN));
        } else if (a.compareTo(b) < 0 && b.compareTo(c) > 0) {
            return a.min(c);
        } else if (a.compareTo(b) > 0 && b.compareTo(c) < 0 || a.compareTo(b) < 0 && b.compareTo(c) < 0) {
            return b.add(c).divide(new BigDecimal(2), 6, RoundingMode.HALF_EVEN);
        } else {
            log.error("未知的值对比:{},{},{}", a, b, c);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 获取近四个季度的纯利润总和
     *
     * @param currentYearJson 当前年 季度数据
     * @param lastYearJson    去年 季度数据
     * @return 近四个季度的纯利润总和
     */
    private static BigDecimal getNetProfitTotal(JSONObject currentYearJson, JSONObject lastYearJson) {
        // 读取当前年和去年的季度数据
        BigDecimal firstQuarterRevenue = currentYearJson.getBigDecimal("q1");
        BigDecimal secondQuarterRevenue = currentYearJson.getBigDecimal("q2");
        BigDecimal thirdQuarterRevenue = currentYearJson.getBigDecimal("q3");
        BigDecimal fourthQuarterRevenue = currentYearJson.getBigDecimal("q4");

        BigDecimal lastYearFirstQuarterRevenue = lastYearJson.getBigDecimal("q1");
        BigDecimal lastYearSecondQuarterRevenue = lastYearJson.getBigDecimal("q2");
        BigDecimal lastYearThirdQuarterRevenue = lastYearJson.getBigDecimal("q3");
        BigDecimal lastYearFourthQuarterRevenue = lastYearJson.getBigDecimal("q4");

        // 计算总净利润
        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (firstQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            totalRevenue = totalRevenue.add(firstQuarterRevenue);
        } else {
            totalRevenue = totalRevenue.add(lastYearFirstQuarterRevenue);
        }
        if (secondQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            totalRevenue = totalRevenue.add(secondQuarterRevenue);
        } else {
            totalRevenue = totalRevenue.add(lastYearSecondQuarterRevenue);
        }
        if (thirdQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            totalRevenue = totalRevenue.add(thirdQuarterRevenue);
        } else {
            totalRevenue = totalRevenue.add(lastYearThirdQuarterRevenue);
        }
        if (fourthQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            totalRevenue = totalRevenue.add(fourthQuarterRevenue);
        } else {
            totalRevenue = totalRevenue.add(lastYearFourthQuarterRevenue);
        }

        return totalRevenue;
    }

    /**
     * 重新设置当前年的增长率
     *
     * @param currentYearJson 当前年季度数据
     * @param lastYearJson    去年季度数据
     * @return 新的增长率
     */

    private static BigDecimal getCurrentYearGrowthRate(JSONObject currentYearJson, JSONObject lastYearJson) {
        // 读取当前年和去年的季度数据
        BigDecimal firstQuarterRevenue = currentYearJson.getBigDecimal("q1");
        BigDecimal secondQuarterRevenue = currentYearJson.getBigDecimal("q2");
        BigDecimal thirdQuarterRevenue = currentYearJson.getBigDecimal("q3");
        BigDecimal fourthQuarterRevenue = currentYearJson.getBigDecimal("q4");

        BigDecimal lastYearFirstQuarterRevenue = lastYearJson.getBigDecimal("q1");
        BigDecimal lastYearSecondQuarterRevenue = lastYearJson.getBigDecimal("q2");
        BigDecimal lastYearThirdQuarterRevenue = lastYearJson.getBigDecimal("q3");
        BigDecimal lastYearFourthQuarterRevenue = lastYearJson.getBigDecimal("q4");
        // 计算当前年已更新季度的总净利润
        BigDecimal currentYearTotalRevenue = BigDecimal.ZERO;
        int filledQuarters = 0;
        if (firstQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            currentYearTotalRevenue = currentYearTotalRevenue.add(firstQuarterRevenue);
            filledQuarters++;
        }
        if (secondQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            currentYearTotalRevenue = currentYearTotalRevenue.add(secondQuarterRevenue);
            filledQuarters++;
        }
        if (thirdQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            currentYearTotalRevenue = currentYearTotalRevenue.add(thirdQuarterRevenue);
            filledQuarters++;
        }
        if (fourthQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            currentYearTotalRevenue = currentYearTotalRevenue.add(fourthQuarterRevenue);
            filledQuarters++;
        }
        // 跳过已满4季度的当前年
        if (filledQuarters == 4) {
            log.info("当前年已满4季度，跳过计算。");
            return null;
        }
        // 计算去年对应季度的总净利润
        BigDecimal lastYearTotalRevenue = BigDecimal.ZERO;
        if (firstQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            lastYearTotalRevenue = lastYearTotalRevenue.add(lastYearFirstQuarterRevenue);
        }
        if (secondQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            lastYearTotalRevenue = lastYearTotalRevenue.add(lastYearSecondQuarterRevenue);
        }
        if (thirdQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            lastYearTotalRevenue = lastYearTotalRevenue.add(lastYearThirdQuarterRevenue);
        }
        if (fourthQuarterRevenue.compareTo(BigDecimal.ZERO) != 0) {
            lastYearTotalRevenue = lastYearTotalRevenue.add(lastYearFourthQuarterRevenue);
        }
        // 计算同比增长率
        BigDecimal yearOnYearGrowthRate;
        if (lastYearTotalRevenue.compareTo(BigDecimal.ZERO) != 0) {
            yearOnYearGrowthRate = (currentYearTotalRevenue.subtract(lastYearTotalRevenue)).divide(lastYearTotalRevenue, 5,
                    RoundingMode.HALF_EVEN).multiply(new BigDecimal("100"));

            return yearOnYearGrowthRate;
        }
        return null;
    }

    /**
     * 初始化缓存股票信息
     */
    public void cacheHistoricalStocksInfo() {
        List<HistoricalStocksInfo> singleAll = historicalStocksInfoMapper.findAll();
        for (HistoricalStocksInfo historicalStocksInfo : singleAll) {
            String grossRevenue = historicalStocksInfo.getGrossRevenue();
            String tickerSymbol = historicalStocksInfo.getTickerSymbol();
            if (StringUtils.isNotEmpty(grossRevenue)) {
                JSONObject grossRevenueJson = JSONObject.parseObject(grossRevenue);
                Map<String, Map<String, Object>> grossRevenueMap = jsonInfo(grossRevenueJson);
                GROSS_REVENUE_CACHE_MAP.put(tickerSymbol, grossRevenueMap);
            }
            String netProfit = historicalStocksInfo.getNetProfit();
            if (StringUtils.isNotEmpty(netProfit)) {
                JSONObject netProfitJson = JSONObject.parseObject(netProfit);
                Map<String, Map<String, Object>> netProfitMap = jsonInfo(netProfitJson);
                NET_PROFIT_CACHE_MAP.put(tickerSymbol, netProfitMap);
            }
            //保存每年的分红量
            String totalBonus = historicalStocksInfo.getTotalBonus();
            if (StringUtils.isNotEmpty(totalBonus)) {
                JSONObject totalBonusJson = JSONObject.parseObject(totalBonus);
                TOTAL_BONUS_CACHE_MAP.put(tickerSymbol, totalBonusJson);
            }
            String announceDate = historicalStocksInfo.getAnnounceDate();
            if (StringUtils.isNotEmpty(announceDate)) {
                JSONObject announceDateJson = JSONObject.parseObject(announceDate);
                ANNOUNCE_DATE_CACHE_MAP.put(tickerSymbol, announceDateJson);
            }
            String dailyDividends = historicalStocksInfo.getDailyDividends();
            if (StringUtils.isNotEmpty(dailyDividends)) {
                JSONObject dailyDividendsJson = JSONObject.parseObject(dailyDividends);
                DAILY_DIVIDENDS_CACHE_MAP.put(tickerSymbol, dailyDividendsJson);
            }
            //净资产
            String bvpsData = historicalStocksInfo.getBvpsData();
            if (StringUtils.isNotEmpty(bvpsData)) {
                JSONObject bvpsDataJson = JSONObject.parseObject(bvpsData);
                BVPS_DATA_CACHE_MAP.put(tickerSymbol, bvpsDataJson);
            }
            String dividendsData = historicalStocksInfo.getDividendsData();
            if (StringUtils.isNotEmpty(dividendsData)) {
                JSONObject dividendsDataJson = JSONObject.parseObject(dividendsData);
                DIVIDENDS_DATA_CACHE_MAP.put(tickerSymbol, dividendsDataJson);
            }
        }
        getStockInfos();
    }

    public void getStockInfos() {
        log.info("开始获取股票基础信息");
        //获取股票全部基础信息
        STOCK_INFO_LIST_GLOBAL_CACHE.clear();
        try {
            List<StockInfo> all = stockInfoMapper.findAll();
            List<StockInfo> stockInfoGlobalCache = getStockInfoGlobalCache(all);
            //查询缓存所有股票信息
            STOCK_INFO_LIST_GLOBAL_CACHE.addAll(stockInfoGlobalCache);
            log.info("股票基础信息长度:{}", stockInfoGlobalCache.size());

            if (stockInfoGlobalCache.size() > 0) {
                List<StockInfo> stockInfos = new ArrayList<>();
                for (StockInfo stockInfo : stockInfoGlobalCache) {
                    stockInfos.add(stockInfo.clone()); // 使用深拷贝
                }
                for (StockInfo stockInfo : stockInfos) {
                    String tickerSymbol = stockInfo.getTickerSymbol();
                    String datas = ALL_TICKER_SYMBOL_MAP.get(tickerSymbol);
                    stockInfo.setTickerSymbol(datas.split("_")[0]);
                }
                stockInfoMapper.batchInsertNew(stockInfos);
            }
        } catch (Exception e) {
            log.error("获取股票信息出现异常！！", e);
        }
    }

    private static Map<String, Map<String, Object>> jsonInfo(JSONObject data) {
        Map<String, Map<String, Object>> categorizedData = new TreeMap<>();

        for (Map.Entry<String, Object> stringObjectEntry : data.entrySet()) {
            String date = stringObjectEntry.getKey();
            Long value = ((Number) stringObjectEntry.getValue()).longValue();
            String year = date.substring(0, 4);
            String quarter = date.substring(4);
            // 初始化年份数据
            categorizedData.computeIfAbsent(year, k -> {
                Map<String, Object> yearData = new HashMap<>();
                yearData.put("q1", "0");  //第1季度总金额
                yearData.put("q2", "0");  //第2季度总金额
                yearData.put("q3", "0");  //第3季度总金额
                yearData.put("q4", "0");  //第4季度总金额
                yearData.put("tAmt", "0");  //全年累计总金额
                return yearData;
            });
            // 根据季度更新数据
            Map<String, Object> yearData = categorizedData.get(year);
            switch (quarter) {
                case "0331":
                    yearData.put("q1", String.valueOf(value));
                    break;
                case "0630":
                    yearData.put("q2", String.valueOf(value));
                    break;
                case "0930":
                    yearData.put("q3", String.valueOf(value));
                    break;
                case "1231":
                    yearData.put("q4", String.valueOf(value));
                    break;
            }
        }
        // 计算真实总值
        for (Map.Entry<String, Map<String, Object>> entry : categorizedData.entrySet()) {

            Map<String, Object> yearData = entry.getValue();
            long q1 = parseLong(yearData.get("q1"));
            long q2 = parseLong(yearData.get("q2"));
            long q3 = parseLong(yearData.get("q3"));
            long q4 = parseLong(yearData.get("q4"));

            long q2Revenue = (q2 != 0) ? q2 - q1 : 0;
            long q3Revenue = (q3 != 0) ? q3 - (q1 + q2Revenue) : 0;
            long q4Revenue = (q4 != 0) ? q4 - (q1 + q2Revenue + q3Revenue) : 0;
            long q3R = q2 + q3Revenue;
            long tAmt = q3R + q4Revenue;

            yearData.put("q1", q1);
            yearData.put("q2", q2Revenue);
            yearData.put("q3", q3Revenue);
            yearData.put("q4", q4Revenue);
            yearData.put("midR", q2);
            yearData.put("q3R", q3R);
            yearData.put("tAmt", tAmt);
        }

        // 计算增长率
        // 计算增长率和同比率
        Map<String, Map<String, Object>> previousYearData = new HashMap<>();
        Integer previousYear = null;
        Long previousYearTotal = null;
        Long previousYearMidR = null; // 记录上一个年份的中报总金额
        Long previousYearQ3R = null; // 记录上一个年份的累计三季报总金额
        for (Map.Entry<String, Map<String, Object>> entry : categorizedData.entrySet()) {
            String year = entry.getKey();
            Map<String, Object> yearData = entry.getValue();
            long currentTotal = Long.parseLong(yearData.get("tAmt").toString());
            long currentMidR = Long.parseLong(yearData.get("midR").toString());
            long currentQ3R = Long.parseLong(yearData.get("q3R").toString());
            if (previousYear != null) {
                // 计算增长率
                double growthRate;
                if (previousYearTotal == 0) {
                    growthRate = 0; // 或其他处理方式
                } else {
                    //2022的增长率=（2022年-2021年）/2021年
                    growthRate = ((double) currentTotal - previousYearTotal) / Math.abs(previousYearTotal);
                }
                yearData.put("gr", growthRate == 0 ? "0" : String.format("%.2f", growthRate * 100));
                // 计算每个季度对应的同比率
                for (int i = 1; i <= 4; i++) {
                    String currentQuarterKey = getQuarterKey(i);
                    String previousQuarterKey = getQuarterKey(i);
                    if (previousYearData.containsKey(previousYear.toString())) {
                        Map<String, Object> prevYearData = previousYearData.get(previousYear.toString());
                        long currentQuarterRevenue = Long.parseLong(yearData.get(currentQuarterKey).toString());
                        long previousQuarterRevenue = Long.parseLong(prevYearData.get(previousQuarterKey).toString());
                        double quarterGrowthRate;
                        if (previousQuarterRevenue == 0) {
                            quarterGrowthRate = 0; // 或其他处理方式
                        } else {
                            String currentYear = DateUtils.getCurrentYear();

                            if (year.equals(currentYear) && currentQuarterRevenue == 0) {
                                quarterGrowthRate = 0; // 或其他处理方式
                            } else {
                                //2022 1季度的增长率=（2022年 1季度-2021年 1季度）/2021年 1季度
                                quarterGrowthRate =
                                        ((double) currentQuarterRevenue - previousQuarterRevenue) / Math.abs(previousQuarterRevenue);
                            }
                        }
                        yearData.put(currentQuarterKey + "Gr", quarterGrowthRate == 0 ? "0" : String.format("%.2f",
                                quarterGrowthRate * 100));
                    } else {
                        yearData.put(currentQuarterKey + "Gr", "0"); // 前一年数据不存在
                    }
                }
                // 计算中报总金额同比率
                double midRGrowthRate;
                if (previousYearMidR == 0) {
                    midRGrowthRate = 0; // 或其他处理方式
                } else {
                    midRGrowthRate = ((double) currentMidR - previousYearMidR) / Math.abs(previousYearMidR);
                }
                yearData.put("midRGr", midRGrowthRate == 0 ? "0" : String.format("%.2f", midRGrowthRate * 100));
                // 计算累计三季报总金额同比率
                double q3RGrowthRate;
                if (previousYearQ3R == 0) {
                    q3RGrowthRate = 0; // 或其他处理方式
                } else {
                    q3RGrowthRate = ((double) currentQ3R - previousYearQ3R) / Math.abs(previousYearQ3R);
                }
                yearData.put("q3RGr", q3RGrowthRate == 0 ? "0" : String.format("%.2f", q3RGrowthRate * 100));
            } else {
                // 对于第一个年份，默认增长率和同比率为0%
                yearData.put("gr", "0");
                yearData.put("midRGr", "0");
                yearData.put("q3RGr", "0");
                for (int i = 1; i <= 4; i++) {
                    yearData.put(getQuarterKey(i) + "gr", "0");
                }
            }
            // 保存当前年份数据用于下一次计算
            previousYearData.put(year, new HashMap<>(yearData));
            previousYear = Integer.parseInt(year);
            previousYearTotal = currentTotal;
            previousYearMidR = currentMidR; // 更新前一年中报总金额
            previousYearQ3R = currentQ3R;   // 更新前一年累计三季报总金额
        }
        return categorizedData;
    }


    private static long parseLong(Object value) {
        return (value != null) ? Long.parseLong(value.toString()) : 0;
    }

    private static String getQuarterKey(int quarter) {
        switch (quarter) {
            case 1:
                return "q1";
            case 2:
                return "q2";
            case 3:
                return "q3";
            case 4:
                return "q4";
            default:
                throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
    }


    private JSONObject getKDataInfo(String tickerSymbol, String type, String tickerSymbol2, String tickerName) throws Exception {
        JSONObject data = new JSONObject();
        JSONObject body = null;
        if ("day".equalsIgnoreCase(type)) {
//            body = getJsonObject("daily", tickerSymbol);
//            String path = "D:\\" + tickerSymbol + "_day_K.zip";
            String path = "/home/tomcat/aShare_data/Agu_day_K/" + tickerSymbol + "_day_K.zip";
            body = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(path));

        } else if ("week".equalsIgnoreCase(type)) {
            String path = "/home/tomcat/aShare_data/Agu_week_K/" + tickerSymbol + "_week_K.zip";
            body = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(path));
//            body = getJsonObject("weekly", tickerSymbol);
        } else if ("month".equalsIgnoreCase(type)) {
            String path = "/home/tomcat/aShare_data/Agu_month_K/" + tickerSymbol + "_month_K.zip";
            body = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(path));
//            body = getJsonObject("monthly", tickerSymbol);
        }
        if (body != null && body.getInteger("code").equals(0)) {
            JSONArray dataArray = body.getJSONObject("data").getJSONArray("items");
            List<DailyVo> dailyVoList = new ArrayList<>();
            //获取K线数据
            JSONArray jsonArrayAll = getkXian(type, dataArray, dailyVoList);
            //新股票代码
            String tickerSymbolNew = tickerSymbol.split("\\.")[0];
            //对k线数据进行排序降序
            dailyVoList.sort(Comparator.comparing(DailyVo::getTradeDate).reversed());
            //2000-2024年净利润 key 2024  key q1-q4 val 对应季度的金额总量
            Map<String, Map<String, Object>> netProfitCacheMap = NET_PROFIT_CACHE_MAP.get(tickerSymbolNew);
            //2000-2024年营收入 key 2024  key q1-q4 val 对应季度的金额总量
            Map<String, Map<String, Object>> grossRevenueMap = GROSS_REVENUE_CACHE_MAP.get(tickerSymbolNew);
            //2000-2024年 实际披露日期  key 20170331 每年季度时间 val 对应该季度的公布时间
            JSONObject announceDateJson = ANNOUNCE_DATE_CACHE_MAP.get(tickerSymbolNew);
            //交易日期 key 20240904  股本数 val 2985000000.0
            Map<String, Map<String, Object>> dataNew = getTotalShareMap(tickerSymbol);
            //总股本
            Map<String, Object> totalShareMap = dataNew.get("totalShare");
            if (totalShareMap.size() == 0) {
                throw new RuntimeException("调用接口出现异常！！");
            }
            //每季度分红的股息
            JSONObject dailyDividendsJson = DAILY_DIVIDENDS_CACHE_MAP.get(tickerSymbolNew);
            //2000-2024年 净资产 key 2024  key q1-q4 val 对应季度的金额总量
            JSONObject bvpsDataJson = BVPS_DATA_CACHE_MAP.get(tickerSymbolNew);
            //派息日对应的 派息日期的派息详情
            JSONObject dividendsDataJson = DIVIDENDS_DATA_CACHE_MAP.get(tickerSymbolNew);

            Map<String, DailyVo> groupedByTradeDate = dailyVoList.stream().collect(Collectors.toMap(DailyVo::getTradeDate,
                    dailyVo -> dailyVo, (existing, replacement) -> existing));
            // 提取排序后的键
            List<String> dailyDividendsKeys = new ArrayList<>(dividendsDataJson.keySet());
            Collections.sort(dailyDividendsKeys);

            List<DailyVo> dailyVos = fictionalKLine(dividendsDataJson, groupedByTradeDate, dailyDividendsKeys);
            if (dailyVos.size() > 0) {
                dailyVoList.addAll(dailyVos);
                dailyVoList.sort(Comparator.comparing(DailyVo::getTradeDate).reversed());
            }
            Map<String, JSONArray> peRatioTtmInfoAll = getPeRatioTtmInfoAll(dailyVoList, netProfitCacheMap, grossRevenueMap,
                    announceDateJson, totalShareMap, bvpsDataJson, tickerName, dailyDividendsJson, dividendsDataJson);
            JSONArray dataAll = peRatioTtmInfoAll.get("dataAll");
            //复权前
            JSONArray preAdjustedPrice = peRatioTtmInfoAll.get("dataAll2");
            //复权后
            JSONArray postAdjustedPrice = peRatioTtmInfoAll.get("dataAll3");

            data.put("list2", sortJSONArrayByDate(dataAll));
            data.put("list", sortJSONArrayByDate(jsonArrayAll));
            data.put("preAdjustedPrice", sortJSONArrayByDate(preAdjustedPrice));
            data.put("postAdjustedPrice", sortJSONArrayByDate(postAdjustedPrice));
            data.put("tickerSymbol", tickerSymbol2);
            data.put("tickerName", tickerName);
            return data;
        } else {
            throw new RuntimeException("调用接口超时！！");
        }
    }

    private static JSONArray getkXian(String type, JSONArray dataArray, List<DailyVo> dailyVoList) {
        JSONArray jsonArrayAll = new JSONArray();
        Set<String> tradeDateSet = new HashSet<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JSONArray jsonArray = dataArray.getJSONArray(i);
            JSONArray jsonArrayNew = new JSONArray();
            //交易日期
            String tradeDate = jsonArray.getString(1);
            if (tradeDateSet.contains(tradeDate)) {
                continue;
            }
            tradeDateSet.add(tradeDate);
            BigDecimal open = BigDecimal.ZERO;
            BigDecimal high = BigDecimal.ZERO;
            BigDecimal low = BigDecimal.ZERO;
            BigDecimal close = BigDecimal.ZERO;
            if ("day".equalsIgnoreCase(type)) {
//                    //开盘价
                open = jsonArray.getBigDecimal(2);
                //最高价
                high = jsonArray.getBigDecimal(3);
                //	最低价
                low = jsonArray.getBigDecimal(4);
                //收盘价格
                close = jsonArray.getBigDecimal(5);
            } else if ("week".equalsIgnoreCase(type) || "month".equalsIgnoreCase(type)) {
                //收盘价格
                close = jsonArray.getBigDecimal(2);
                //开盘价
                open = jsonArray.getBigDecimal(3);
                //最高价
                high = jsonArray.getBigDecimal(4);
                //	最低价
                low = jsonArray.getBigDecimal(5);
            }
            //昨收价【除权价，前复权】
            BigDecimal preClose = jsonArray.getBigDecimal(6);
            //	涨跌额
            BigDecimal change = jsonArray.getBigDecimal(7);
            //涨跌幅 【基于除权后的昨收计算的涨跌幅：（今收-除权昨收）/除权昨收 】
            BigDecimal pctChg = jsonArray.getBigDecimal(8);
            //成交量 （手）
            BigDecimal vol = jsonArray.getBigDecimal(9);
            //成交额 （千元）
            BigDecimal amount = jsonArray.getBigDecimal(10);
            jsonArrayNew.add(DateUtils.formatDateStringWithNextDay(tradeDate));
            jsonArrayNew.add(open);
            jsonArrayNew.add(high);
            jsonArrayNew.add(low);
            jsonArrayNew.add(close);
            jsonArrayNew.add(preClose);
            jsonArrayNew.add(change);
            jsonArrayNew.add(pctChg);
            jsonArrayNew.add(vol);
            jsonArrayNew.add(amount);
            jsonArrayAll.add(jsonArrayNew);
            dailyVoList.add(new DailyVo(open, high, low, close, tradeDate));
        }
        return jsonArrayAll;
    }

    /**
     * 对 JSONArray 按日期进行降序排序
     *
     * @param jsonArray 原始 JSONArray
     * @return 排序后的 JSONArray
     */
    private static JSONArray sortJSONArrayByDate(JSONArray jsonArray) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // 将 JSONArray 转换为 List<JSONArray>
        List<JSONArray> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.getJSONArray(i));
        }
        // 使用 Java 8 流进行排序
        List<JSONArray> sortedList = list.stream().sorted((o1, o2) -> {
            try {
                Date date1 = dateFormat.parse(o1.getString(0));
                Date date2 = dateFormat.parse(o2.getString(0));
                return date1.compareTo(date2); // 升序
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        // 将排序后的 List 转回 JSONArray
        JSONArray sortedArray = new JSONArray();
        sortedArray.addAll(sortedList);
        return sortedArray;
    }


    /**
     * 获取所有交易日期的市盈率（PE Ratio）和市净率（PB Ratio）信息
     *
     * @param dailyVoList       每日交易数据列表
     * @param netProfitCacheMap 净利润缓存映射
     * @param grossRevenueMap   总收入缓存映射
     * @param announceDateJson  财报公布日期的JSON对象
     * @param totalShareMap     总股份数映射
     *                          //     * @param dividendDateJson   分红日期的JSON对象
     *                          //     * @param dailyDividendsJson 每日分红的JSON对象
     *                          //     * @param netAssetsJson      净资产的JSON对象
     * @return 包含市盈率和市净率信息的JSON数组
     * @throws Exception 如果计算过程中发生异常
     */
    private Map<String, JSONArray> getPeRatioTtmInfoAll(List<DailyVo> dailyVoList,
                                                        Map<String, Map<String, Object>> netProfitCacheMap, Map<String,
                    Map<String, Object>> grossRevenueMap, JSONObject announceDateJson, Map<String, Object> totalShareMap,
                                                        JSONObject bvpsDataJson, String tickerName,
                                                        JSONObject dailyDividendsJson, JSONObject dividendsDataJson) throws Exception {

        // 用于保存最终结果的JSON数组
        Map<String, JSONArray> dataAllMap = new HashMap<>();
//        // 获取每个交易日期的分红金额映射
        Map<String, JSONArray> dividendAmountMap = getDividendAmountMap(dailyVoList, totalShareMap, dailyDividendsJson);
        Map<String, BigDecimal> dividendsDataMap = getDividendsDataJson(dailyVoList, dividendsDataJson);
        Long totalShareOld = null; // 记录上一个有效的总股份数
        JSONArray dataAll = new JSONArray();
        JSONArray dataAll2 = new JSONArray();
        JSONArray dataAll3 = new JSONArray();
        for (DailyVo dailyVo : dailyVoList) {
            try {
                // 获取当前交易日期和收盘价格
                String tradeDate = dailyVo.getTradeDate();
                BigDecimal close = dailyVo.getClose();
                // 获取当前交易日期的总股份数
                Long totalShare = getTotalShare(totalShareMap, tradeDate);
                if (totalShare != null) {
                    totalShareOld = totalShare;
                } else {
                    totalShare = totalShareOld;
                }
                // 计算总市值
                BigDecimal marketCap = close.multiply(new BigDecimal(totalShare));
//                // 提取年份和月份信息
                String year = tradeDate.substring(0, 4);
                String quarter = tradeDate.substring(4);
                String dateNow;
                if (Long.parseLong(quarter) > 331L) {
                    // 获取与当前日期最接近的季度结束日期
                    String closestQuarterEndDate = DateUtils.getClosestQuarterEndDate(quarter);
                    dateNow = year + closestQuarterEndDate;
                } else {
                    dateNow = (Long.parseLong(year) - 1) + "1231";
                }

                // 找到当前交易日期之前已披露的最近季度报告
                String latestDisclosedQuarter = findLatestDisclosedQuarter(tradeDate, announceDateJson);
                if (StringUtils.isEmpty(latestDisclosedQuarter)) {
                    latestDisclosedQuarter = dateNow;
                    log.error("tickerName:{},TradeDate为空:{},latestDisclosedQuarter:{}", tickerName, dailyVo.getTradeDate(), latestDisclosedQuarter);
                }
                // 获取实际公布日期
                Long announceDate = announceDateJson.getLong(latestDisclosedQuarter);
                // 处理不同情况下的市盈率和市净率计算
                if (announceDate != null) {
                    if (Long.parseLong(tradeDate) >= announceDate) {
                        // 使用实际公布数据进行计算
                        BigDecimal dividend = calculateDividend(tradeDate, dividendAmountMap, marketCap);

                        BigDecimal pbRatio = calculatePbRatio(dateNow, bvpsDataJson, totalShare, marketCap);

                        dataAll.add(peRatioTtmInfo(netProfitCacheMap, grossRevenueMap, tradeDate, close, totalShare, marketCap,
                                latestDisclosedQuarter, dividend, pbRatio));
                    } else {
                        // 交易日期早于公布日期，使用历史数据进行计算
                        BigDecimal dividend = calculateDividend(tradeDate, dividendAmountMap, marketCap);
                        BigDecimal pbRatio = calculatePbRatio(DateUtils.getPreviousQuarter(dateNow), bvpsDataJson, totalShare,
                                marketCap);
                        dataAll.add(peRatioTtmInfo(netProfitCacheMap, grossRevenueMap, tradeDate, close, totalShare, marketCap,
                                DateUtils.getPreviousQuarterEnd(latestDisclosedQuarter), dividend, pbRatio));
                    }
                } else {
                    // 没有公布日期的情况下，使用历史数据进行计算
                    BigDecimal dividend = calculateDividend(tradeDate, dividendAmountMap, marketCap);
                    BigDecimal pbRatio = calculatePbRatio(DateUtils.getPreviousQuarter(dateNow), bvpsDataJson, totalShare,
                            marketCap);
                    dataAll.add(peRatioTtmInfo(netProfitCacheMap, grossRevenueMap, tradeDate, close, totalShare, marketCap,
                            DateUtils.getPreviousQuarterEnd(latestDisclosedQuarter), dividend, pbRatio));
                }

                Map<String, JSONArray> newKXian = getNewKXian(dailyVo, dividendsDataMap, tradeDate);
                //前复权
                JSONArray dataQfq = newKXian.get("dataQfq");
                //后复权
                JSONArray dataHfq = newKXian.get("dataHfq");

                dataAll2.add(dataQfq);
                dataAll3.add(dataHfq);
            } catch (Exception e) {
                log.error("遍历股票出现异常！股票代码:{}", tickerName, e);
            }
        }
        dataAllMap.put("dataAll", dataAll);
        dataAllMap.put("dataAll2", dataAll2);
        dataAllMap.put("dataAll3", dataAll3);

        return dataAllMap;
    }

    /**
     * 找到指定日期之前已披露的最近季度报告
     *
     * @param tradeDate        当前交易日期 (格式: yyyyMMdd)
     * @param announceDateJson 季度结束日期到实际披露日期的映射 (FastJSON的JSONObject类型)
     * @return 最近已披露季度的结束日期 (格式: yyyyMMdd)
     */
    private String findLatestDisclosedQuarter(String tradeDate, JSONObject announceDateJson) {
        long currentDate = Long.parseLong(tradeDate);
        //最新的季度
        String latestQuarter = null;
        //最新的披露日期
        long latestAnnounceDate = 0;
        // FastJSON遍历JSONObject的正确方式
        for (Map.Entry<String, Object> entry : announceDateJson.entrySet()) {
            String quarterEndDate = entry.getKey();
            Object value = entry.getValue();
            try {
                // 将值转换为Long类型
                long announceDate = Long.parseLong(value.toString());
                // 检查该季度报告是否在当前交易日期之前已披露
                if (announceDate <= currentDate) {
                    // 找到最近披露的季度
                    if (announceDate > latestAnnounceDate) {
                        latestAnnounceDate = announceDate;
                        latestQuarter = quarterEndDate;
                    } else if (latestQuarter != null && announceDate == latestAnnounceDate) {
                        //如果存在两个季度的披露日期相同 去距离当前日期最近的季度日期
                        long tradeDateLongThis = Long.parseLong(tradeDate);
                        long quarterEndDateLongThis = Long.parseLong(quarterEndDate);
                        long latestQuarterLongThis = Long.parseLong(latestQuarter);
                        long val = tradeDateLongThis - quarterEndDateLongThis;
                        long val2 = tradeDateLongThis - latestQuarterLongThis;
                        if (val < val2) {
                            latestQuarter = quarterEndDate;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // 处理数值转换异常
                log.error("无效的日期格式:{}", quarterEndDate);
                e.printStackTrace();
            }
        }

        return latestQuarter;
    }

    private static List<DailyVo> fictionalKLine(JSONObject dividendsDataJson, Map<String, DailyVo> groupedByTradeDate,
                                                List<String> dailyDividendsKeys) {

        List<DailyVo> dailyVos = new ArrayList<>();
        // 处理每一个键
        for (String dailyDividendsKey : dailyDividendsKeys) {
            DailyVo dailyVo = groupedByTradeDate.get(dailyDividendsKey);
            if (dailyVo == null) {
                // 如果当前日期的 K 线数据为空，找到前一个交易日期
                List<String> sortedTradeDates = new ArrayList<>(groupedByTradeDate.keySet());
                sortedTradeDates.sort(Collections.reverseOrder());
                //获取到上个交易日的日期并且找到对应的k线数据下面用了进行虚构曲线
                String previousTradeDate = findPreviousTradeDate(sortedTradeDates, dailyDividendsKey);
                if (previousTradeDate != null) {
                    dailyVo = groupedByTradeDate.get(previousTradeDate);
                    BigDecimal x = dailyVo.getClose();

                    JSONArray dailyDividendsArrays = dividendsDataJson.getJSONArray(dailyDividendsKey);
                    BigDecimal totalA = BigDecimal.ZERO;
                    BigDecimal totalB = BigDecimal.ZERO;
                    BigDecimal totalM = BigDecimal.ZERO;
                    for (int i = 0; i < dailyDividendsArrays.size(); i++) {
                        JSONObject jsonObject = dailyDividendsArrays.getJSONObject(i);
                        BigDecimal a = jsonObject.getBigDecimal("a");
                        BigDecimal b = jsonObject.getBigDecimal("b");
                        BigDecimal m = jsonObject.getBigDecimal("m");
                        totalA = totalA.add(a);
                        totalB = totalB.add(b);
                        totalM = totalM.add(m);
                    }
                    // 计算调整后的价格 q
                    BigDecimal numerator = x.subtract(totalM.divide(BigDecimal.TEN, MathContext.DECIMAL128));
                    BigDecimal denominator = BigDecimal.TEN.add(totalA).add(totalB);
                    BigDecimal q = numerator.divide(denominator, MathContext.DECIMAL128).multiply(new BigDecimal(10));
                    dailyVo = new DailyVo();
                    // 设置调整后的价格
                    dailyVo.setClose(q);
                    dailyVo.setOpen(q);
                    dailyVo.setHigh(q);
                    dailyVo.setLow(q);
                    dailyVo.setTradeDate(dailyDividendsKey);
                    dailyVos.add(dailyVo);
                    // 存储或更新调整后的数据
                    groupedByTradeDate.put(dailyDividendsKey, dailyVo);
                }
            }
        }
        return dailyVos;
    }


    // 查找指定日期的前一个交易日期
    private static String findPreviousTradeDate(List<String> sortedTradeDates, String currentTradeDate) {
        String previousTradeDate = null;
        for (String tradeDate : sortedTradeDates) {
            // 比较当前日期和列表中的日期，找出小于当前日期的最近日期
            if (tradeDate.compareTo(currentTradeDate) < 0) {
                previousTradeDate = tradeDate;
                break;
            }
        }
        return previousTradeDate;
    }


    /**
     * 获取当天复权前和复权后价格
     *
     * @param dailyVo          当天k线
     * @param dividendsDataMap 派息日期对应的复权因子
     * @param tradeDate        交易日期
     * @return 复权后和复权前的k线
     */
    private Map<String, JSONArray> getNewKXian(DailyVo dailyVo, Map<String, BigDecimal> dividendsDataMap, String tradeDate) {
        //前复权
        JSONArray dataQfq = new JSONArray();
        //后复权
        JSONArray dataHfq = new JSONArray();
        Map<String, JSONArray> jsonArrayMap = new HashMap<>();

        // 当前价格数据
        BigDecimal close = dailyVo.getClose();
        BigDecimal open = dailyVo.getOpen();
        BigDecimal high = dailyVo.getHigh();
        BigDecimal low = dailyVo.getLow();
        // 将 dividendsDataMap 转换为 TreeMap，以便按日期顺序处理
        TreeMap<String, BigDecimal> sortedDividends = new TreeMap<>(dividendsDataMap);
        // 计算后复权因子
        BigDecimal postAdjustmentFactorClose = BigDecimal.ONE;
        BigDecimal postAdjustmentFactorOpen = BigDecimal.ONE;
        BigDecimal postAdjustmentFactorHigh = BigDecimal.ONE;
        BigDecimal postAdjustmentFactorLow = BigDecimal.ONE;

        // 计算前复权因子
        BigDecimal preAdjustmentFactorClose = BigDecimal.ONE;
        BigDecimal preAdjustmentFactorOpen = BigDecimal.ONE;
        BigDecimal preAdjustmentFactorHigh = BigDecimal.ONE;
        BigDecimal preAdjustmentFactorLow = BigDecimal.ONE;

        // 遍历分红数据
        for (Map.Entry<String, BigDecimal> entry : sortedDividends.entrySet()) {
            String date = entry.getKey(); // 分红日期
            BigDecimal factor = entry.getValue(); // 分红因子
            // 计算后复权因子（日期小于等于目标日期）
            if (date.compareTo(tradeDate) <= 0) {
                postAdjustmentFactorClose = postAdjustmentFactorClose.multiply(factor).setScale(4, RoundingMode.HALF_UP);
                postAdjustmentFactorOpen = postAdjustmentFactorOpen.multiply(factor).setScale(4, RoundingMode.HALF_UP);
                postAdjustmentFactorHigh = postAdjustmentFactorHigh.multiply(factor).setScale(4, RoundingMode.HALF_UP);
                postAdjustmentFactorLow = postAdjustmentFactorLow.multiply(factor).setScale(4, RoundingMode.HALF_UP);
            }
            // 计算前复权因子（日期大于等于目标日期）
            if (date.compareTo(tradeDate) > 0) {
                preAdjustmentFactorClose = preAdjustmentFactorClose.multiply(BigDecimal.ONE.divide(factor, 6,
                        RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);
                preAdjustmentFactorOpen = preAdjustmentFactorOpen.multiply(BigDecimal.ONE.divide(factor, 6,
                        RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);
                preAdjustmentFactorHigh = preAdjustmentFactorHigh.multiply(BigDecimal.ONE.divide(factor, 6,
                        RoundingMode.HALF_UP)).setScale(4, RoundingMode.HALF_UP);
                preAdjustmentFactorLow =
                        preAdjustmentFactorLow.multiply(BigDecimal.ONE.divide(factor, 6, RoundingMode.HALF_UP)).setScale(4,
                                RoundingMode.HALF_UP);
            }
        }

        // 计算后复权价格
        BigDecimal postAdjustedOpen = open.multiply(postAdjustmentFactorOpen).setScale(4, RoundingMode.HALF_UP);
        BigDecimal postAdjustedHigh = high.multiply(postAdjustmentFactorHigh).setScale(4, RoundingMode.HALF_UP);
        BigDecimal postAdjustedLow = low.multiply(postAdjustmentFactorLow).setScale(4, RoundingMode.HALF_UP);
        BigDecimal postAdjustedClose = close.multiply(postAdjustmentFactorClose).setScale(4, RoundingMode.HALF_UP);

        // 计算前复权价格
        BigDecimal preAdjustedOpen = open.multiply(preAdjustmentFactorOpen).setScale(4, RoundingMode.HALF_UP);
        BigDecimal preAdjustedHigh = high.multiply(preAdjustmentFactorHigh).setScale(4, RoundingMode.HALF_UP);
        BigDecimal preAdjustedLow = low.multiply(preAdjustmentFactorLow).setScale(4, RoundingMode.HALF_UP);
        BigDecimal preAdjustedClose = close.multiply(preAdjustmentFactorClose).setScale(4, RoundingMode.HALF_UP);

        dataQfq.add(DateUtils.formatDateStringWithNextDay(tradeDate));
        dataQfq.add(preAdjustedOpen);
        dataQfq.add(preAdjustedHigh);
        dataQfq.add(preAdjustedLow);
        dataQfq.add(preAdjustedClose);

        dataHfq.add(DateUtils.formatDateStringWithNextDay(tradeDate));
        dataHfq.add(postAdjustedOpen);
        dataHfq.add(postAdjustedHigh);
        dataHfq.add(postAdjustedLow);
        dataHfq.add(postAdjustedClose);

        jsonArrayMap.put("dataQfq", dataQfq);
        jsonArrayMap.put("dataHfq", dataHfq);

        return jsonArrayMap;
    }

    private BigDecimal calculateDividend(String tradeDate, Map<String, JSONArray> dividendAmountMap, BigDecimal marketCap) {
        LocalDate tradeLocalDate = LocalDate.parse(tradeDate, DATE_FORMATTER);
        LocalDate startDate = tradeLocalDate.minusMonths(14);
        LocalDate endDate = tradeLocalDate.minusDays(1);
        // 获取14个月时间范围内的数据
        JSONArray dividendDataInRange = getDividendDataInRange(dividendAmountMap, startDate, endDate);
        // 如果数据点数大于等于2，处理特殊情况
        if (dividendDataInRange.size() >= 2) {
            LocalDate earliestDate = LocalDate.parse(dividendDataInRange.getJSONObject(0).getString("date"), DATE_FORMATTER);
            LocalDate latestDate = LocalDate.parse(dividendDataInRange.getJSONObject(dividendDataInRange.size() - 1).getString(
                    "date"), DATE_FORMATTER);
            if (ChronoUnit.MONTHS.between(latestDate, earliestDate) > 10) {
                // 获取 earliestDate + 1 天后的数据
                LocalDate nextDate = latestDate.plusDays(1);
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Map.Entry<String, JSONArray> entry : dividendAmountMap.entrySet()) {
                    LocalDate entryDate = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
                    // 确保 entryDate 在 nextDate 之后且在 earliestDate 之前或等于 earliestDate
                    if (entryDate.isAfter(nextDate) && (earliestDate == null || entryDate.isBefore(earliestDate) || entryDate.isEqual(earliestDate))) {
                        JSONArray amounts = entry.getValue();
                        for (int i = 0; i < amounts.size(); i++) {
                            totalAmount = totalAmount.add(amounts.getBigDecimal(i));
                        }
                    }
                }
                // 如果有找到分红金额，计算分红率
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    return calculateDividendRate(totalAmount, marketCap);
                }
            }
        }
        // 默认情况，使用交易日期（tradeDate）的数据
        JSONArray tradeDateData = dividendAmountMap.get(tradeDate);
        BigDecimal dividendAmountSum = BigDecimal.ZERO;
        if (tradeDateData != null) {
            dividendAmountSum = tradeDateData.stream().map(val -> (BigDecimal) val).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // 若没有交易日期的分红数据，寻找最近的日期数据
        if (dividendAmountSum.compareTo(BigDecimal.ZERO) == 0) {
            LocalDate closestDate =
                    dividendAmountMap.keySet().stream().map(date -> LocalDate.parse(date, DATE_FORMATTER)).filter(date -> !date.isAfter(tradeLocalDate)).max(LocalDate::compareTo).orElse(null);
            if (closestDate != null) {
                JSONArray closestDateData = dividendAmountMap.get(closestDate.format(DATE_FORMATTER));
                if (closestDateData != null) {
                    dividendAmountSum = closestDateData.stream().map(val -> (BigDecimal) val).reduce(BigDecimal.ZERO,
                            BigDecimal::add);
                }
            }
        }
        return calculateDividendRate(dividendAmountSum, marketCap);
    }

    // 获取指定日期范围内的分红数据
    private JSONArray getDividendDataInRange(Map<String, JSONArray> dividendAmountMap, LocalDate startDate, LocalDate endDate) {
        JSONArray resultArray = new JSONArray();
        for (Map.Entry<String, JSONArray> entry : dividendAmountMap.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                JSONArray dataArray = entry.getValue();
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("date", date.format(DATE_FORMATTER));
                    jsonObject.put("amount", dataArray.getBigDecimal(i));
                    resultArray.add(jsonObject);
                }
            }
        }
        return resultArray;
    }

    // 计算股息率
    private BigDecimal calculateDividendRate(BigDecimal dividendAmountSum, BigDecimal marketCap) {
        return dividendAmountSum.divide(marketCap, 6, RoundingMode.HALF_UP);
    }


    /**
     * 计算市净率
     *
     * @param dateNow      当前日期
     * @param bvpsDataJson 每股净资产的JSON对象
     * @param totalShare   当前股本数
     * @param marketCap    当前市值
     * @return 计算得到的市净率
     */
    private static BigDecimal calculatePbRatio(String dateNow, JSONObject bvpsDataJson, Long totalShare, BigDecimal marketCap) throws Exception {
        BigDecimal bvps = findLatestBvps(dateNow, bvpsDataJson);
        if (bvps.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal netAssets = bvps.multiply(new BigDecimal(totalShare));
            if (netAssets.compareTo(BigDecimal.ZERO) != 0) {
                return marketCap.divide(netAssets, 6, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 查找最近日期的每股净资产
     *
     * @param dateNow      当前日期
     * @param bvpsDataJson 每股净资产的JSON对象
     * @return 最近日期的每股净资产
     */
    private static BigDecimal findLatestBvps(String dateNow, JSONObject bvpsDataJson) {
        LocalDate currentDate = LocalDate.parse(dateNow, DATE_FORMATTER);
        TreeMap<LocalDate, BigDecimal> bvpsMap =
                bvpsDataJson.entrySet().stream().collect(Collectors.toMap(e -> LocalDate.parse(e.getKey(), DATE_FORMATTER),
                        e -> new BigDecimal(e.getValue().toString()), (existing, replacement) -> existing, TreeMap::new));

        BigDecimal bvps = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, BigDecimal> entry : bvpsMap.descendingMap().entrySet()) {
            if (!entry.getKey().isAfter(currentDate) && entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                bvps = entry.getValue();
                break;
            }
        }
        return bvps;
    }

    private Map<String, JSONArray> getDividendAmountMap(List<DailyVo> dailyVoList, Map<String, Object> totalShareMap,
                                                        JSONObject dailyDividendsJson) throws Exception {
        Map<String, JSONArray> dividendAmountMap = new HashMap<>(); // 存储每个交易日期对应的分红金额
        long totalShareOld = 0L; // 初始化旧的总股份数，用于比较
        // 遍历每日数据列表
        for (DailyVo dailyVo : dailyVoList) {
            String tradeDate = dailyVo.getTradeDate(); // 获取交易日期
            // 计算当前季度的结果日期
            // 从分红日期JSON中获取有效的分红日期
            // 从总股份数映射中获取当前交易日期的总股份数
            JSONArray dividend = dailyDividendsJson.getJSONArray(tradeDate);
            if (dividend != null && dividend.size() > 0) {
                Long totalShareNew = getTotalShare(totalShareMap, Long.parseLong(tradeDate) - 1 + "");
                if (totalShareNew == null) {
                    totalShareNew = totalShareOld;
                }
                dividendAmountMap.put(tradeDate, setDividendAmount(totalShareNew, dividend));
                totalShareOld = totalShareNew;
            }
        }
        return dividendAmountMap; // 返回计算好的分红金额映射
    }

    private Map<String, BigDecimal> getDividendsDataJson(List<DailyVo> dailyVoList, JSONObject dividendsDataJson) throws Exception {
        Map<String, BigDecimal> dividendAmountMap = new HashMap<>(); // 存储每个交易日期对应的分红金额
        // 遍历每日数据列表
        for (int i = 0; i < dailyVoList.size() - 1; i++) {
            //派息日前一天的收盘价格是x，10股派息了m元（比如财报10股派息0.35元，这里m就等于0.35），标注派息日这个时间点的系数p=x/（x-m/10）
            //派息日前一天的价格是x，10股转a股派b股派m元（比如财报10股派息0.35元，这里m就等于0.35），标注派息日这个时间点的系数p=【x/（x-m/10）】*【（10+a+b）/10】
            String tradeDate = dailyVoList.get(i).getTradeDate(); // 获取交易日期
            BigDecimal x = dailyVoList.get(i + 1).getClose();
            JSONArray dividend = dividendsDataJson.getJSONArray(tradeDate);
            if (dividend != null && dividend.size() > 0) {
                for (int j = 0; j < dividend.size(); j++) {
                    JSONObject jsonObject = dividend.getJSONObject(j);
                    BigDecimal a = jsonObject.getBigDecimal("a");
                    BigDecimal b = jsonObject.getBigDecimal("b");
                    BigDecimal m = jsonObject.getBigDecimal("m");
                    // 默认算法: p = x / (x - m / 10)
                    BigDecimal ten = new BigDecimal("10");
                    BigDecimal mDiv10 = m.divide(ten, 4, RoundingMode.HALF_UP);
                    BigDecimal denominator = x.subtract(mDiv10);

                    BigDecimal p = new BigDecimal(0);
                    if (denominator.compareTo(BigDecimal.ZERO) != 0) {
                        p = x.divide(denominator, 4, RoundingMode.HALF_UP);
                        // 条件修改算法
                        if (a.compareTo(BigDecimal.ZERO) != 0 || b.compareTo(BigDecimal.ZERO) != 0) {
                            //p=【x/（x-m/10）】*【（10+a+b）/10】
                            BigDecimal adjustmentFactor = ten.add(a).add(b).divide(ten, 4, RoundingMode.HALF_UP);
                            p = p.multiply(adjustmentFactor);
                        }
                    }
                    dividendAmountMap.put(tradeDate, p);
                }
            }
        }
        return dividendAmountMap; // 返回计算好的分红金额映射
    }


    private static JSONArray setDividendAmount(Long totalShareNew, JSONArray dividend) {
        JSONArray data = new JSONArray();
        if (dividend != null && dividend.size() > 0) {
            for (int i = 0; i < dividend.size(); i++) {
                //分红金额
                BigDecimal dividendAmount = dividend.getBigDecimal(i).multiply(new BigDecimal(totalShareNew));
                data.add(dividendAmount);
            }
        }
        return data;
    }


    public Long getTotalShare(Map<String, Object> totalShareMap, String tradeDate) {
        Long totalShare = (Long) totalShareMap.get(tradeDate);
        if (totalShare == null) {
            for (int i = 1; i < 10; i++) {
                totalShare = (Long) totalShareMap.get(Long.parseLong(tradeDate) - i + "");
                if (totalShare != null) {
                    break;
                }
            }
            return totalShare;
        }
        return totalShare;
    }


    /**
     * 计算市盈率（PE Ratio TTM）和其他财务指标，并将结果封装到JSON数组中
     *
     * @param netProfitCacheMap 净利润数据映射
     * @param grossRevenueMap   营业收入数据映射
     * @param tradeDate         当前交易日期
     * @param close             当前股价
     * @param totalShare        总股份数
     * @param marketCap         市值
     * @param dateNow           当前季度日期
     * @param dividend          股息率
     * @param pbRatio           市净率
     * @return 包含市盈率和其他财务指标的JSON数组
     * @throws Exception 如果计算过程中发生异常
     */
    private JSONArray peRatioTtmInfo(Map<String, Map<String, Object>> netProfitCacheMap,
                                     Map<String, Map<String, Object>> grossRevenueMap, String tradeDate, BigDecimal close,
                                     Long totalShare, BigDecimal marketCap, String dateNow, BigDecimal dividend,
                                     BigDecimal pbRatio) throws Exception {
        JSONArray data = new JSONArray();
        //获取到最近四个季度
        List<String> previousThreeQuarters = DateUtils.getPreviousThreeQuarters(dateNow);
        //去年的当前季度
        String lastYear = DateUtils.getPreviousYearDate(dateNow);
        //四个季度
        List<String> lastYearPreviousThreeQuarters = DateUtils.getPreviousThreeQuarters(lastYear);

        //前推4个季度的纯利润
        BigDecimal recentPeriodSum = extractQuarterlyData(netProfitCacheMap, previousThreeQuarters);
        //对应前推一年的的四个季度的纯利润
        BigDecimal previousPeriodSum = extractQuarterlyData(netProfitCacheMap, lastYearPreviousThreeQuarters);

        // 前推4个季度的利润增长率    22年后3个季度+23年一季度的  这些加起来，比上，21年后3个季度+22年一季度
        BigDecimal growthRate = calculateGrowthRate(recentPeriodSum, previousPeriodSum);
        //前推4个季度的营业收入
        BigDecimal currentQuarterGrossRevenue = extractQuarterlyData(grossRevenueMap, previousThreeQuarters);
        //对应前推一年的的四个季度的营业收入
        BigDecimal lastQuarterGrossRevenue = extractQuarterlyData(grossRevenueMap, lastYearPreviousThreeQuarters);

        // 前推4个季度的营业增长率    22年后3个季度+23年一季度的  这些加起来，比上，21年后3个季度+22年一季度
        BigDecimal revenueGrowthRate = calculateGrowthRate(currentQuarterGrossRevenue, lastQuarterGrossRevenue);

        // 计算市盈率（PE Ratio TTM）
        BigDecimal peRatioTtm = new BigDecimal(0);
        if (recentPeriodSum != null && recentPeriodSum.compareTo(BigDecimal.ZERO) > 0) {
            //市盈率TTm
            peRatioTtm = marketCap.divide(recentPeriodSum, 4, RoundingMode.HALF_UP);
        }

        //市值回报率：利润除以总市值
        BigDecimal marketCapReturnRate = new BigDecimal(0);
        if (recentPeriodSum != null && marketCap.compareTo(BigDecimal.ZERO) != 0) {
            marketCapReturnRate = recentPeriodSum.divide(marketCap, 4, RoundingMode.HALF_UP);
        }
        BigDecimal netAssets = new BigDecimal(0);
        if (pbRatio != null && pbRatio.compareTo(BigDecimal.ZERO) != 0) {
            //净资产
            netAssets = marketCap.divide(pbRatio, 4, RoundingMode.HALF_UP);
        }

        //资产回报率：前4个季度利润除以净资产
        BigDecimal returnOnAssets = new BigDecimal(0);
        if (recentPeriodSum != null && netAssets.compareTo(BigDecimal.ZERO) != 0) {
            returnOnAssets = recentPeriodSum.divide(netAssets, 4, RoundingMode.HALF_UP);
        }
        //利润率，就是用利润除以收入
        BigDecimal profitRate = new BigDecimal(0);
        if (recentPeriodSum != null && currentQuarterGrossRevenue.compareTo(BigDecimal.ZERO) != 0) {
            profitRate = recentPeriodSum.divide(currentQuarterGrossRevenue, 4, RoundingMode.HALF_UP);
        }
        data.add(DateUtils.formatDateStringWithNextDay(tradeDate)); //交易日期
        data.add(totalShare);//总本股
        data.add(close);//价格
        data.add(marketCap);//总市值
        data.add(recentPeriodSum);//前推4个季度利润
        data.add(peRatioTtm);//市盈率
        data.add(growthRate);//前推4个季度的平均增长率
        data.add(currentQuarterGrossRevenue);//前推4个季度的营业收入
        data.add(revenueGrowthRate);//收入增长率
        data.add(growthRate);//利润增长率
        data.add(dividend);//股息率
        data.add(pbRatio);//市净率
        data.add(netAssets);//净资产
        data.add(returnOnAssets);//资产回报率
        data.add(marketCapReturnRate);//市值回报率
        data.add(profitRate);//市值回报率
        return data;
    }


    /**
     * 前推4个季度的平均增长率
     *
     * @param recentPeriodSum   当前日期前推4个季度金额总和
     * @param previousPeriodSum 去年当前日期前推4个季度金额总和
     * @return 前推4个季度的平均增长率
     */
    private BigDecimal calculateGrowthRate(BigDecimal recentPeriodSum, BigDecimal previousPeriodSum) {
        if (previousPeriodSum.compareTo(BigDecimal.ZERO) != 0) {
            // 计算增长率：((recentPeriodSum / previousPeriodSum) - 1)
            return recentPeriodSum.divide(previousPeriodSum, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        }
        return BigDecimal.ZERO;
    }


    private static BigDecimal extractQuarterlyData(Map<String, Map<String, Object>> data, List<String> quarters) {
        BigDecimal result = new BigDecimal(0);
        for (String date : quarters) {
            String year = date.substring(0, 4);
            String quarter = getQuarterFromDate(date);
            Map<String, Object> yearData = data.get(year);
            if (yearData != null) {
                BigDecimal quarterValue = new BigDecimal((Long) yearData.get(quarter));
                result = result.add(quarterValue);
            }
        }
        return result;
    }


    private static String getQuarterFromDate(String date) {
        String month = date.substring(4, 6);
        switch (month) {
            case "03":
                return "q1";
            case "06":
                return "q2";
            case "09":
                return "q3";
            case "12":
                return "q4";
            default:
                return "unknown";
        }
    }


    private JSONObject getJsonObject(String apiName, String tsCode) {
        try {
            JSONObject paramsJson = new JSONObject();
            paramsJson.put("api_name", apiName);
            paramsJson.put("token", TOKEN);
            JSONObject params = new JSONObject();
            params.put("ts_code", tsCode);
            paramsJson.put("params", params);
            return tuShareApi.genericMethods(paramsJson).execute().body();
        } catch (IOException e) {
            log.error("请求接口获取数据出现异常！！接口名称:{},股票代码:{}", apiName, tsCode, e);
        }
        return null;
    }

    /**
     * 获取历史股本数
     *
     * @return 股票map
     */
    private Map<String, Map<String, Object>> getTotalShareMap(String tsCode) throws IOException {
        Map<String, Object> totalShareMap = new TreeMap<>();
        Map<String, Object> dvRatioMap = new TreeMap<>();
        Map<String, Object> pbMap = new TreeMap<>();
//        String path = "D:\\" + tsCode + "_daily_basic.zip";

        String path = "/home/tomcat/aShare_data/Agu_daily_basic/" + tsCode + "_daily_basic.zip";
        Map<String, Map<String, Object>> data = new HashMap<>();
//        JSONObject jsonObject = getJsonObject("daily_basic", tsCode);
        JSONObject jsonObject = JSONObject.parseObject(DataProcessorUtil.extractAndReadData(path));
        if (jsonObject != null && jsonObject.getInteger("code").equals(0)) {
            JSONArray dataArray = jsonObject.getJSONObject("data").getJSONArray("items");

            Set<String> tradeDateSet = new HashSet<>();

            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray jsonArray = dataArray.getJSONArray(i);
                //交易日期
                String tradeDate = jsonArray.getString(1);
                if (tradeDateSet.contains(tradeDate)) {
                    continue;
                }
                tradeDateSet.add(tradeDate);
                //市净率（总市值/净资产）
                BigDecimal pb = jsonArray.getBigDecimal(8);
                //股息率（TTM）
                BigDecimal dvTtm = jsonArray.getBigDecimal(12);
                if (dvTtm == null) {
                    dvTtm = BigDecimal.ZERO;
                }
                BigDecimal totalShare = jsonArray.getBigDecimal(13);
                long totalShareNew = totalShare.multiply(new BigDecimal(10000)).longValue();
                totalShareMap.put(tradeDate, totalShareNew);
                dvRatioMap.put(tradeDate, dvTtm.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
                pbMap.put(tradeDate, pb);
            }
        }
        data.put("totalShare", totalShareMap);
        data.put("dvTtm", dvRatioMap);
        data.put("pb", pbMap);

        return data;
    }


}
