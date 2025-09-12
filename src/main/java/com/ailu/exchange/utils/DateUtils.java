package com.ailu.exchange.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DateUtils {

    //日期常量
    enum DateConstants {
        TODAY(0), NEARLYWEEK(1), MONTH(2), NEARLYMONTH(3);
        public int value;

        DateConstants(int value) {
            this.value = value;
        }
    }

    /**
     * 显示日期的格式,yyyy-MM-dd
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat DATE_FORMATNew = new SimpleDateFormat("yyyyMMdd");
    /**
     * 显示日期的格式,yyyy-MM-dd
     */
    public static final String DATE_HOUR_FORMAT = "yyyy-MM-dd HH:mm";

    public static final String DATE_HOUR_FORMAT2 = "yyyy/MM/dd HH:mm";

    /**
     * 显示日期的格式,yyyy-MM-ddTHH:mm:ss.SSSZ
     */
    public static final String FULL_TIME_FORMAT_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * 显示日期的格式,yyyy-MM-ddTHH:mm:ssZ
     */
    public static final String FULL_TIME_OKEX_FORMAT_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * 显示日期的格式,yyyy-MM
     */
    public static final String DATE_YEAE_MONTH = "yyyy-MM";

    /**
     * 显示日期的格式,yyyy-MM-dd HH:mm:ss
     */
    public static final String TIMEF_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 显示日期的格式,yyyy-MM-dd HH:mm:ss.SSS
     */
    public static final String FULL_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 显示日期的格式,yyyy年MM月dd日HH时mm分ss秒
     */
    public static final String ZHCN_TIME_FORMAT = "yyyy年MM月dd日HH时mm分ss秒";
    /**
     * 显示日期的格式,yyyy年MM月dd日HH时mm分
     */
    public static final String ZHCN_TIME_FORMAT1 = "yyyy年MM月dd日HH时mm分";

    /**
     * 中文日期  年月日
     */
    public static final String ZHCN_DAY_FORMAT = "yyyy年MM月dd日";

    /**
     * 显示日期的格式,yyyyMMddHHmmss
     */
    public static final String TIME_STR_FORMAT = "yyyyMMddHHmmss";
    /**
     * 显示日期的格式,yyyyMMddHHmmssSSS
     */
    public static final String TIMESSS_STR_FORMAT = "yyyyMMddHHmmssSSS";
    /**
     * 显示日期的格式,yyyyMMdd
     */
    public static final String DATE_STR_FORMAT = "yyyyMMdd";

    /**
     * 显示日期的格式,yyMMdd
     */
    public static final String DATE_SIMPLE_SHORT_FORMAT = "yyMMdd";

    /**
     * DateFormat,格式:yyyy-MM-dd
     */
    private static DateFormat dateFormat;

    /**
     * DateFormat,格式:yyyy-MM-dd HH:mm:ss
     */
    private static DateFormat dateTimeFormat;

    /**
     * DateFormat,格式:yyyyMMddHHmmss
     */
    private static DateFormat dateTimeStrFormat;

    /**
     * DateFormat,格式:yyyy年MM月dd日HH时mm分ss秒
     */
    private static DateFormat zhcnDateTimeStrFormat;

    static {
        dateFormat = getInstance(DATE_FORMAT);
        dateTimeFormat = getInstance(TIMEF_FORMAT);
        dateTimeStrFormat = getInstance(TIME_STR_FORMAT);
        zhcnDateTimeStrFormat = getInstance(ZHCN_TIME_FORMAT);
    }

    public static DateFormat getInstance(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format;
    }

    private static final Integer sixtySec = 60;


    public static Date getDate(Date date) {
        DateFormat df = getDateFormat(DATE_HOUR_FORMAT);
        Date date1 = null;
        try {
            date1 = convertStringToDate(df.format(date));
        } catch (Exception e) {
            log.error("处理时间格式出现异常！！", e);
        }
        return date1;
    }

    /**
     * 获取剩余秒数
     *
     * @return
     */
    public static Integer getSurplusSeconds() {
        //获取到当前的秒数
        Integer residueSec = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ss");
            Calendar beforeTime = Calendar.getInstance();
            Date beforeD = beforeTime.getTime();
            String time = sdf.format(beforeD);
            //得到剩余的秒数
            residueSec = sixtySec - Integer.valueOf(time);
        } catch (NumberFormatException e) {
            log.error("获取剩余秒数出现异常！！", e);
        }
        return residueSec;
    }


    /**
     * 自定义日期格式  日期转为字符串
     *
     * @param date  要转换的日期
     * @param fomat 日期格式 如: "yyyy-MM-dd HH:mm:ss"
     * @return
     * @date 2018/1/13
     * @author hgl
     * @see
     * @since
     */
    public static String getDateToStringByFormat(Date date, String fomat) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(fomat);
        return format.format(date);
    }

    /**
     * 日期转换为 剩余时间 (如: 10sec ago ; 12min ago)
     *
     * @return
     * @date 2018/1/15
     * @author hgl
     * @see
     * @since
     */
    public static String getDateToLeftTime(Date date) {
        String leftTime = "";
        if (date == null) {
            return leftTime;
        }

        String postfix = " ago";
        String sec = "sec";
        String min = "min";
        String hour = "hrs";
        String yesterday = "昨天";
        Date currentTime = new Date();
        long currentMs = System.currentTimeMillis();
        long dateMs = date.getTime();
        long letfSec = (currentMs - dateMs) / 1000;
        //不足一分钟
        if (letfSec < 60) {
            leftTime = letfSec + sec + postfix;
            return leftTime;
        }
        //不足一小时
        if (letfSec >= 60 && letfSec < 60 * 60) {

            leftTime = letfSec / 60 + min + postfix;
            return leftTime;
        }
        //不足一天
        if (letfSec >= 60 * 60 && letfSec < 60 * 60 * 24) {

            leftTime = letfSec / (60 * 60) + hour + postfix;
            return leftTime;
        }
        //大于一天
        if (letfSec >= 60 * 60 * 24) {
            leftTime = DateUtils.getDateToStringByFormat(date, DATE_HOUR_FORMAT2);
            return leftTime;
        }

        return leftTime;

    }

    /**
     * 获取当前时间在＋－n分钟后的字符串时间
     *
     * @param n int
     * @return String
     */
    public static String getTimebyMinAfter(int n) {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        now.add(Calendar.MINUTE, n);
        return (dateTimeFormat.format(now.getTime()));
    }

    /**
     * 获取当前时间在＋－n秒后的字符串时间
     *
     * @param n int
     * @return String
     */
    public static String getTimebySecAfter(int n) {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        now.add(Calendar.SECOND, n);
        return (dateTimeFormat.format(now.getTime()));
    }

    /**
     * 获取当前时间在＋－n分钟后的时间(java.util.Date)
     *
     * @param n int
     * @return String
     */
    public static Date getTimebyMinAfterDate(int n) {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        now.add(Calendar.MINUTE, n);
        return now.getTime();
    }

    /**
     * 获取定义的DateFormat格式
     *
     * @param formatStr
     * @return
     */
    private static DateFormat getDateFormat(String formatStr) {
        if (formatStr.equalsIgnoreCase(DATE_FORMAT)) {
            return dateFormat;
        } else if (formatStr.equalsIgnoreCase(TIMEF_FORMAT)) {
            return dateTimeFormat;
        } else {
            return getInstance(formatStr);
        }
    }

    /**
     * 将Date转换成字符串“yyyy-mm-dd hh:mm:ss”的字符串
     *
     * @param date 日期
     * @return String 字符串
     */
    public static String dateToDateString(Date date) {
        return dateToDateString(date, TIMEF_FORMAT);
    }

    /**
     * 将Date转换成formatStr格式的字符串
     *
     * @param date
     * @param formatStr
     * @return
     */
    public static String dateToDateString(Date date, String formatStr) {
        DateFormat df = getDateFormat(formatStr);
        return df.format(date);
    }

    /**
     * 将Date转换成yyyy-MM-dd的字符串
     *
     * @param date
     * @return
     */
    public static String getDateString(Date date) {
        DateFormat df = getDateFormat(DATE_FORMAT);
        return df.format(date);
    }

    /**
     * 将小时数换算成返回以毫秒为单位的时间
     *
     * @param hours
     * @return
     */
    public static long getMicroSec(BigDecimal hours) {
        BigDecimal bd;
        bd = hours.multiply(new BigDecimal(3600 * 1000));
        return bd.longValue();
    }

    /**
     * 获取今天的日期，格式自定
     *
     * @param pattern -
     *                设定显示格式
     * @return String - 返回今天的日期
     */
    public static String getToday(String pattern) {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        DateFormat sdf = getDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getDefault());
        return (sdf.format(now.getTime()));
    }

    // 得到系统当前的分钟数,如当前时间是上午12:00,系统当前的分钟数就是12*60
    public static int getCurrentMinutes() {
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        int iMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return iMin;
    }

    /**
     * 获取当前日期时间yyyy年MM月dd日HH时mm分ss秒的形式
     *
     * @return 当前日期时间yyyy年MM月dd日HH时mm分ss秒的形式
     */
    public static String getCurZhCNDateTime() {
        return dateToDateString(new Date(), ZHCN_TIME_FORMAT);
    }

    /**
     * 获取当前时间
     *
     * @return
     */
    public static Date getCurrentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * @return 得到本月月份 如09
     */
    public static String getMonth() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH) + 1;
        String monStr = String.valueOf(month);

        // 对于10月份以下的月份,加"0"在前面

        if (month < 10) {
            monStr = "0" + monStr;
        }
        return monStr;
    }

    /**
     * @return 得到本月第几天
     */
    public static String getDay() {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        String dayStr = String.valueOf(day);

        // 对于10月份以下的月份,加"0"在前面

        if (day < 10) {
            dayStr = "0" + dayStr;
        }
        return dayStr;
    }

    /**
     * @return 获取指定日期月份 如09
     */
    public static String getMonth(Date date) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        int month = now.get(Calendar.MONTH) + 1;
        String monStr = String.valueOf(month);
        // 对于10月份以下的月份,加"0"在前面
        if (month < 10) {
            monStr = "0" + monStr;
        }
        return monStr;
    }

    /**
     * @return 获取指定日期年 如2016
     */
    public static String getYear(Date date) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        int year = now.get(Calendar.YEAR);
        String monStr = String.valueOf(year);
        return monStr;
    }

    /**
     * @return 获取指定日期天数
     */
    public static String getDay(Date date) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        int day = now.get(Calendar.DAY_OF_MONTH);
        String dayStr = String.valueOf(day);
        // 对于10月份以下的月份,加"0"在前面
        if (day < 10) {
            dayStr = "0" + dayStr;
        }
        return dayStr;
    }

    /**
     * 根据失效时间判断是否依然有效
     *
     * @param expireTime 失效时间的字符串形式,格式要求:yyMMddHHmmss
     * @return true:失效 false:没有失效
     * @throws ParseException
     */
    public static boolean isTimeExpired(String expireTime) throws ParseException {
        boolean ret = false;

        // SimpleDateFormat不是线程安全的,所以每次调用new一个新的对象

        Date date = getInstance(TIME_STR_FORMAT).parse(expireTime);
        Calendar expire = Calendar.getInstance();
        expire.setTime(date);
        Calendar now = Calendar.getInstance();
        // 将毫秒字段设为0,保持精度一致性

        now.set(Calendar.MILLISECOND, 0);
        if (now.after(expire)) {
            ret = true;
        }
        return ret;
    }

    /**
     * 根据开始时间和可用时间计算出失效时间
     *
     * @param startTime     开始时间
     * @param availableTime 有效时长（单位：秒）
     * @return 失效时间
     * @throws ParseException
     */
    public static String getExpireTimeByCalculate(Date startTime, int availableTime) {

        Calendar expire = Calendar.getInstance();

        // 设置为开始时间

        expire.setTime(startTime);

        // 开始时间向后有效时长秒得到失效时间
        expire.add(Calendar.SECOND, availableTime);

        // 返回失效时间字符串

        // SimpleDateFormat不是线程安全的,所以每次调用new一个新的对象

        return getInstance(TIME_STR_FORMAT).format(expire.getTime());

    }

    /**
     * 将Date转换为yyyyMMddHHmmss的形式
     *
     * @param date
     * @return 日期的字符串形式, 格式：yyyyMMddHHmmss
     */
    public static String convertDateToString(Date date) {
        return getInstance(TIME_STR_FORMAT).format(date);
    }

    /**
     * 将Date转换为yyMMddHHmmss的形式
     *
     * @param date
     * @return 日期的字符串形式, 格式：yyMMddHHmmss
     */
    public static String convertDateToString(Date date, String format) {
        return getInstance(format).format(date);
    }

    /**
     * 将yyMMddHHmmss形式的字符串转换成Date的形式
     *
     * @param date yyMMddHHmmss形式的日期字符串
     * @return Date对象
     * @throws ParseException
     */
    public static Date convertStringToDate(String date) throws ParseException {
        return getInstance(TIME_STR_FORMAT).parse(date);
    }

    /**
     * 字符串转化为日期
     *
     * @param date         日期字符串
     * @param formatString 格式化字符串
     * @return
     * @throws ParseException
     */
    public static Date convertStringToDate(String date, String formatString) throws ParseException {
        return getInstance(formatString).parse(date);
    }

    /**
     * 日期转化为格式化日期
     *
     * @param date         日期
     * @param formatString 格式化字符串
     * @return
     * @throws ParseException
     */
    public static Date convertDateToDate(Date date, String formatString) throws ParseException {
        return getInstance(formatString).parse(convertDateToString(date, formatString));
    }

    /**
     * 字符串转化为格式化字符串格式
     *
     * @param date         日期
     * @param formatString 格式化字符串
     * @return
     * @throws ParseException
     */
    public static String convertStringToString(String date, String formatString) throws ParseException {
        return getInstance(formatString).format(date);
    }

    /**
     * 将yy-MM-dd形式的字符串转换成Date的形式
     *
     * @param date yy-MM-dd形式的日期字符串
     * @return Date对象
     * @throws ParseException
     */
    public static Date convertSimpleStringToDate(String date) throws ParseException {
        return getInstance(DATE_FORMAT).parse(date);
    }

    /**
     * @param date yyyyMMddHHmmss格式的日期字符转换为yyyy年MM月dd日HH时mm分ss秒格式的字符串
     * @return yyyy年MM月dd日HH时mm分ss秒格式的日期字符串
     * @throws ParseException
     */
    public static String convertStringToZhCN(String date) throws ParseException {
        return zhcnDateTimeStrFormat.format(dateTimeStrFormat.parse(date));
    }

    /**
     * 时间字符串转换成日期时间的形式
     *
     * @param date yy-MM-dd形式的日期字符串
     * @return Date对象
     * @throws ParseException
     */
    public static Long convertSimpleStringToDateTime(String date) {
        try {
            return getInstance(DATE_FORMAT).parse(date).getTime();
        } catch (ParseException e) {
            log.error("获取时间搓出现异常！！", e);
        }
        return 0L;
    }

    /**
     * 获取当天日期
     */
    public static Date getTodayDate() {
        // 获取昨日的日期
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        return today;
    }

    /**
     * 获取昨日日期 返回格式：yyyy-MM-dd
     */
    public static String getYesterdayDateStr() {
        // 获取昨日的日期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();
        return getInstance(DATE_FORMAT).format(yesterday);
    }

    /**
     * 获取昨日日期 返回格式：yyyy-MM-dd
     */
    public static Date getYesterdayDate() {
        // 获取昨日的日期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();
        return yesterday;
    }

    /**
     * 获取明天日期 返回格式：yyyy-MM-dd
     */
    public static String getTomorrowDateStr() {
        // 获取昨日的日期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = cal.getTime();
        return getInstance(DATE_FORMAT).format(tomorrow);
    }

    /**
     * 获取明天日期 返回格式：yyyy-MM-dd
     */
    public static Date getTomorrowDate() {
        // 获取昨日的日期

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date tomorrow = cal.getTime();
        return tomorrow;
    }

    /**
     * 根据Calendar对象、字符串日期类型获得字符串日期
     *
     * @param calendar    Calendar对象
     * @param strDateType 字符串日期类型（1：XXXX年XX月XX日，2：XX月XX日，3，XXXX年，4：XXXX-XX-XX，5：XX-XX，6：XXXX）
     * @return
     */
    public static String getStrDate(Calendar calendar, int strDateType) {
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = (calendar.get(Calendar.MONTH) + 1) < 10 ? "0" + (calendar.get(Calendar.MONTH) + 1) : String
                .valueOf

                        ((calendar.get(Calendar.MONTH) + 1));
        String day = calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + calendar.get(Calendar.DAY_OF_MONTH) : String
                .valueOf

                        (calendar.get(Calendar.DAY_OF_MONTH));
        String strDate = "";

        switch (strDateType) {
            case 1:
                strDate = year + "年" + month + "月" + day + "日";
                break;
            case 2:
                strDate = month + "月" + day + "日";
                break;
            case 3:
                strDate = year + "年";
                break;
            case 4:
                strDate = year + "-" + month + "-" + day;
                break;
            case 5:
                strDate = month + "-" + day;
                break;
            case 6:
                strDate = year;
                break;
            default:
                strDate = year + "-" + month + "-" + day;
                break;
        }

        return strDate;
    }

    /**
     * 根据原来的时间（Date）获得相对偏移 N 月的时间（Date）
     *
     * @param protoDate                原来的时间（java.util.Date）
     * @param monthOffset（向前移正数，向后移负数）
     * @return 时间（java.util.Date）
     */
    public static Date getOffsetMonthDate(Date protoDate, int monthOffset) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(protoDate);
        //		cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - monthOffset);错误写法
        cal.add(Calendar.MONTH, -monthOffset);
        return cal.getTime();
    }

    /**
     * 根据原来的时间（Date）获得相对偏移 N 天的时间（Date）
     *
     * @param protoDate               原来的时间（java.util.Date）
     * @param dateOffset（向前移正数，向后移负数）
     * @return 时间（java.util.Date）
     */
    public static Date getOffsetDayDate(Date protoDate, int dateOffset) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(protoDate);
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - dateOffset);
        return cal.getTime();
    }

    /**
     * 根据原来的时间（Date）获得相对偏移 N 小时的时间（Date）
     *
     * @param protoDate  原来的时间（java.util.Date）
     * @param offsetHour （向前移正数，向后移负数）
     * @return 时间（java.util.Date）
     */
    public static Date getOffsetHourDate(Date protoDate, int offsetHour) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(protoDate);
        cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY) - offsetHour);
        return cal.getTime();
    }

    /**
     * 获取指定月份和日子的日期(未做日期效验)
     *
     * @param currentDate 当前日期
     * @param assignYear  指定年份,-1代表年不做修改
     * @param assignMonth 指定月份,从0开始,超过月最大值自动往后加一个月年,-1代表月不做修改
     * @param assignDay   指定日,从1开始,超过日最大值往后加一个月,-1代表日不做修改
     * @return 修改后的日期
     */
    public static Date getAssignDate(Date currentDate, int assignYear, int assignMonth, int assignDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        if (assignYear > -1) {
            cal.set(Calendar.YEAR, assignYear);
        }
        if (assignMonth > -1) {
            cal.set(Calendar.MONTH, assignMonth);
        }
        if (assignDay > -1) {
            cal.set(Calendar.DAY_OF_MONTH, assignDay);
        }
        return cal.getTime();
    }

    /**
     * 获得两个日前之间相差的天数,有时分秒的影响
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 天数
     */
    public static int getDayCountBetweenDate(Date startDate, Date endDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        int i = 0;
        while (endCalendar.compareTo(startCalendar) >= 0) {
            startCalendar.set(Calendar.DAY_OF_MONTH, startCalendar.get(Calendar.DAY_OF_MONTH) + 1);
            i++;
        }
        return i;
    }

    /**
     * 获得两个日前之间相差的月份
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 月数
     */
    public static int getMonthCountBetweenDate(Date startDate, Date endDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        int i = 0;
        while (endCalendar.compareTo(startCalendar) >= 0) {
            startCalendar.set(Calendar.MONTH, startCalendar.get(Calendar.MONTH) + 1);
            i++;
        }
        return i;
    }


    /**
     * @return
     * @Description
     * @author HYK
     * @data 2017
     */
    public static String getNowDate(Date date) {
        return convertDateToString(date, TIMEF_FORMAT);
    }


    /**
     * 获得两个日前之间相差的年
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 年
     */
    public static int getYearlyCountBetweenDate(Date startDate, Date endDate) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);
        int i = 0;
        while (endCalendar.compareTo(startCalendar) >= 0) {
            startCalendar.set(Calendar.YEAR, startCalendar.get(Calendar.YEAR) + 1);
            i++;
        }
        return i;
    }

    /**
     * 根据原来的时间（Date）获得相对偏移 N 天的时间（Date）
     *
     * @param protoDate               原来的时间（java.util.Date）
     * @param dateOffset（向前移正数，向后移负数）
     * @param type                    指定不同的格式（1：年月日，2：年月日时，3：年月日时分）
     * @return 时间（java.util.Date），没有时分秒
     */
    public static Date getOffsetSimpleDate(Date protoDate, int dateOffset, int type) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(protoDate);
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - dateOffset);
        if (type == 1) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        }
        if (type == 2) {
            cal.set(Calendar.MINUTE, 0);
        }
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 时间转为化为字符串
     * <p>
     * 格式为：yyyyMMddHHmmssSSS
     *
     * @return
     */
    public static String getDateToString() {
        SimpleDateFormat dateFormat = (SimpleDateFormat) getInstance(TIMESSS_STR_FORMAT);
        Date date = new Date();
        String str = dateFormat.format(date);
        return str;
    }

    /**
     * 时间转为化为字符串
     * <p>
     * 格式为：yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getTodayTimeString() {
        SimpleDateFormat dateFormat = (SimpleDateFormat) getInstance(TIMEF_FORMAT);
        Date date = new Date();
        String str = dateFormat.format(date);
        return str;
    }

    /**
     * 增加一年
     *
     * @param s
     * @param n
     * @return
     */
    public static Date addYear(Date s, int n) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(s);
        cd.add(Calendar.YEAR, n);// 增加一天
        return cd.getTime();
    }


    /**
     * 增加一天
     *
     * @param s
     * @param n
     * @return
     */
    public static String addDay(String s, int n) {
        try {
            SimpleDateFormat sdf = (SimpleDateFormat) getInstance(DateUtils.DATE_FORMAT);
            Calendar cd = Calendar.getInstance();
            cd.setTime(sdf.parse(s));
            cd.add(Calendar.DATE, n);// 增加一天
            return sdf.format(cd.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 增加一天
     *
     * @param s
     * @param n
     * @return
     */
    public static Date addDay(Date s, int n) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(s);
        cd.add(Calendar.DATE, n);// 增加一天
        return cd.getTime();
    }

    /**
     * 增加一个月
     *
     * @param m
     * @param n
     * @return
     */
    public static Date addMonth(Date m, int n) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(m);
        cd.add(Calendar.MONTH, n);// 增加一个月
        return cd.getTime();
    }

    /**
     * 增加一个月
     *
     * @param m
     * @param n
     * @param formatstring
     * @return
     */
    public static String addMonth(Date m, int n, String formatstring) {
        try {
            SimpleDateFormat sdf = (SimpleDateFormat) getInstance(formatstring);
            Calendar cd = Calendar.getInstance();
            cd.setTime(m);
            cd.add(Calendar.MONTH, n);//增加一个月
            return sdf.format(cd.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 增加一分
     *
     * @param s
     * @param n
     * @return
     */
    public static Date addMinute(Date s, int n) {
        try {
            Calendar cd = Calendar.getInstance();
            cd.setTime(s);
            cd.add(Calendar.MINUTE, n);// 增加一天
            return cd.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取需要执行的统计的日期数组
     *
     * @return 格式['2011-01-01',2011-01-02']
     */
    public static String[] getExecDay(Date lastDate) {
        String[] day = null;
        // 获取昨天日期
        Date yesdate = null;
        try {
            yesdate = DateUtils.convertDateToDate(DateUtils.getYesterdayDate(), DateUtils.DATE_FORMAT);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // 获取上次最后执行日期与昨天相隔天数
        int dayCount = DateUtils.getDayCountBetweenDate(lastDate, yesdate);
        if (dayCount <= 0) {
            return day;
        }
        if (dayCount == 1) {
            return new String[]{DateUtils.getYesterdayDateStr()};
        } else {
            day = new String[dayCount];
            for (int i = 0; i < dayCount; i++) {
                String date = getInstance(DateUtils.DATE_HOUR_FORMAT).format(DateUtils.addDay(lastDate, i));
                day[i] = date;
            }
        }
        return day;
    }

    /**
     * 获取需要执行的统计的年-月数组
     *
     * @return 格式['2011-01',2011-01']
     */
    public static String[] getExecYearMonth(Date lastYearMonth) {
        String[] yearMonth = null;
        // 获取上个月日期
        Date lastMonth = DateUtils.getOffsetMonthDate(new Date(), 1);
        try {
            lastMonth = DateUtils.convertDateToDate(lastMonth, DateUtils.DATE_YEAE_MONTH);
            //			System.out.println(lastMonth);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 获取上次最后执行日期与昨天相隔天数
        int monthCount = DateUtils.getMonthCountBetweenDate(lastYearMonth, lastMonth);
        if (monthCount <= 0) {
            return yearMonth;
        }
        if (monthCount == 1) {
            return yearMonth = new String[]{DateUtils.convertDateToString(lastMonth, DateUtils.DATE_YEAE_MONTH)};
        } else {
            yearMonth = new String[monthCount];
            for (int i = 0; i < monthCount; i++) {
                String date = DateUtils.addMonth(lastYearMonth, i, DateUtils.DATE_YEAE_MONTH);
                yearMonth[i] = date;
            }
        }
        return yearMonth;
    }

    /**
     * 获取这个月第一天
     *
     * @return
     */
    public static Date getFirstDayOfMonth() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDate = ca.getTime();
        ca.add(Calendar.MONTH, 1);
        ca.add(Calendar.DAY_OF_MONTH, -1);
        return firstDate;
    }

    /**
     * 获这个月的最后一天
     *
     * @return
     */
    public static Date getLastDayOfMonth() {
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, 1);
        ca.add(Calendar.MONTH, 1);
        ca.add(Calendar.DAY_OF_MONTH, -1);
        return ca.getTime();
    }

    /**
     * 获取一天的最后时间，常用于时间段查询的结束时间的处理
     *
     * @param d
     * @return 得到的时间为在d的23:59:59
     */
    public static Date getLastTimeOfDay(Date d) {
        if (null == d) {
            d = DateUtils.getCurrentDate();
        }
        String dateStr = getInstance(DATE_FORMAT).format(d);
        try {
            d = DateUtils.convertStringToDate(dateStr, DATE_FORMAT);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar ca = Calendar.getInstance();
        ca.setTime(d);
        ca.add(Calendar.DAY_OF_MONTH, 1);
        ca.add(Calendar.MILLISECOND, -1);
        d = ca.getTime();
        return d;
    }

    /**
     * 获查询日期区间
     * 今天(0), 近一周(1), 本月(2),近一月(3) ;
     *
     * @return Date[0] 开始时间  Date[1] 结束时间
     */
    public static Date[] getDateSection(int dateType) {
        Date[] dateSection = new Date[2];
        if (DateConstants.TODAY.value == dateType) {
            dateSection[0] = getTodayDate();
            dateSection[1] = dateSection[0];
        } else if (DateConstants.NEARLYWEEK.value == dateType) {
            dateSection[0] = getOffsetDayDate(getTodayDate(), 7);
            dateSection[1] = getTodayDate();
        } else if (DateConstants.NEARLYMONTH.value == dateType) {
            dateSection[0] = getOffsetMonthDate(getTodayDate(), 1);
            dateSection[1] = getTodayDate();
        } else if (DateConstants.MONTH.value == dateType) {
            dateSection[0] = getFirstDayOfMonth();
            dateSection[1] = getLastDayOfMonth();
        } else {
            dateSection = null;
        }
        return dateSection;
    }


    public static boolean isNumberic(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算两个日期之间相差的天数
     *
     * @param smdate 较小的时间
     * @param bdate  较大的时间
     * @return 相差天数
     * @throws ParseException
     */
    public static int daysBetween(Date smdate, Date bdate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        smdate = sdf.parse(sdf.format(smdate));
        bdate = sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();
        cal.setTime(smdate);
        long time1 = cal.getTimeInMillis();
        cal.setTime(bdate);
        long time2 = cal.getTimeInMillis();
        long betweenDays = (time2 - time1) / (1000 * 3600 * 24);

        return Integer.parseInt(String.valueOf(betweenDays));
    }

    /**
     * @return
     * @Description
     * @author HYK
     * @data 2017
     */
    public static String getNowDate() {
        return convertDateToString(new Date(), TIMEF_FORMAT);
    }


    public static Date getStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime();
    }

    public static Date getZeroTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);
        return LocalDateTimeToDate(result);
    }

    public static Date getEightTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 8, 0, 0);
        return LocalDateTimeToDate(result);
    }

    public static Date getYesterdatEightTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 8, 0, 0);
        return LocalDateTimeToDate(result.minusDays(1));
    }

    public static Date getYesterdatZeroTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);
        return LocalDateTimeToDate(result.minusDays(1));
    }

    public static Date getEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime();
    }

    /**
     * 获取距离当前时间前多少天的日期
     *
     * @param dayNum 被减数
     * @return
     */
    public static Date getBeforeNowTimeByDays(int dayNum) {
        LocalDateTime result = LocalDateTime.now().minusDays(dayNum);
        return LocalDateTimeToDate(result);
    }

    static Date LocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    static Date LocalDateToDate(LocalDate localDate) {
        return Date.from(localDate.atTime(0, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将UTC时间字符串转为Date类型
     *
     * @param timestamp
     * @return
     */
    public static Date coverStringFormatTime(String timestamp) {
        Date date;
        try {
            date = DateUtils.convertStringToDate(timestamp, FULL_TIME_FORMAT_UTC);
        } catch (ParseException e) {
            return getStartTime();
        }
        return DateUtils.addMinute(date, 60 * 8);
    }

    public static Date getFridayForThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate friday = today.with(DayOfWeek.FRIDAY);
        return LocalDateToDate(friday);
    }

    public static Date getFridayForNextWeek() {
        LocalDate today = LocalDate.now();
        LocalDate nextDay = today.plusWeeks(1);
        LocalDate friday = nextDay.with(DayOfWeek.FRIDAY);
        return LocalDateToDate(friday);
    }

    public static Date getFridayForThisQuarter() {
        LocalDate today = LocalDate.now();
        Month month = today.getMonth();
        int year = today.getYear();
        int dayOfMonth = today.getDayOfMonth();
        LocalDate lastMouthForThisQuarter = getLastMouthForThisQuarter(year, month, 2, dayOfMonth);
        LocalDate lastFridayForThisMouth = getLastFridayForThisMouth(lastMouthForThisQuarter);
        return LocalDateToDate(lastFridayForThisMouth);
    }

    public static Date getFridayForThisQuarterBybinance() {
        LocalDateTime today = LocalDateTime.now();
        Month month = today.getMonth();
        int year = today.getYear();
        int dayOfMonth = today.getDayOfMonth();
        LocalDate lastMouthForThisQuarter = getLastMouthForThisQuarter(year, month, 2, dayOfMonth);
        LocalDate lastFridayForThisMouth = getLastFridayForThisMouth(lastMouthForThisQuarter);
        LocalDateTime endTime = LocalDateTime.of(lastFridayForThisMouth.getYear(), lastFridayForThisMouth.getMonth().getValue()
                , lastFridayForThisMouth.getDayOfMonth(), 16, 0);
        //如果月份正好是季度的最后一个月 且 季度合约即将转为次周（离季度合约最后的日子补足两周或刚好两周）
        if (month.compareTo(lastMouthForThisQuarter.getMonth()) == 0 && endTime.minusDays(14).compareTo(today) <= 0) {
            if (month.compareTo(Month.DECEMBER) == 0) {
                //如果是12月的话，年需要再加1
                year++;
            }
            lastMouthForThisQuarter = getLastMouthForThisQuarter(year, month.plus(1), 2, dayOfMonth);
            lastFridayForThisMouth = getLastFridayForThisMouth(lastMouthForThisQuarter);
        }
        return LocalDateToDate(lastFridayForThisMouth);
    }

    private static LocalDate getLastMouthForThisQuarter(int year, Month month, int plusNum, int dayOfMonth) {
        return LocalDate.of(year, month.firstMonthOfQuarter().plus(plusNum), dayOfMonth);
    }

    private static LocalDate getLastFridayForThisMouth(LocalDate localDate) {
        return localDate.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY));
    }

    /**
     * 当前季度的结束时间     *     * @return
     */
    public static Date getCurrentQuarterEndTime() {
        Calendar c = Calendar.getInstance();
        int currentMonth = c.get(Calendar.MONTH) + 1;
        Date now = null;
        try {
            if (currentMonth <= 3) {
                c.set(Calendar.MONTH, 2);
                c.set(Calendar.DATE, 31);
            } else if (currentMonth <= 6) {
                c.set(Calendar.MONTH, 5);
                c.set(Calendar.DATE, 30);
            } else if (currentMonth <= 9) {
                c.set(Calendar.MONTH, 8);
                c.set(Calendar.DATE, 30);
            } else if (currentMonth <= 12) {
                c.set(Calendar.MONTH, 11);
                c.set(Calendar.DATE, 31);
            }
            now = c.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return now;
    }


    public static Date getDateStr(String time) {
        SimpleDateFormat formatter = new SimpleDateFormat(TIMEF_FORMAT);
        try {
            if (StringUtils.isNotEmpty(time)) {
                return formatter.parse(time);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Long convertDateToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateStr);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Long getStartOfDayTimestamp(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date(timestamp));
        try {
            Date date = sdf.parse(dateStr);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * 获取当前年份。
     *
     * @return 当前年份
     */
    public static String getCurrentYear() {
        return LocalDate.now().getYear() + "";
    }

    public static String getLastYear(Integer a) {
        return String.valueOf(Integer.parseInt(getCurrentYear()) - a);
    }

    /**
     * 获取从当前年份开始的过去 N 年的年份，包括当前年份。
     *
     * @param numberOfYears 要获取的总年份数量（包括当前年份）
     * @return 包含从当前年份开始的 N 年年份的数组
     */
    public static String[] getPastYearsIncludingCurrent(int numberOfYears) {
        String currentYear = getCurrentYear();
        String[] years = new String[numberOfYears];
        for (int i = 0; i < numberOfYears; i++) {
            years[i] = String.valueOf(Integer.parseInt(currentYear) - i);
        }
        return years;
    }

    public static String[] getPastYearsExcludingCurrentAndPrevious(int numberOfYears) {
        String currentYear = getCurrentYear();
        int currentYearInt = Integer.parseInt(currentYear);
        String[] years = new String[numberOfYears];
        int startYear = currentYearInt - numberOfYears - 1;
        for (int i = 0; i < numberOfYears; i++) {
            years[i] = String.valueOf(startYear + i);
        }
        return years;
    }


    public static String[] getPastYearsIncludingCurrentNew(int numberOfYears) {
        String currentYear = getCurrentYear(); // 假设 getCurrentYear() 返回当前年份的字符串
        int currentYearInt = Integer.parseInt(currentYear);
        String[] years = new String[numberOfYears];
        // 计算起始年份，以确保包括当前年份
        int startYear = currentYearInt - numberOfYears + 1;
        for (int i = 0; i < numberOfYears; i++) {
            years[i] = String.valueOf(startYear + i);
        }
        return years;
    }

    public static String getClosestQuarterEndDate(String dateStr) {
        // 解析输入日期字符串
        int month = Integer.parseInt(dateStr.substring(0, 2));
        int day = Integer.parseInt(dateStr.substring(2, 4));
        LocalDate inputDate = LocalDate.of(LocalDate.now().getYear(), month, day);

        // 定义四个季度结束日期
        LocalDate[] quarterEndDates = {
                LocalDate.of(inputDate.getYear(), Month.MARCH, 31),
                LocalDate.of(inputDate.getYear(), Month.JUNE, 30),
                LocalDate.of(inputDate.getYear(), Month.SEPTEMBER, 30),
                LocalDate.of(inputDate.getYear(), Month.DECEMBER, 31)
        };

        // 找到距离输入日期最近的且在输入日期之前的季度结束日期
        LocalDate closestDate = null;
        long minDaysDiff = Long.MAX_VALUE;
        for (LocalDate quarterEndDate : quarterEndDates) {
            if (quarterEndDate.isBefore(inputDate)) {
                long daysDiff = ChronoUnit.DAYS.between(quarterEndDate, inputDate);
                if (daysDiff < minDaysDiff) {
                    minDaysDiff = daysDiff;
                    closestDate = quarterEndDate;
                }
            }
        }
        // 返回季度结束日期的字符串表示
        return closestDate != null ? formatAsMMDD(closestDate) : "No valid date";
    }

    private static String formatAsMMDD(LocalDate date) {
        return String.format("%02d%02d", date.getMonthValue(), date.getDayOfMonth());
    }

    public static List<String> getPreviousThreeQuarters(String startQuarter) throws ParseException {
        List<String> quarters = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DATE_FORMATNew.parse(startQuarter));
        for (int i = 0; i < 4; i++) {
            quarters.add(getQuarterEndDate(calendar));
            calendar.add(Calendar.MONTH, -3);  // 向前推算到上一个季度
        }
        Collections.reverse(quarters); // 将列表反转，以确保按时间顺序排列
        return quarters;
    }


    private static String getQuarterEndDate(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH) + 1;
        int endMonth;
        if (month <= 3) endMonth = 3;
        else if (month <= 6) endMonth = 6;
        else if (month <= 9) endMonth = 9;
        else endMonth = 12;

        calendar.set(Calendar.MONTH, endMonth - 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return DATE_FORMATNew.format(calendar.getTime());
    }

    public static String getPreviousYearDate(String currentDate) throws ParseException {
        // 解析输入的日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DATE_FORMATNew.parse(currentDate));
        // 获取年份和月份
        int year = calendar.get(Calendar.YEAR);
        // 计算新的年份
        int previousYear = year - 1;
        // 构造新的日期字符串
        return String.format("%d%02d%02d", previousYear, calendar.get(Calendar.MONTH) + 1,
                calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    }


//    // 根据输入的季度日期，返回上一个季度的最后一天
//    public static String getPreviousQuarterEnd(String currentDateStr) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//        LocalDate date = LocalDate.parse(currentDateStr, formatter);
//
//        // 确定当前季度的最后一个月
//        int currentMonth = date.getMonthValue();
//        int currentQuarterLastMonth = ((currentMonth - 1) / 3) * 3 + 3;
//
//        // 获取当前季度的最后一天
//        LocalDate currentQuarterEnd = LocalDate.of(
//                date.getYear(),
//                currentQuarterLastMonth,
//                currentQuarterLastMonth == 3 ? 31 :
//                        currentQuarterLastMonth == 6 ? 30 :
//                                currentQuarterLastMonth == 9 ? 30 : 31
//        );
//        // 如果输入日期是季度末，则直接返回
//        if (date.isEqual(currentQuarterEnd)) {
//            return currentDateStr;
//        }
//        // 计算上一个季度的最后一天
//        LocalDate previousQuarterEnd;
//        if (currentMonth >= 1 && currentMonth <= 3) {
//            previousQuarterEnd = LocalDate.of(date.getYear() - 1, 12, 31);
//        } else if (currentMonth >= 4 && currentMonth <= 6) {
//            previousQuarterEnd = LocalDate.of(date.getYear(), 3, 31);
//        } else if (currentMonth >= 7 && currentMonth <= 9) {
//            previousQuarterEnd = LocalDate.of(date.getYear(), 6, 30);
//        } else { // currentMonth >= 10 && currentMonth <= 12
//            previousQuarterEnd = LocalDate.of(date.getYear(), 9, 30);
//        }
//        return formatter.format(previousQuarterEnd);
//    }

    public static String getPreviousQuarterEnd(String currentDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(currentDateStr, formatter);
        // 根据月份和日来确定上一个季度的最后一天
        switch (date.getMonthValue()) {
            case 3:
                date = date.minusYears(1).withMonth(12).withDayOfMonth(31);
                break;
            case 6:
                date = date.withMonth(3).withDayOfMonth(31);
                break;
            case 9:
                date = date.withMonth(6).withDayOfMonth(30);
                break;
            case 12:
                date = date.withMonth(9).withDayOfMonth(30);
                break;
        }
        return formatter.format(date);
    }



    public static String formatDateStringWithNextDay(String dateStr) {
        try {
            // 定义输入日期的格式
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            // 定义输出日期的格式
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 解析输入日期字符串
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            // 加一天
            LocalDate nextDay = date.plusDays(0);
            // 格式化日期并返回
            return nextDay.format(outputFormatter);
        } catch (DateTimeParseException e) {
            // 捕获解析日期字符串时的异常
            e.printStackTrace();
            return null;
        }
    }


    // 获取前推 x 个月的数据
    public static String getPreviousMonthsData(String dateStr, int months) {
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        // 解析输入日期
        LocalDate date = LocalDate.parse(dateStr, formatter);
        // 前推 x 个月
        LocalDate resultDate = date.minus(months, ChronoUnit.MONTHS);
        // 格式化结果日期
        return resultDate.format(formatter);
    }

    public static String getNextQuarter(String startQuarter) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DATE_FORMATNew.parse(startQuarter));
        calendar.add(Calendar.MONTH, -6);  // 向后推算一个季度
        return getQuarterEndDate(calendar);
    }

    public static String getPreviousQuarter(String startQuarter) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DATE_FORMATNew.parse(startQuarter));
        calendar.add(Calendar.MONTH, -3);  // 向前推算到上一个季度
        return getQuarterEndDate(calendar);
    }


    public static String getCurrentDateStr() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate today = LocalDate.now();
        return today.format(formatter);
    }

    public static List<String> generateDateRange(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        List<String> dateList = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dateList.add(current.format(formatter));
            current = current.plusDays(1);
        }
        return dateList;
    }


    /**
     * 提取在指定起始和结束标签之间的字符串
     *
     * @param text 原始文本
     * @return 提取的字符串
     */
    private static final Pattern pattern = Pattern.compile("派([0-9]*\\.?[0-9]+)元");

    public static List<String> extractValues(String text) {
        List<String> values = new ArrayList<>();
        // 正则表达式匹配"派"和"元"之间的数值
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            // 提取匹配到的数值
            String value = matcher.group(1).trim();
            values.add(value);
        }
        return values;
    }


    public static void main(String[] args) throws ParseException {

        String[] pastYearsIncludingCurrent = DateUtils.getPastYearsIncludingCurrent(3);

        System.out.println(getLastYear(1));
        System.out.println(getLastYear(2));
    }
}
