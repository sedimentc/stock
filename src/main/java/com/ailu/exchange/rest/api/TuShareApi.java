package com.ailu.exchange.rest.api;

import com.alibaba.fastjson.JSONObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface TuShareApi {

    @POST("http://api.tushare.pro")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> genericMethods(@Body JSONObject paramsJson);




}
