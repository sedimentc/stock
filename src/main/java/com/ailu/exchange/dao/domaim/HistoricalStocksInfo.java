package com.ailu.exchange.dao.domaim;


import lombok.Data;

@Data
public class HistoricalStocksInfo {

    private String tickerSymbol;        // 股票代码
    private String tickerName;          // 股票简称
    private String grossRevenue;      // 营业总收入 JSON 串
    private String netProfit;         // 净利润 JSON 串
    private String totalBonus;        // 分红总额 JSON 串
    private String netProfitDeduction;// 扣除非经常损益的利润 JSON 串
    private String announceDate;// 实际披露日期
    private String dailyDividends;// 每日分红
    private String netAssets; //净资产
    private String bvpsData; //每股净资产
    private String totalBonusNew; //每股净资产

    private String dividendsData; //详细股息日分红量


    public HistoricalStocksInfo() {
    }

    public HistoricalStocksInfo(String tickerSymbol, String dividendsData) {
        this.tickerSymbol = tickerSymbol;
        this.dividendsData = dividendsData;
    }
}
