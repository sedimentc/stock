package com.ailu.exchange.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OkHttpUtil {
    @Resource
    private OkHttpClient okHttpClient;


    final static String RECV_WINDOW = "5000";


    private static final OkHttpClient sharedClient;

    static {
        sharedClient= new OkHttpClient.Builder()
                .connectTimeout(20L, TimeUnit.SECONDS)
                .readTimeout(20L, TimeUnit.SECONDS)
                .build();
    }

    public static JSONObject doPost(String url, JSONObject params) {
        RequestBody body = RequestBody.create(JSON_TYPE, params.toJSONString());
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = null;
        try {
            response = sharedClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return JSONObject.parseObject(response.body().string());
            }
        } catch (Exception e) {
            log.error("okhttp post error >> ex = {}", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }


    public String httpGet(String url, Map<String, String> headMap) throws Exception {
        // 设置HTTP请求参数
        String result;
        Headers setHeaders = SetHeaders(headMap);
        Request request = new Request.Builder().url(url).headers(setHeaders).build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        String string = response.body().string();
        result = String.valueOf(JSON.parse(string));
        return result;
    }
    public String httpGetNew(String url, Map<String, String> headMap) throws Exception {
        // 设置HTTP请求参数
        String result;
        Headers setHeaders = SetHeaders(headMap);
        Request request = new Request.Builder().url(url).headers(setHeaders).build();
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        String dataStr = response.body().string();
        String jsonPart = dataStr.substring(dataStr.indexOf('(') + 1, dataStr.lastIndexOf(')'));
        result = String.valueOf(JSON.parse(jsonPart));
        return result;
    }

    private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=utf-8");

    /**
     * post
     *
     * @param url    请求的url
     * @param params post form 提交的参数
     * @return
     */
    public String postData(String url, String params) {
        RequestBody body = RequestBody.create(JSON_TYPE, params);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            int status = response.code();
            if (status == 200) {
                return response.body().string();
            }
        } catch (Exception e) {
            log.error("okhttp post error >>", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }


    private Headers SetHeaders(Map<String, String> headersParams) {
        Headers headers;
        Headers.Builder headerSbuilder = new Headers.Builder();
        if (!StringUtils.isEmpty(headersParams)) {
            Iterator<String> iterator = headersParams.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                headerSbuilder.add(key, headersParams.get(key));
            }
        }
        headers = headerSbuilder.build();
        return headers;
    }

    /**
     * The way to generate the sign for GET requests
     *
     * @param params: Map input parameters
     * @return signature used to be a parameter in the header
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private static String genGetSign(Map<String, Object> params, String timestamp, String apiKey, String apiSecret) throws NoSuchAlgorithmException,
            InvalidKeyException {
        StringBuilder sb = genQueryStr(params);
        String queryStr = timestamp + apiKey + RECV_WINDOW + sb;

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return bytesToHex(sha256_HMAC.doFinal(queryStr.getBytes()));
    }


    /**
     * To convert bytes to hex
     *
     * @param hash
     * @return hex string
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * To generate query string for GET requests
     *
     * @param map
     * @return
     */
    public static StringBuilder genQueryStr(Map<String, Object> map) {
        Set<String> keySet = map.keySet();
        Iterator<String> iter = keySet.iterator();
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            String key = iter.next();
            sb.append(key)
                    .append("=")
                    .append(map.get(key))
                    .append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb;
    }


}

