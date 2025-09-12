package com.ailu.exchange.rest;

import com.ailu.exchange.rest.api.*;
import com.alibaba.fastjson.support.retrofit.Retrofit2ConverterFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.util.concurrent.TimeUnit;

/**
 * 配置、初始化OkHttp
 *
 * @author Vincent
 * @date 2018-05-08 15:37
 */
@Slf4j
@Configuration
public class API {

    static OkHttpClient OK_HTTP_CLIENT_STANDARD = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public <T> T build(OkHttpClient client, String baseUrl, Class<T> clz) {
        return build(client, baseUrl, clz, new CustomConvertFactory(), new Retrofit2ConverterFactory());
    }

    private <T> T build(OkHttpClient client, String baseUrl, Class<T> clz, Converter.Factory... factories) {

        Retrofit.Builder builder = new Retrofit.Builder();

        for (Converter.Factory factory : factories) {
            builder.addConverterFactory(factory);
        }

        return builder
                .client(client)
                .baseUrl(baseUrl)
                .build()
                .create(clz);
    }

    @Bean
    public TuShareApi tuShareApi(@Value("${tuShare.url}") String url) {
        return build(OK_HTTP_CLIENT_STANDARD, url, TuShareApi.class);
    }

    @Bean
    public OkLinkApi oklinkApi(@Value("${oklink.url}") String url) {
        return build(OK_HTTP_CLIENT_STANDARD, url, OkLinkApi.class);
    }
}
