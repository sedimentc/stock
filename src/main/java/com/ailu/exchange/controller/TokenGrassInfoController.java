package com.ailu.exchange.controller;

import com.ailu.exchange.dao.domaim.TokenGrassConfig;
import com.ailu.exchange.service.TokenGrassConfigService;
import com.ailu.exchange.service.TokenGrassInfoService;
import com.ailu.exchange.service.TokenMainInfoService;
import com.ailu.exchange.utils.web.WebResult;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/grassInfo")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TokenGrassInfoController {
    @Resource
    TokenGrassInfoService tokenGrassInfoService;

    @GetMapping("/getTokenGrassKlineData")
    public Object getTokenGrassKlineData(String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            return WebResult.failResult("提交数据不能为空");
        }
        try {
            JSONObject tokenGrassKlineData = tokenGrassInfoService.getTokenGrassKlineData(contractId);
            return WebResult.okResult(tokenGrassKlineData);
        } catch (Exception e) {
            log.error("请求getTokenGrassKlineData出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/delTokenGrassRemind")
    public Object delTokenGrassRemind(Integer id) {
        try {
            tokenGrassInfoService.delTokenGrassRemind(id);
            return WebResult.okResult();
        } catch (Exception e) {
            log.error("请求delTokenGrassRemind出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/reviseRemind")
    public Object reviseRemind(String remind, String contractId) {
        try {
            tokenGrassInfoService.reviseRemind(remind, contractId);
            return WebResult.okResult();
        } catch (Exception e) {
            log.error("请求reviseRemind出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getTokenGrassRemind")
    public Object getTokenGrassRemind(String contractId) {
        try {
            JSONObject tokenGrassRemind = tokenGrassInfoService.getTokenGrassRemind(contractId);
            return WebResult.okResult(tokenGrassRemind);
        } catch (Exception e) {
            log.error("请求getTokenGrassRemind出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getExchangeList")
    public Object getExchangeList() {
        try {
            List<String> exchangeList = tokenGrassInfoService.getExchangeList();
            return WebResult.okResult(exchangeList);
        } catch (Exception e) {
            log.error("请求getExchangeList出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getTokenGrassKlineVoList")
    public Object getTokenGrassKlineVoList(String contractId) {
        try {
            JSONObject tokenGrassKlineVoList = tokenGrassInfoService.getTokenGrassKlineVoList(contractId);
            return WebResult.okResult(tokenGrassKlineVoList);
        } catch (Exception e) {
            log.error("请求getTokenGrassKlineVoList出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getChangeDataInfo")
    public Object getChangeDataInfo(String contractId) {
        try {
            JSONObject changeDataInfo = tokenGrassInfoService.getChangeDataInfo(contractId);
            return WebResult.okResult(changeDataInfo);
        } catch (Exception e) {
            log.error("请求getChangeDataInfo出现异常！！", e);
            return WebResult.failResult("error");
        }
    }


    @PostMapping("/addTokenGrassRemind")
    public Object addTokenGrassRemind(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空", "提交数据不能为空");
            }
            log.info("addTokenGrassRemind方法请求参数:{}", data.toJSONString());
            tokenGrassInfoService.addTokenGrassRemind(data);
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求addTokenGrassRemind出现异常！！请求数据:{}", data, e);
            return WebResult.failResult("保存失败");
        }
    }

    /****
     * 关于 下拉分类相关的分类分类显示的名称和对应的值
     */
    @Resource
    TokenGrassConfigService tokenGrassConfigService;

    @PostMapping("/addTokenGrassConfig")
    public Object addTokenGrassConfig(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空", "提交数据不能为空");
            }
            List<TokenGrassConfig> tokenGrassConfigs = data.getJSONArray("data").toJavaList(TokenGrassConfig.class);
            tokenGrassConfigService.addTokenGrassConfig(tokenGrassConfigs);
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求addTokenGrassConfig出现异常！！请求数据:{}", data, e);
            return WebResult.failResult("保存失败");
        }
    }


    @GetMapping("/getTokenGrassConfigList")
    public Object getTokenGrassConfigList() {
        try {
            List<TokenGrassConfig> tokenGrassConfig = tokenGrassConfigService.getTokenGrassConfig();
            return WebResult.okResult(tokenGrassConfig);
        } catch (Exception e) {
            log.error("请求getTokenGrassConfigList出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    /****
     *
     * 添加合约id相关地址代码
     */
    @Resource
    TokenMainInfoService tokenMainInfoService;

    @PostMapping("/addTokenMainInfo")
    public Object addTokenMainInfo(@RequestBody JSONObject data) {
        try {
            if (data == null) {
                return WebResult.failResult("提交数据不能为空", "提交数据不能为空");
            }
            tokenMainInfoService.addTokenMainInfo(data);
            return WebResult.okResult("保存成功");
        } catch (Exception e) {
            log.error("请求addTokenMainInfo出现异常！！请求数据:{}", data, e);
            return WebResult.failResult("保存失败");
        }
    }


    @GetMapping("/getTokenMainInfoList")
    public Object getTokenMainInfoList(String contractId) {
        try {
            JSONObject tokenMainInfoList = tokenMainInfoService.getTokenMainInfoList(contractId);
            return WebResult.okResult(tokenMainInfoList);
        } catch (Exception e) {
            log.error("请求getTokenMainInfoList出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/getContractIds")
    public Object getContractIds() {
        try {
            List<String> contractIds = tokenMainInfoService.getContractIds();
            return WebResult.okResult(contractIds);
        } catch (Exception e) {
            log.error("请求getContractIds出现异常！！", e);
            return WebResult.failResult("error");
        }
    }

    @GetMapping("/delTokenMainInfo")
    public Object getContractIds(Integer id) {
        try {
            tokenMainInfoService.delTokenMainInfo(id);
            return WebResult.okResult();
        } catch (Exception e) {
            log.error("请求delTokenMainInfo出现异常！！", e);
            return WebResult.failResult("error");
        }
    }







}
