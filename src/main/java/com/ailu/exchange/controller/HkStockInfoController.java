package com.ailu.exchange.controller;


import com.ailu.exchange.dao.domaim.ConditionLog;
import com.ailu.exchange.dao.domaim.StockInfo;
import com.ailu.exchange.dao.mapper.source1.HkConditionLogMapper;
import com.ailu.exchange.dao.mapper.source1.HkStockInfoMapper;
import com.ailu.exchange.service.HkStockInfoService;
import com.ailu.exchange.utils.web.WebResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;


@RestController

@RequestMapping("/hkStockInfo")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class HkStockInfoController {
    @Resource
    HkStockInfoService hkStockInfoService;

    @Resource
    HkStockInfoMapper hkStockInfoMapper;
    @Resource
    HkConditionLogMapper hkConditionLogMapper;

    @PostMapping("/getPagedStocksInfo")
    public Object getPagedStocks(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("请求参数不能为空");
            }
            Integer pageNumber = data.getInteger("pageNumber");
            Integer pageSize = data.getInteger("pageSize");
            String orderByFields = data.getString("orderByFields");
            String sortDirection = data.getString("sortDirection");
            JSONArray filterList = data.getJSONArray("filterList");
            if (pageNumber == null) {
                pageNumber = 1;
            }
            if (pageSize == null) {
                pageSize = 500;
            }
            JSONObject pagedAndSortedStocks = hkStockInfoService.getPagedAndSortedStocks(pageNumber, pageSize, orderByFields,
                    sortDirection, filterList);
            return WebResult.okResult(pagedAndSortedStocks);
        } catch (Exception e) {
            log.error("获取股票信息信息出现异常！！", e);
            return WebResult.failResult("error");
        }
    }


    @PostMapping("/updateTargetPriceAndNote")
    public Object updateTargetPriceAndNote(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空");
            }
            Integer id = data.getInteger("id");
            Set<String> strings = data.keySet();
            log.info("HkUpdateTargetPriceAndNote方法请求参数:{}", data.toJSONString());
            for (String name : strings) {
                if ("id".equalsIgnoreCase(name)) {
                    continue;
                }
                updateTargetPriceAndNoteIf(name, id, data.getString(name));
            }
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求修改updateTargetPriceAndNote出现异常！！", e);
            return WebResult.failResult("保存失败");
        }
    }

    @PostMapping("/updateScores")
    public Object updateScores(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空", "提交数据不能为空");
            }
            String tickerSymbol = data.getString("tickerSymbol");
            Set<String> strings = data.keySet();
            log.info("HkUpdateScores方法请求参数:{}", data.toJSONString());
            for (String name : strings) {
                if ("tickerSymbol".equalsIgnoreCase(name)) {
                    continue;
                }
                updateScoresIf(name, tickerSymbol, data.getBigDecimal(name));
            }
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求修改updateScores出现异常！！请求数据:{}", data, e);
            return WebResult.failResult("保存失败");
        }
    }


    @PostMapping("/saveConditionInfo")
    public Object saveConditionInfo(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空");
            }
            JSONArray dataString = data.getJSONArray("data");
            if (dataString != null && dataString.size() > 0) {
                hkConditionLogMapper.addConditionLog(new ConditionLog(dataString.toJSONString(), System.currentTimeMillis()));
            }
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求修改updateTargetPriceAndNote出现异常！！", e);
            return WebResult.failResult("保存失败");
        }
    }

    @GetMapping("/getRecentCondition")
    public Object getLastCondition() {
        try {
            JSONArray conditionLog = hkStockInfoService.getConditionLogKeyRedis();
            return WebResult.okResult(conditionLog);
        } catch (Exception e) {
            log.error("请求修改getLastCondition出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getConditionLog")
    public Object getConditionLog() {
        try {
            List<ConditionLog> conditionLog = hkConditionLogMapper.getConditionLogNew();
            return WebResult.okResult(conditionLog);
        } catch (Exception e) {
            log.error("请求修改getConditionLog出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/delConditionLog")
    public Object delConditionLog(Integer id) {
        try {
            if (id == null) {
                return WebResult.failResult("提交数据不能为空");
            }
            hkConditionLogMapper.delConditionLog(id);
            return WebResult.okResult("删除成功");
        } catch (Exception e) {
            log.error("请求修改delConditionLog出现异常！！", e);
            return WebResult.failResult("删除成功");
        }
    }


    private void updateScoresIf(String paramName, String key, BigDecimal val) {
        if ("performanceGrowthScore".equals(paramName)) {
            hkStockInfoMapper.updateByPerformanceGrowthScore(key, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getTickerSymbol().equals(key)) {
                        stockInfo.setPerformanceGrowthScore(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }

        } else if ("overallScore".equals(paramName)) {
            hkStockInfoMapper.updateByOverallScore(key, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getTickerSymbol().equals(key)) {
                        stockInfo.setOverallScore(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }
        } else if ("liudaScore".equals(paramName)) {
            hkStockInfoMapper.updateByLiudaScore(key, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getTickerSymbol().equals(key)) {
                        stockInfo.setLiudaScore(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }
        } else {
            log.error("updateScoresIf方法,未知参数:{},{},{}", paramName, key, val);
        }
    }

    private void updateTargetPriceAndNoteIf(String paramName, Integer id, String val) {
        if ("targetPrice1".equals(paramName)) {
            hkStockInfoMapper.updateByPrimaryTargetPrice(id, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getId().equals(id)) {
                        stockInfo.setTargetPrice1(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }

        } else if ("targetPrice2".equals(paramName)) {
            hkStockInfoMapper.updateBySecondaryTargetPrice(id, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getId().equals(id)) {
                        stockInfo.setTargetPrice2(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }

        } else if ("note1".equals(paramName)) {
            hkStockInfoMapper.updateByNote1(id, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getId().equals(id)) {
                        stockInfo.setNote1(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }
        } else if ("note2".equals(paramName)) {
            hkStockInfoMapper.updateByNote2(id, val);
            synchronized (HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE) {
                // 查找并替换缓存中的旧数据
                for (int i = 0; i < HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.size(); i++) {
                    StockInfo stockInfo = HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.get(i);
                    if (stockInfo.getId().equals(id)) {
                        stockInfo.setNote2(val);
                        HkStockInfoService.STOCK_INFO_LIST_GLOBAL_CACHE.set(i, stockInfo);
                        break;
                    }
                }
            }
        } else {
            log.error("updateTargetPriceAndNote接口,未知参数:{},{},{}", paramName, id, val);
        }
    }
}
