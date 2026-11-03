package com.ailu.exchange.controller;

import com.ailu.exchange.dao.mapper.source1.StockMultiRatingMapper;
import com.ailu.exchange.utils.web.WebResult;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/stockInfo/multiRating")
public class StockMultiRatingController {
    private static final Set<String> NAMES = new HashSet<>(Arrays.asList("小雷", "小梦", "肖明", "陈纪言"));
    @Resource private StockMultiRatingMapper mapper;

    @PostMapping("/list")
    public Object list() {
        try { return WebResult.okResult(mapper.findAll()); }
        catch (Exception e) { return WebResult.failResult("评分读取失败"); }
    }

    @PostMapping("/save")
    public Object save(@RequestBody JSONObject data) {
        try {
            if (data == null) return WebResult.failResult("请求不能为空");
            String ticker = data.getString("tickerSymbol");
            String name = data.getString("raterName");
            if (ticker == null || !ticker.matches("[A-Za-z0-9.]{1,32}") || !NAMES.contains(name)) return WebResult.failResult("股票代码或评分人无效");
            BigDecimal overall = data.getBigDecimal("overallScore");
            BigDecimal potential = data.getBigDecimal("potentialScore");
            if (!valid(overall) || !valid(potential)) return WebResult.failResult("评分必须为0到10，最多一位小数");
            if (overall == null && potential == null) mapper.delete(ticker, name);
            else mapper.save(ticker, name, overall, potential);
            return WebResult.okResult("保存成功");
        } catch (Exception e) { return WebResult.failResult("保存失败，请稍后重试"); }
    }
    private boolean valid(BigDecimal value) {
        return value == null || (value.signum() >= 0 && value.compareTo(BigDecimal.TEN) <= 0 && value.stripTrailingZeros().scale() <= 1);
    }
}
