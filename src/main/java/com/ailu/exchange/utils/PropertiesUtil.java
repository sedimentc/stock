package com.ailu.exchange.utils;

import com.ailu.exchange.utils.web.WebResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
public class PropertiesUtil {

    public static Properties getProperties() {
        Properties prop = new Properties();
        InputStream in = WebResult.class.getClassLoader().getResourceAsStream("webResult.properties");
        try {
            //InputStream in = new FileInputStream(new File(path));
            prop.load(in);
        } catch (IOException e) {
            log.error("返回信息时，获取配置文件出错,{}", e.getMessage(), e);
        }
        return prop;
    }

    public static Map<String, String> getMapForProperties() {
        Properties prop = new Properties();
        InputStream in = WebResult.class.getClassLoader().getResourceAsStream("config/webResult.properties");
        try {
            //InputStream in = new FileInputStream(new File(path));
            prop.load(in);
        } catch (IOException e) {
            log.error("返回信息时，获取配置文件出错,{}", e.getMessage(), e);
        }
        return PropertiesToMap(prop);
    }

    public static Map<String, String> PropertiesToMap(Properties properties) {
        Map<String, String> map = new HashMap<String, String>((Map) properties);
        return map;
    }


}
