package com.ailu.exchange.utils.web;

import lombok.Data;

/**
 * Created by 郭青枫 on 2017/11/24.
 */
@Data
public class ResultDialogEntity {
	
    String type;// 0.confirm 1.alert 2.toast 3.不弹
    String title;//标题
    String content;//内容
    String cancelBtn;//取消文字 confirm
    String confirmBtn;//确认文字 confirm alert 公用一个
    String cancelColor;//取消颜色
    String confirmColor;//确认颜色
    String titleColor;//标题颜色
    String contentColor;//内容颜色
    boolean isCancelAndClose;//取消后是否关闭页面
    String code;//弹窗的逻辑码
    String url;//跳转到网页的链接
    String time; //倒计时
    
}
