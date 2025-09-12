package com.ailu.exchange.service;

import com.ailu.exchange.dao.domaim.HistoricalStocksInfo;
import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.*;
import com.ailu.exchange.dao.vo.ConditionLogVo;
import com.ailu.exchange.service.redis.ObjectRedisService;
import com.ailu.exchange.utils.DateUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.ailu.exchange.utils.FilterUtils.applyFilter;
import static com.ailu.exchange.utils.FilterUtils.getComparator;

@Service
@Slf4j
public class HkStockInfoService {

    @Resource
    HkStockInfoMapper hkStockInfoMapper;

    @Resource
    HkHistoricalStocksInfoMapper hkHistoricalStocksInfoMapper;

    /**
     * 2000-2024年净利润
     */
    private final static Map<String, Map<String, Map<String, Object>>> NET_PROFIT_CACHE_MAP = new HashMap<>();

    /**
     * 2000-2024年营业收入
     */
    private final static Map<String, Map<String, Map<String, Object>>> GROSS_REVENUE_CACHE_MAP = new HashMap<>();
    /**
     * 2000-2023年 分红金额
     */
    private final static Map<String, JSONObject> TOTAL_BONUS_CACHE_MAP = new HashMap<>();
    /**
     * A股基础信息全局缓存
     */
    public final static List<StockInfo> STOCK_INFO_LIST_GLOBAL_CACHE = new ArrayList<>();

    /**
     * 保存 取点后的股票代码 获取对应的股票代码和股票简称
     */
    public final static Map<String, String> ALL_TICKER_SYMBOL_MAP = new HashMap<>();

    @Resource
    ObjectRedisService objectRedisService;


    private static final Long LIMIT_VALUE = 10L;

    private static String CONDITION_LOG_KEY = "HK_CONDITION_LOG_KEY";

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

    /**
     * 排序逻辑
     *
     * @param sortBy    排序字段
     * @param ascending 排序规则
     * @return 排序后的集合
     */


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

                //当前价格
                BigDecimal stockPrice = stockInfo.getStockPrice();
                //每股净资产
                BigDecimal bookValuePerShare = stockInfo.getBookValuePerShare();
                if (bookValuePerShare.compareTo(BigDecimal.ZERO) != 0) {
                    stockInfo.setPbRatio(stockPrice.divide(bookValuePerShare, 4, RoundingMode.HALF_UP));
                }

                //去年
                String lastYear = DateUtils.getLastYear(1);
                JSONObject currentYearJson = netProfitJson.getJSONObject(currentYear);
                //获取去年的四个季度的近利润
                JSONObject lastYearJson = netProfitJson.getJSONObject(lastYear);
                if (currentYearJson != null && lastYearJson != null) {
                    BigDecimal netProfitTotal = getNetProfitTotal(currentYearJson, lastYearJson);
                    //总市值/近四个季度净利润之和
                    if (netProfitTotal.compareTo(BigDecimal.ZERO) != 0) {
                        stockInfo.setPeRatioTtm(marketCap.divide(netProfitTotal, 4, RoundingMode.HALF_UP));
                    }
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
//                if (totalBonus != null) {
//                    String[] pastYearsIncludingCurrent = DateUtils.getPastYearsIncludingCurrent(3);
//                    BigDecimal totalAll = BigDecimal.ZERO;
//                    for (String years : pastYearsIncludingCurrent) {
//                        BigDecimal total = totalBonus.getBigDecimal(years);
//                        if (total != null) {
//                            totalAll = totalAll.add(total);
//                        }
//                    }
//                    //当前年的分红总量
//                    BigDecimal currentYearTotalBonus = totalBonus.getBigDecimal(currentYear);
//                    //去年的分红率
//                    BigDecimal lastYearTotalBonus = totalBonus.getBigDecimal(lastYear);
//                    //获取当前年的四个季度的近利润
//                    if (marketCap.compareTo(BigDecimal.ZERO) != 0) {
//                        //2024年股息率=2025年分红总额/总市值
//                        stockInfo.setLastYearDividendRate(BigDecimal.ZERO.divide(marketCap, 6, RoundingMode.HALF_EVEN)
//                        .multiply(new BigDecimal(100)));
//                    }
//                    if (marketCap.compareTo(BigDecimal.ZERO) != 0 && currentYearTotalBonus != null) {
//                        //2023年股息率=2024年分红总额/总市值
//                        stockInfo.setLastYearDividendRate2(currentYearTotalBonus.divide(marketCap, 6, RoundingMode.HALF_EVEN)
//                        .multiply(new BigDecimal(100)));
//                    }
//                    if (marketCap.compareTo(BigDecimal.ZERO) != 0 && lastYearTotalBonus != null) {
//                        //2022年股息率=2023年分红总额/总市值
//                        stockInfo.setLastYearDividendRate3(lastYearTotalBonus.divide(marketCap, 6, RoundingMode.HALF_EVEN)
//                        .multiply(new BigDecimal(100)));
//                    }
//                    if (marketCap.compareTo(BigDecimal.ZERO) != 0) {
//                        //近三年分红率=2024年2023年2022年分红金额之和/3/总市值s
//                        stockInfo.setAvgDividendYield3yr(totalAll.divide(new BigDecimal("3"), 6, RoundingMode.HALF_EVEN)
//                        .divide(marketCap, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
//                    }
//                }
                BigDecimal currentAnnualDividend = stockInfo.getCurrentAnnualDividend();
                //获取当前年的四个季度的近利润
                if (marketCap.compareTo(BigDecimal.ZERO) != 0 && currentAnnualDividend != null) {
                    //2024年的分红率=2024年分红总额/总市值
                    stockInfo.setLastYearDividendRate(currentAnnualDividend.divide(marketCap, 6, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)));
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

                BigDecimal forecastGrossRevenueGrowthRateNew =
                        getForecastGrowthRateInfoNew(JSONObject.parseObject(JSON.toJSONString(stockInfo.getGrossRevenueMap())));

                BigDecimal forecastNetProfitGrowthRateNew =
                        getForecastGrowthRateInfoNew(JSONObject.parseObject(JSON.toJSONString(stockInfo.getNetProfitMap())));

                stockInfo.setRevGrowthRateNew(forecastGrossRevenueGrowthRateNew);
                stockInfo.setProfitGrowthRateNew(forecastNetProfitGrowthRateNew);

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
        // 读取当前年季度数据
        BigDecimal firstQuarterRevenue = currentYearJson.getBigDecimal("q1");
        BigDecimal secondQuarterRevenue = currentYearJson.getBigDecimal("q2");
        BigDecimal thirdQuarterRevenue = currentYearJson.getBigDecimal("q3");
        BigDecimal fourthQuarterRevenue = currentYearJson.getBigDecimal("q4");

        // 读取去年的季度数据
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
        List<HistoricalStocksInfo> singleAll = hkHistoricalStocksInfoMapper.findAll();
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
        }
        getStockInfos();
    }

    public void getStockInfos() {
        log.info("开始获取股票基础信息");
        //获取股票全部基础信息
        STOCK_INFO_LIST_GLOBAL_CACHE.clear();
        try {
            List<StockInfo> all = hkStockInfoMapper.findAll();
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
                    if (datas != null) {
                        stockInfo.setTickerSymbol(datas.split("_")[0]);
                    }
                }
                hkStockInfoMapper.batchInsertNew(stockInfos);
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
}
