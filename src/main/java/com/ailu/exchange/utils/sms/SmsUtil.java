package com.ailu.exchange.utils.sms;

import com.ailu.paas.AiluPaasNotice;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author HYK
 * @Company 河南艾鹿
 * @Date 2018/1/10 0010 15:18
 */
@Slf4j
@Configuration
public class SmsUtil {

    @Value("${application.sms.appKey}")
    private String appKey;
    @Value("${application.sms.secret}")
    private String secret;

    @Value("${application.sms.template-id.sms-id_1}")
    private String msmTemplateId;
    @Value("${application.sms.template-id.phone-id_1}")
    private String phoneTemplateId;

    public AiluPaasNotice getSms() {
        //log.info("========={},{}",appKey,secret);
        return AiluPaasNotice.getAiluPaas(appKey, secret);
    }


    /**
     * 发送手机验证码
     *
     * @param phone 手机号
     * @return
     */
    public JSONObject sendPhoneCode(String phone) throws Exception {
        return getSms().smsSendValidCode(phone);
    }

    /**
     * 检查手机验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否验证成功
     */
    public boolean checkPhoneCode(String phone, String code) {
        try {
            JSONObject jsonObject = getSms().smsCheckValidCode(phone, code);
            return Objects.equals(0, jsonObject.getInteger("code")) ? true : false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 发送微信消息
     *
     * @param openId
     * @param templetId
     * @param content
     * @return boolean
     * @date 2018/5/17
     * @author hgl
     * @see
     * @since
     */
    public boolean sendWeChatPriceRemind(String openId, String templetId, String content) {

        log.info("未实现微信消息发送");
        return false;
    }

    /**
     * 发送价格提醒短信
     * 注意短信内容必须使用固定模板，如下
     * 模板：您好，当前%s的%s/%s价格为%s%s,已%s于您设置的%s%s。
     * 示例：您好，当前Huobi的BTC/USDT价格为8000美元,已超过于您设置的8000美元。
     *
     * @param phone   电话号
     * @param content 短信内容
     * @return boolean
     * @date 2018/5/17
     * @author hgl
     * @see
     * @since
     */
    public boolean sendSmsPriceRemind(String phone, String content) {
        try {
            JSONObject jsonObject = getSms().smsSendWarningMsg(phone, content, 2);
            return (jsonObject != null && Objects.equals(0, jsonObject.getInteger("code"))) ? true : false;
        } catch (Exception e) {
            log.info("发送短信异常：", e);
            return false;
        }
    }

    /**
     * 发送电话提醒
     * 注意 电话内容 最好和短信内容使用同一模板
     *
     * @param phone    电话号
     * @param content  内容
     * @param nickName 被提醒人昵称
     * @return boolean
     * @date 2018/5/17
     * @author hgl
     * @see
     * @since
     */
    public boolean sendPhoneData(String phone, String content, String nickName) {
        try {
            log.info("【电话】================phone:{}，content:{}", phone, content);
            JSONObject jsonObject = getSms().smsSendVoiceWarning(phone, content, nickName);
            System.out.println(jsonObject);
            return (jsonObject != null && Objects.equals(0, jsonObject.getInteger("code"))) ? true : false;
        } catch (Exception e) {
            log.error("发送电话提醒异常：", e);
            return false;
        }
    }

}
