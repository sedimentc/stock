package com.ailu.exchange.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WarnUtil {
    public static void sendMsgToFeyShu(String content) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg_type", "text");
            JSONObject contentJson = new JSONObject();
            contentJson.put("text", "数量变动机器人:" + content);
            jsonObject.put("content", contentJson);
            String url = "https://open.feishu.cn/open-apis/bot/v2/hook/12c7fdc4-a122-43b6-8e64-bb05b615dcc2";
            JSONObject backData = OkHttpUtil.doPost(url, jsonObject);
            log.info("发送飞书消息====={}======{}", content, backData != null ? JSONObject.toJSONString(backData) : "无返回数据");
        } catch (Exception e) {
            log.error("飞书发送消息失败", e);
        }
    }

    public static void main(String[] args) {
        sendMsgToFeyShu("测试推送");
    }

}
