package com.ailu.exchange.rest.api;

import com.alibaba.fastjson.JSONObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * @Author: lzx
 * @Date: 2020/6/28 19:28
 */
public interface OkLinkApi {


    /**
     * 获取某条公链上某个代币的持仓列表，仅返回余额为top10000的地址。
     *
     * @param chainShortName       公链缩写符号，例如：btc、eth
     * @param tokenContractAddress 代币地址
     * @param page                 返回条数，默认返回最近的20条，最多100条
     * @param limit                页码  100
     * @return
     */
    @GET("/api/v5/explorer/token/position-list")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getPositionList(@Header("Ok-Access-Key") String apiKey,
                                     @Query("chainShortName") String chainShortName,
                                     @Query("tokenContractAddress") String tokenContractAddress,
                                     @Query("page") Integer page,
                                     @Query("limit") String limit);
    /**
     * 通过钱包地址加代币地址获取到对应代币的余额
     *
     * @param apiKey
     * @param chainShortName
     * @param address
     * @param protocolType
     * @param tokenContractAddress
     * @return
     */
    @GET("/api/v5/explorer/address/token-balance")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getTokenBalance(@Header("Ok-Access-Key") String apiKey,
                                     @Query("chainShortName") String chainShortName,
                                     @Query("address") String address,
                                     @Query("protocolType") String protocolType,
                                     @Query("limit") String limit,
                                     @Query("tokenContractAddress") String tokenContractAddress);
    /**
     * 根据持有人地址获取划转记录
     *
     * @param apiKey
     * @param chainShortName
     * @param tokenContractAddress
     * @param page
     * @param limit 当前显示页数 100
     * @return
     */
    @GET("/api/v5/explorer/address/token-transaction-list")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getTokenTransactionList(@Header("Ok-Access-Key") String apiKey,
                                             @Query("chainShortName") String chainShortName,
                                             @Query("address") String address,
                                             @Query("protocolType") String protocolType,
                                             @Query("tokenContractAddress") String tokenContractAddress,
                                             @Query("isFromOrTo") String isFromOrTo,
                                             @Query("page") String page,
                                             @Query("limit") String limit);

    /**
     * 查询指定交易哈希代币交易
     *
     * @param apiKey
     * @param chainShortName
     * @param page
     * @param limit 100
     * @return
     */
    @GET("/api/v5/explorer/transaction/token-transaction-detail")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getTokenTransactionDetail(@Header("Ok-Access-Key") String apiKey,
                                               @Query("chainShortName") String chainShortName,
                                               @Query("txId") String txId,
                                               @Query("page") String page,
                                               @Query("limit") String limit);
    /**
     * 查询指定交易哈希代币交易
     *
     * @param apiKey
     * @param chainShortName
     * @return
     */
    @GET("/api/v5/explorer/address/information-evm")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getInformationEvm(@Header("Ok-Access-Key") String apiKey,
                                               @Query("chainShortName") String chainShortName,
                                               @Query("address") String address);
    @GET("/api/v5/explorer/address/entity-label")
    @Headers({"Content-Type: application/json"})
    Call<JSONObject> getEntityLabel(@Header("Ok-Access-Key") String apiKey,
                                               @Query("chainShortName") String chainShortName,
                                               @Query("address") String address);

}
