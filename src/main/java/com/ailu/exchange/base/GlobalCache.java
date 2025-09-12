package com.ailu.exchange.base;


import com.ailu.exchange.dao.domaim.TokenMainInfo;
import com.ailu.exchange.dao.vo.AddressInfoVo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalCache {


    /**
     * 合约id key val=map 分类名 key   val 合约地址
     */
    public static final Map<String, Map<String, List<String>>> CONTRACT_ID_TYPE_MAP = new ConcurrentHashMap<>();

    /**
     * 合约id  对应的分类名称
     */
    public static final Map<String, Set<String>> CONTRACT_ID_TYPE_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * 合约id  对应的所有地址
     */
    public static final Map<String, Set<String>> CONTRACT_ID_ADDRESS_ALL_MAP = new ConcurrentHashMap<>();


    /**
     * 保存地址关系
     */
    public static final Map<String, List<TokenMainInfo>> CONTRACT_ID_RELATION_MAP = new ConcurrentHashMap<>();

    /**
     * 获取VIRTUAL缓存数据
     */
    public static final Map<String, List<AddressInfoVo>> VIRTUAL_CACHE_DATA_MAP = new ConcurrentHashMap<>();

    /**
     * 代币名称对应的地址
     */
    public static final Map<String, String> TOKEN_NAME_AND_CONTRACT_ID = new ConcurrentHashMap<>();

    /**
     * key  标签编号
     * value 标签实际使用名称
     */
    public static final Map<String, String> TOKEN_BY_NAME_CONFIG_INFO_MAP = new ConcurrentHashMap<>();

    public static final Map<String, String> TOKEN_BY_NAME_CONFIG_INFO_MAP_NEW = new ConcurrentHashMap<>();


    /**
     * key   标签实际使用名称
     * value  标签编号
     */
    public static final Map<String, String> TOKEN_BY_VALUE_CONFIG_INFO_MAP2 = new ConcurrentHashMap<>();

    /**
     * key  标签编号
     * value 标签程序实际使用名
     */
    public static final Map<String, String> TOKEN_BY_RUN_ALIAS_CONFIG_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * key  标签编号
     * value  k线排序排序
     */
    public static final Map<String, Integer> TOKEN_BY_SORT_NUMBER_CONFIG_INFO_MAP = new ConcurrentHashMap<>();
    /**
     * key  标签编号
     * value  表格显示排序
     */
    public static final Map<String, Integer> TOKEN_BY_SORT_NUMBER2_CONFIG_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * key  程序使用的别名
     * value  名称
     */
    public static final Map<String, String> TOKEN_BY_VALUE_CONFIG_INFO_MAP = new ConcurrentHashMap<>();

    public final static List<Map<String, String>> DATA_CONFIG = new ArrayList<>();
    public final static String[] ARRAY_PATHS = {
            "Nasdaq 100-预估市盈率.zip",
            "MSCI韩国指数-市盈率.zip",
            "英国-FTSE 100.zip",
            "LME-铜期货价格.zip"
    };
    public static final String PATH = "/home/tomcat/us_treasury_bonds/";

    static {
        DATA_CONFIG.add(createDataEntry("baseRate_", PATH + "美国-基准利率.zip"));
        DATA_CONFIG.add(createDataEntry("usTreasuryBonds_", PATH + "美国-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("cnyUsysVsCnh_", PATH + "中国-中美利差vs.人民币(离岸).zip"));
        DATA_CONFIG.add(createDataEntry("cNYLoanQuoteRate_", PATH + "中国-贷款市场报价利率.zip"));
        DATA_CONFIG.add(createDataEntry("jPNBaseRateVs10YYield_", PATH + "日本-基准利率vs.十年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("japan30yBondYieldVsUsd_", PATH + "日本-30年期国债利率vs.美元.zip"));
        DATA_CONFIG.add(createDataEntry("top10CountriesHoldingUsDebt_", PATH + "全球-前十大国家持有美债金额.zip"));
        DATA_CONFIG.add(createDataEntry("nasdaqVsSp500VsCsi300_", PATH + "纳斯达克VS标普500VS沪深300.zip"));
        DATA_CONFIG.add(createDataEntry("usdToJpy_", PATH + "美元_日圆.zip"));
        DATA_CONFIG.add(createDataEntry("usCpi_", PATH + "美国-消费者物价指数[CPI].zip"));
        DATA_CONFIG.add(createDataEntry("usFedBalanceSheet_", PATH + "美国-美联储资产负债表规模.zip"));
        DATA_CONFIG.add(createDataEntry("usFedTreasuryHoldings_", PATH + "美国-美联储持有美债金额.zip"));
        DATA_CONFIG.add(createDataEntry("benchmarkInterestRate_", PATH + "美国国债收益率 vs. 基准利率.zip"));
        DATA_CONFIG.add(createDataEntry("usTreasuryBondIssuance", PATH + "美国-财政部每月债券发行量.zip"));
        DATA_CONFIG.add(createDataEntry("sp500SectorPe", PATH + "美国-S&P 500-各类股-预估市盈率.zip"));
        DATA_CONFIG.add(createDataEntry("m2MoneySupply", PATH + "美国-M1和M2.zip"));
        DATA_CONFIG.add(createDataEntry("mMuSIndex", PATH + "MM美股基本面指数.zip"));
        DATA_CONFIG.add(createDataEntry("cN30YbondYield", PATH + "中国-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("mealFuturesChart", PATH + "中美豆粕期货价格对比走势图.zip"));
        DATA_CONFIG.add(createDataEntry("uk30YearBondYield", PATH + "英国-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("id30YearBondYield", PATH + "印尼-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("india30YearBondYield", PATH + "印度-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("indiaIndex", PATH + "印度 SENSEX 指数 vs. FPI [股票].zip"));
        DATA_CONFIG.add(createDataEntry("tan", PATH + "糖.zip"));
        DATA_CONFIG.add(createDataEntry("shcIndex", PATH + "上证指数.zip"));
        DATA_CONFIG.add(createDataEntry("japan", PATH + "日本-日圆vs.日经225指数.zip"));
        DATA_CONFIG.add(createDataEntry("oDguo", PATH + "欧元 vs.德国股市.zip"));
        DATA_CONFIG.add(createDataEntry("usBondYieldSpread", PATH + "美国与各国 10 年期国债利差.zip"));
        DATA_CONFIG.add(createDataEntry("ca30yrBondYield", PATH + "加拿大-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("gold", PATH + "黄金.zip"));
        DATA_CONFIG.add(createDataEntry("soybeanVsBrazilStocks", PATH + "黄豆价格vs.巴西股市.zip"));
        DATA_CONFIG.add(createDataEntry("fr30yrBondYield", PATH + "法国-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("franceCac40", PATH + "法国 CAC 40.zip"));
        DATA_CONFIG.add(createDataEntry("brentOilPrice", PATH + "布兰特原油.zip"));
        DATA_CONFIG.add(createDataEntry("silver", PATH + "白银.zip"));
        DATA_CONFIG.add(createDataEntry("aus30yrBondYield", PATH + "澳洲-30年期国债收益率.zip"));
        DATA_CONFIG.add(createDataEntry("iceCoffeeFutures", PATH + "ICE-咖啡期货价格.zip"));
        DATA_CONFIG.add(createDataEntry("hShareIndex", PATH + "Ｈ股指数.zip"));
        DATA_CONFIG.add(createDataEntry("dJvsSpx500vsNas100", PATH + "DJ vs SPX500 vs NAS100.zip"));
        DATA_CONFIG.add(createDataEntry("cbotCornFutures", PATH + "CBOT-玉米期货价格.zip"));
        DATA_CONFIG.add(createDataEntry("btcPrice_", PATH + "BTC价格数据.zip"));
    }


    private static Map<String, String> createDataEntry(String key, String path) {
        Map<String, String> entry = new HashMap<>();
        entry.put("key", key);
        entry.put("path", path);
        return entry;
    }

}
