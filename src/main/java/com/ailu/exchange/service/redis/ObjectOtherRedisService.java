package com.ailu.exchange.service.redis;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ObjectOtherRedisService {


	void set(String key, Object obj, RedisTemplate<String, Object> redisTemplate);

	void remove(String key, RedisTemplate<String, Object> redisTemplate);

	Object get(String key, RedisTemplate<String, Object> redisTemplate);

	List<Object> getKeys(List<String> keys, RedisTemplate<String, Object> redisTemplate);

	Boolean hasKey(String key, RedisTemplate<String,Object> redisTemplate);

	List<Object> hasKeys(List<Object> keys, RedisTemplate<String, Object> redisTemplate);

	void set(String key, Object obj, int time, RedisTemplate<String, Object> redisTemplate);

	void set(String key, Object obj, int time, TimeUnit timeUnit, RedisTemplate<String, Object> redisTemplate);

	void setHashModel(String key, String HK, Object HV, RedisTemplate<String, Object> redisTemplate);

	void setHashModel(String key, String HK, RedisTemplate<String, Object> redisTemplate);

	void setHashMap(String key, Map<String, Object> map, RedisTemplate<String, Object> redisTemplate);

    void setHashMap(String key, Map<String, Object> map, Integer num, TimeUnit timeUnit, RedisTemplate<String, Object> redisTemplate);

	void deleteHashModel(String key, String price, RedisTemplate<String, Object> redisTemplate);

	Object getHashModel(String key, String hk, RedisTemplate<String, Object> redisTemplate);

	Object getHashModel(String key, RedisTemplate<String, Object> redisTemplate);

	Object getEntries(String key, RedisTemplate<String, Object> redisTemplate);




	/**
	* @author YiYi
	* @Description 对有序的set操作
	* @Date  2019/5/9 17:20
	* @Param [key, value, score]
	* @return void
	**/
	void setSortSet(String key, String value, Double score, RedisTemplate<String, Object> redisTemplate);

	/**
	* @author YiYi
	* @Description 获取排序的set
	* @Date  2019/5/9 17:20
	* @Param [sort, key]
	* @return java.lang.Object
	**/
	Set<Object> getAllSortSet(String sort, String key, RedisTemplate<String, Object> redisTemplate);

	/**
	* @author YiYi
	* @Description 获取分页的排序的set
	* @Date  2019/5/9 17:20
	* @Param [sort, key, pageSize, pageNum]
	* @return java.lang.Object
	**/
	Set<Object> getAllSortSetByPage(String sort, String key, Integer pageSize, Integer pageNum, RedisTemplate<String, Object> redisTemplate);

	Set<ZSetOperations.TypedTuple<Object>> getAllSortSetWithScor(String sort, String key, RedisTemplate<String,Object> redisTemplate);

	//删除一定排序范围的数据,返回删除个数
	Long removeSortSetByScore(String key, Long min, Long max, RedisTemplate<String, Object> redisTemplate);

	//删除一定排序范围的数据,返回删除个数
	Long removeSortSetByIndex(String key, Long start, Long end, RedisTemplate<String, Object> redisTemplate);

	//合并两个set,注意相同值的score会相加,返回合并后的个数
	Long unionAndStore(String key, String otherKey, String destkey, RedisTemplate<String, Object> redisTemplate);

	//合并多个set,注意相同值的score会相加,返回合并后的个数
	//destkey,合并后的key
	Long unionAndStore(String key, List<String> otherKey, String destkey, RedisTemplate<String, Object> redisTemplate);

	Long getSoetSetSize(String key,RedisTemplate<String,Object> redisTemplate);

	void setSetData(String key, RedisTemplate<String,Object> redisTemplate, Object... obj);

	//自增
	Double incrData(String key,Double incrNum,RedisTemplate<String,Object> redisTemplate);

	Set<Object> getAllSet(String key, RedisTemplate<String,Object> redisTemplate);

	Long getSetSize(String key, RedisTemplate<String,Object> redisTemplate);
}


