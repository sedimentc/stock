package com.ailu.exchange.task;

import com.ailu.exchange.dao.domaim.AddressTxMonitor;
import com.ailu.exchange.dao.mapper.source1.AddressTxMonitorMapper;
import com.ailu.exchange.rest.api.OkLinkApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TxMonitorTask {

    @Resource
    OkLinkApi okLinkApi;

    @Value("${oklink.apikey}")
    private String apikey;

    @Resource
    AddressTxMonitorMapper addressTxMonitorMapper;


    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 30 * 60 * 1000L)
    public void syncAddressTxMonitorInfo() {
        long timeMillis = System.currentTimeMillis();
        log.info("开始处理AddressTxMonitor定时处理:{}", timeMillis);
        AddressTxMonitor latestData = addressTxMonitorMapper.getLatestData();
        String txId = "";
        if (latestData != null) {
            txId = latestData.getTxId();
        }
        getTokenTransactionList("base", "0x86CbAC9d9Ac726F729eEf6627Dc4817BcBB03A9c", txId);
        log.info("结束处理AddressTxMonitor定时处理:{}", System.currentTimeMillis() - timeMillis);
    }


    private static final int BATCH_SIZE = 500; // 每批插入500条

    private void getTokenTransactionList(String chainName, String address, String txIdNew) {
        List<AddressTxMonitor> list = new ArrayList<>();
        try {
            int currentPage = 1; // 初始页数
            int pageSize = 100; // 每页条数
            boolean shouldContinue = true; // 控制循环的标志
            while (shouldContinue) {
                Thread.sleep(1000L); // 避免请求过快
                JSONObject body = okLinkApi.getTokenTransactionList(apikey, chainName, address, "token_20",
                        "0x0b3e328455c4059eeb9e3f84b5543f74e24e7e1b", null, String.valueOf(currentPage),
                        String.valueOf(pageSize)).execute().body();

                if (body != null && body.getInteger("code").equals(0)) {
                    JSONObject jsonObject = body.getJSONArray("data").getJSONObject(0);
                    JSONArray tokenList = jsonObject.getJSONArray("transactionList");
                    // 遍历当前页的交易记录
                    for (Object o : tokenList) {
                        JSONObject transaction = JSONObject.parseObject(JSON.toJSONString(o));
                        String from = transaction.getString("from");
                        String to = transaction.getString("to");
                        String txId = transaction.getString("txId");
                        BigDecimal amount = transaction.getBigDecimal("amount");
                        Date transactionTime = transaction.getDate("transactionTime");
                        Boolean isFromContract = transaction.getBoolean("isFromContract");
                        //查询到上次最新的数据后跳出循环
                        if (StringUtils.isNotEmpty(txIdNew) && txId.equalsIgnoreCase(txIdNew)) {
                            shouldContinue = false; // 停止查询
                            break;
                        }
                        int isContract = 0;
                        if (isFromContract) {
                            isContract = 1;
                        }
                        if (!to.equalsIgnoreCase(address)) {
                            amount = amount.negate();
                        }
                        list.add(new AddressTxMonitor(from, to, amount, transactionTime, txId, isContract));
                    }
                    // 判断是否继续
                    if (tokenList.size() < pageSize) {
                        // 当前返回条数小于 100，跳出循环
                        shouldContinue = false;
                    } else {
                        currentPage++; // 获取下一页
                    }
                } else {
                    log.warn("未知错误 address: {}, Page: {},{}", address, currentPage, body);
                    shouldContinue = false; // 停止查询
                }
            }
        } catch (Exception e) {
            log.error(" 循环出现异常！！ address: {}", address, e);
        }

        if (list.size() > 0) {
            log.info("AddressTxMonitor方法添加数量:{}", list.size());
            batchInsert(list);
        }
    }

    private void batchInsert(List<AddressTxMonitor> list) {
        int totalSize = list.size();
        int batchCount = (totalSize + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int i = 0; i < batchCount; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min((i + 1) * BATCH_SIZE, totalSize);
            List<AddressTxMonitor> batchList = list.subList(start, end);
            addressTxMonitorMapper.insertList(batchList);  // 执行批量插入
        }
    }
}
