package com.ailu.exchange.utils;

import com.ailu.exchange.dao.domaim.StockInfo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FilterUtils {


    public static boolean applyFilterIf(String value, String filterValue, int conditionType) {
        boolean isNumericValue = isNumeric(value); // 判断 value 是否为数字
        boolean isNumericFilter = isNumeric(filterValue); // 判断 filterValue 是否为数字

        if (isNumericValue && isNumericFilter) {
            // 如果 value 和 filterValue 都是数字
            BigDecimal bigDecimalValue = new BigDecimal(value);
            BigDecimal bigDecimalFilterValue = new BigDecimal(filterValue);
            switch (conditionType) {
                case 0:
                    return value.contains(filterValue);
                case 1: // 等于
                    return bigDecimalValue.compareTo(bigDecimalFilterValue) == 0;
                case 2: // 大于
                    return bigDecimalValue.compareTo(bigDecimalFilterValue) > 0;
                case 3: // 小于
                    return bigDecimalValue.compareTo(bigDecimalFilterValue) < 0;
                default:
                    throw new IllegalArgumentException("无效的条件类型");
            }
        } else {
            // 至少有一个 value 或 filterValue 不是数字
            switch (conditionType) {
                case 0: // 包含
                    return value != null && value.contains(filterValue);
                case 1: // 等于
                    return value != null && value.equals(filterValue);
                case 2: // 大于 (不适用于字符串)
                    return false;
                case 3: // 小于 (不适用于字符串)
                    return false;
                default:
                    throw new IllegalArgumentException("无效的条件类型");
            }
        }
    }

    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str); // 尝试将字符串转换为 BigDecimal
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static boolean applyFilter(StockInfo stockInfo, JSONArray filterList) {
        if (filterList == null || filterList.isEmpty()) {
            // 如果 filterList 为空，则不需要过滤，返回 true 或其他适当值
            return true;
        }
        List<JSONObject> filters = convertJSONArrayToList(filterList);

        return filters.stream().allMatch(filter -> {
            String name = filter.getString("name");
            String filterValue = filter.getString("filterValue");
            String inputText = filter.getString("inputText");
            switch (name) {
                case "grossRevenueMap":
                case "netProfitMap":
                    return applyRevenueFilter(name, stockInfo, filterValue, inputText, filter.getString("year"),
                            filter.getInteger("quarter"), filter.getInteger("amountType"));
                default:
                    return applyStandardFilter(stockInfo, name, filterValue, inputText);
            }
        });
    }

    private static boolean applyStandardFilter(StockInfo stockInfo, String name, String filterValue, String inputText) {
        Object value = getValueByName(stockInfo, name);
        if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
            return false;
        }
        return applyFilterUnify(value.toString(), filterValue, inputText);
    }

    private static Object getValueByName(StockInfo stockInfo, String name) {
        if (stockInfo == null || name == null) {
            return null;
        }
        switch (name) {
            case "tickerSymbol":
                return stockInfo.getTickerSymbol();
            case "tickerName":
                return stockInfo.getTickerName();
            case "marketCap":
                return stockInfo.getMarketCap();
            case "pbRatio":
                return stockInfo.getPbRatio();
            case "peRatioTtm":
                return stockInfo.getPeRatioTtm();
            case "avgDividendYield3yr":
                return stockInfo.getAvgDividendYield3yr();
            case "stockPrice":
                return stockInfo.getStockPrice();
            case "totalShares":
                return stockInfo.getTotalShares();
            case "bookValuePerShare":
                return stockInfo.getBookValuePerShare();
            case "totalAssets":
                return stockInfo.getTotalAssets();
            case "totalLiabilities":
                return stockInfo.getTotalLiabilities();
            case "netAssets":
                return stockInfo.getNetAssets();
            case "capitalChanges":
                return stockInfo.getCapitalChanges();
            case "priceToEarningsRatio":
                return stockInfo.getPriceToEarningsRatio();
            case "industryCategory":
                return stockInfo.getIndustryCategory();
            case "performanceGrowthScore":
                return stockInfo.getPerformanceGrowthScore();
            case "overallScore":
                return stockInfo.getOverallScore();
            case "liudaScore":
                return stockInfo.getLiudaScore();
            case "lastYearDividendRate":
                BigDecimal lastYearDividendRate = stockInfo.getLastYearDividendRate();
                return lastYearDividendRate != null ? lastYearDividendRate.divide(new BigDecimal("100"), 6,
                        RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            case "lastYearDividendRate2":
                BigDecimal lastYearDividendRate2 = stockInfo.getLastYearDividendRate2();
                return lastYearDividendRate2 != null ? lastYearDividendRate2.divide(new BigDecimal("100"), 6,
                        RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            case "targetPrice1":
                return stockInfo.getTargetPrice1();
            case "targetPrice2":
                return stockInfo.getTargetPrice2();
            case "note1":
                return stockInfo.getNote1();
            case "note2":
                return stockInfo.getNote2();
            case "forecastRevenueGrowthRate":
                BigDecimal forecastRevenueGrowthRate = stockInfo.getForecastRevenueGrowthRate();
                return forecastRevenueGrowthRate != null ? forecastRevenueGrowthRate.divide(new BigDecimal("100"), 6,
                        RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            case "forecastProfitGrowthRate":
                BigDecimal forecastProfitGrowthRate = stockInfo.getForecastProfitGrowthRate();
                return forecastProfitGrowthRate != null ? forecastProfitGrowthRate.divide(new BigDecimal("100"), 6,
                        RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            case "forecastPe3Years":
                return stockInfo.getForecastPe3Years();
            case "annualPriceChange":
                BigDecimal annualPriceChange = stockInfo.getAnnualPriceChange();
                return annualPriceChange != null ? annualPriceChange.divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN) :
                        BigDecimal.ZERO;
            case "revGrowthRateNew":
                BigDecimal revGrowthRateNew = stockInfo.getRevGrowthRateNew();
                return revGrowthRateNew != null ? revGrowthRateNew.divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN) :
                        BigDecimal.ZERO;
            case "profitGrowthRateNew":
                BigDecimal profitGrowthRateNew = stockInfo.getProfitGrowthRateNew();
                return profitGrowthRateNew != null ? profitGrowthRateNew.divide(new BigDecimal("100"), 6,
                        RoundingMode.HALF_EVEN) : BigDecimal.ZERO;
            case "peForecastNew":
                return stockInfo.getPeForecastNew();
            default:
                return null;
        }
    }


    /**
     * 排序逻辑
     *
     * @param orderByFields 排序字段
     * @param sortDirection 排序规则
     * @return 排序后的集合
     */
    public static Comparator<StockInfo> getComparator(String orderByFields, String sortDirection) {
        Comparator<StockInfo> comparator;
        switch (orderByFields) {
            // 按需添加其他字段
            case "ticker_symbol":
                comparator = Comparator.comparing(StockInfo::getTickerSymbol, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "pe_ratio_ttm":
                comparator = Comparator.comparing(StockInfo::getPeRatioTtm, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "pb_ratio":
                comparator = Comparator.comparing(StockInfo::getPbRatio, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "last_year_dividend_rate":
                comparator = Comparator.comparing(StockInfo::getLastYearDividendRate,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "last_year_dividend_rate2":
                comparator = Comparator.comparing(StockInfo::getLastYearDividendRate2,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "forecast_revenue_growth_rate":
                comparator = Comparator.comparing(StockInfo::getForecastRevenueGrowthRate,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "forecast_profit_growth_rate":
                comparator = Comparator.comparing(StockInfo::getForecastProfitGrowthRate,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "forecast_pe_3_years":
                comparator = Comparator.comparing(StockInfo::getForecastPe3Years,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "annual_price_change":
                comparator = Comparator.comparing(StockInfo::getAnnualPriceChange,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "rev_growth_rate_new":
                comparator = Comparator.comparing(StockInfo::getRevGrowthRateNew,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "profit_growth_rate_new":
                comparator = Comparator.comparing(StockInfo::getProfitGrowthRateNew,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "pe_forecast_new":
                comparator = Comparator.comparing(StockInfo::getPeForecastNew, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "market_cap":
                comparator = Comparator.comparing(StockInfo::getMarketCap, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "performance_growth_score":
                comparator = Comparator.comparing(StockInfo::getPerformanceGrowthScore,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "overall_score":
                comparator = Comparator.comparing(StockInfo::getOverallScore, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "liuda_score":
                comparator = Comparator.comparing(StockInfo::getLiudaScore, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            case "last_year_dividend_rate3":
                comparator = Comparator.comparing(StockInfo::getLastYearDividendRate3,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(StockInfo::getTickerSymbol, Comparator.nullsFirst(Comparator.naturalOrder()));
                break;
        }

        return sortDirection.equalsIgnoreCase("ASC") ? comparator : comparator.reversed();
    }

    // 过滤逻辑
    private static List<JSONObject> convertJSONArrayToList(JSONArray jsonArray) {
        return IntStream.range(0, jsonArray.size()).mapToObj(jsonArray::getJSONObject).collect(Collectors.toList());
    }

    private static boolean applyFilterUnify(String value, String filterValue, String inputText) {
        try {
            //当前值
            return applyFilterIf(value, inputText, Integer.parseInt(filterValue));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean applyRevenueFilter(String name, StockInfo stockInfo, String filterValue, String inputText,
                                              String year, Integer quarter, Integer amountType) {
        try {
            Map<String, Object> stringObjectMap;
            if (name.equalsIgnoreCase("grossRevenueMap")) {
                stringObjectMap = stockInfo.getGrossRevenueMap().get(year);
            } else {
                stringObjectMap = stockInfo.getNetProfitMap().get(year);
            }
            //当前值
            BigDecimal quarterKeyData = getQuarterKeyData(quarter, amountType, stringObjectMap);
            return applyFilterIf(quarterKeyData.toString(), inputText, Integer.parseInt(filterValue));
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 获取对应的季度金额或者是增长率
     *
     * @param quarter     第几季度
     * @param amountType  金额或者增长率
     * @param quarterData 原数组
     * @return
     */
    private static BigDecimal getQuarterKeyData(Integer quarter, Integer amountType, Map<String, Object> quarterData) {
        switch (quarter) {
            case 0:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("tAmt") + "");
                } else {
                    return new BigDecimal(quarterData.get("gr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            case 1:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("q1") + "");
                } else {
                    return new BigDecimal(quarterData.get("q1Gr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            case 2:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("q2") + "");
                } else {
                    return new BigDecimal(quarterData.get("q2Gr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            case 3:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("q3") + "");
                } else {
                    return new BigDecimal(quarterData.get("q3Gr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            case 4:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("q4") + "");
                } else {
                    return new BigDecimal(quarterData.get("q4Gr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            case 5:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("midR") + "");
                } else {
                    return new BigDecimal(quarterData.get("midRGr") + "").divide(new BigDecimal("100"), 6,
                            RoundingMode.HALF_EVEN);
                }
            case 6:
                if (amountType == 0) {
                    return new BigDecimal(quarterData.get("q3R") + "");
                } else {
                    return new BigDecimal(quarterData.get("q3RGr") + "").divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN);
                }
            default:
                throw new IllegalArgumentException("Invalid quarter: " + quarter);
        }
    }


}
