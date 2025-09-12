package com.ailu.exchange.dao.domaim;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class StockInfo implements Cloneable {

    private Integer id;
    private String tickerSymbol;        // 股票代码

    private String tickerSymbolNew;        // 股票代码
    private String tickerName;          // 股票简称
    private BigDecimal marketCap;       // 总市值（股价*总股本）
    private BigDecimal peRatioTtm;      // 市盈率ttm（总市值/近四个季度净利润之和）
    private BigDecimal pbRatio;      // 市净率（股价/每股净资产）
    private BigDecimal avgDividendYield3yr; // 近三年平均分红率
    private BigDecimal stockPrice;      // 股价 元
    private Long totalShares;           // 总股本(股)
    private BigDecimal bookValuePerShare; // 每股净资产

    private BigDecimal totalAssets;     // 资产总计
    private BigDecimal totalLiabilities; // 负债合计
    private BigDecimal netAssets;       // 净资产

    private String capitalChanges;      // 股本变化
    private BigDecimal priceToEarningsRatio; // 市盈率

    private String industryCategory;    // 所属同花顺行业
    private BigDecimal performanceGrowthScore; // 业绩增长评分
    private BigDecimal overallScore; // 综合评分
    private BigDecimal liudaScore; // 刘大评分


    public Map<String, Map<String, Object>> grossRevenueMap; // 营业收入
    private Map<String, Map<String, Object>> netProfitMap; // 净利润

    private BigDecimal lastYearDividendRate; //2024年股息率
    private BigDecimal lastYearDividendRate2; //2023年股息率
    private BigDecimal lastYearDividendRate3; //2022年股息率


    private String targetPrice1; // 第一目标价
    private String targetPrice2; // 第二目标价
    private String note1; //备注1
    private String note2; //备注2
    private BigDecimal forecastRevenueGrowthRate; //收入预测增长率
    private BigDecimal forecastProfitGrowthRate;//利润预测增长率
    private BigDecimal forecastPe3Years;//预测3年后PE
    private BigDecimal annualPriceChange;//一年内涨跌幅

    private BigDecimal revGrowthRateNew; // 新营收增长率
    private BigDecimal profitGrowthRateNew; // 新利润增长率
    private BigDecimal peForecastNew; //新预测PE

    private BigDecimal maxPrice5y; //近5年最高价
    private BigDecimal currentAnnualDividend; //当年分红金额  用来动态计算当前年的股息率

    private BigDecimal revGrowthRateNew2;//新营收增长率

    public StockInfo(String tickerSymbol, String tickerName, BigDecimal marketCap, BigDecimal peRatioTtm, BigDecimal pbRatio,
                     BigDecimal avgDividendYield3yr, BigDecimal stockPrice, Long totalShares, BigDecimal bookValuePerShare,
                     String capitalChanges, BigDecimal priceToEarningsRatio, BigDecimal totalAssets,
                     BigDecimal totalLiabilities, BigDecimal netAssets, String industryCategory) {
        this.tickerSymbol = tickerSymbol;
        this.tickerName = tickerName;
        this.marketCap = marketCap;
        this.peRatioTtm = peRatioTtm;
        this.pbRatio = pbRatio;
        this.avgDividendYield3yr = avgDividendYield3yr;
        this.stockPrice = stockPrice;
        this.totalShares = totalShares;
        this.bookValuePerShare = bookValuePerShare;
        this.capitalChanges = capitalChanges;
        this.priceToEarningsRatio = priceToEarningsRatio;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.netAssets = netAssets;
        this.industryCategory = industryCategory;
    }

    public StockInfo(String tickerSymbol, String tickerName, BigDecimal marketCap, BigDecimal pbRatio, BigDecimal stockPrice,
                     Long totalShares,
                     BigDecimal bookValuePerShare, BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal netAssets,
                     String industryCategory) {
        this.tickerSymbol = tickerSymbol;
        this.tickerName = tickerName;
        this.stockPrice = stockPrice;
        this.marketCap = marketCap;
        this.pbRatio = pbRatio;
        this.totalShares = totalShares;
        this.bookValuePerShare = bookValuePerShare;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.netAssets = netAssets;
        this.industryCategory = industryCategory;
    }


    public StockInfo(String tickerSymbol, BigDecimal stockPrice, BigDecimal annualPriceChange, BigDecimal maxPrice5y) {
        this.tickerSymbol = tickerSymbol;
        this.stockPrice = stockPrice;
        this.annualPriceChange = annualPriceChange;
        this.maxPrice5y = maxPrice5y;
    }

    public StockInfo(String tickerSymbol, BigDecimal stockPrice, BigDecimal annualPriceChange, BigDecimal marketCap,
                     Long totalShares) {
        this.tickerSymbol = tickerSymbol;
        this.annualPriceChange = annualPriceChange;
        this.stockPrice = stockPrice;
        this.marketCap = marketCap;
        this.totalShares = totalShares;
    }

    public StockInfo(String tickerSymbol, BigDecimal lastYearDividendRate, BigDecimal lastYearDividendRate2,
                     BigDecimal lastYearDividendRate3, BigDecimal currentAnnualDividend) {
        this.tickerSymbol = tickerSymbol;
        this.lastYearDividendRate = lastYearDividendRate;
        this.lastYearDividendRate2 = lastYearDividendRate2;
        this.lastYearDividendRate3 = lastYearDividendRate3;
        this.currentAnnualDividend = currentAnnualDividend;
    }


    public StockInfo(String tickerSymbol, BigDecimal bookValuePerShare) {
        this.tickerSymbol = tickerSymbol;
        this.bookValuePerShare = bookValuePerShare;

    }


    public StockInfo() {
    }

    @Override
    public StockInfo clone() {
        try {
            // 如果有其他引用类型字段需要深度克隆
            return (StockInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // 这是不会发生的
        }
    }

}
