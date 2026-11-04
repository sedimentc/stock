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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stockInfo/multiRating")
public class StockMultiRatingController {
    private static final Set<String> NAMES = new HashSet<>(Arrays.asList("小雷", "小梦", "肖明", "陈纪言", "罗丰越", "杜斌"));
    @Resource private StockMultiRatingMapper mapper;

    @RequestMapping(value = "/list", method = {RequestMethod.GET, RequestMethod.POST})
    public Object list() {
        try { return WebResult.okResult(mapper.findAll()); }
        catch (Exception e) { return WebResult.failResult("评分读取失败"); }
    }

    @RequestMapping(value = "/save", method = {RequestMethod.GET, RequestMethod.POST})
    public Object save(@RequestBody(required = false) JSONObject data,
                       @RequestParam(value = "tickerSymbol", required = false) String tickerSymbol,
                       @RequestParam(value = "raterName", required = false) String raterName,
                       @RequestParam(value = "overallScore", required = false) String overallScore,
                       @RequestParam(value = "potentialScore", required = false) String potentialScore) {
        try {
            String ticker = data != null ? data.getString("tickerSymbol") : tickerSymbol;
            String name = data != null ? data.getString("raterName") : raterName;
            BigDecimal overall = data != null ? data.getBigDecimal("overallScore") : parseScore(overallScore);
            BigDecimal potential = data != null ? data.getBigDecimal("potentialScore") : parseScore(potentialScore);
            if (ticker == null || !ticker.matches("[A-Za-z0-9.]{1,32}") || !NAMES.contains(name)) return WebResult.failResult("股票代码或评分人无效");
            if (!valid(overall) || !valid(potential)) return WebResult.failResult("评分必须为0到10，最多一位小数");
            if (overall == null && potential == null) mapper.delete(ticker, name);
            else mapper.save(ticker, name, overall, potential);
            return WebResult.okResult("保存成功");
        } catch (Exception e) { return WebResult.failResult("保存失败，请稍后重试"); }
    }

    private static BigDecimal parseScore(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return null;
        return new BigDecimal(trimmed);
    }

    private boolean valid(BigDecimal value) {
        return value == null || (value.signum() >= 0 && value.compareTo(BigDecimal.TEN) <= 0 && value.stripTrailingZeros().scale() <= 1);
    }
}
