package com.ailu.exchange.service.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository("objectOtherRedisService")
public class ObjectOtherRedisServiceImpl implements ObjectOtherRedisService {

//	@Resource(name = "redisTemplate")
//	private RedisTemplate<String,Object> redisTemplate;

	@Override
	public void set(String key, Object obj, RedisTemplate<String,Object> redisTemplate) {
		redisTemplate.opsForValue().set(key, obj);
	}

	@Override
	public void remove(String key, RedisTemplate<String,Object> redisTemplate) {
		redisTemplate.delete(key);
	}

	@Override
	public Object get(String key, RedisTemplate<String,Object> redisTemplate) {
		Object obj = redisTemplate.opsForValue().get(key);
		return obj;
	}

	@Override
	public  List<Object> getKeys(List<String> keys, RedisTemplate<String,Object> redisTemplate) {
		List<Object> objects = redisTemplate.opsForValue().multiGet(keys);
		return objects;
	}

	@Override
	public Boolean hasKey(String key, RedisTemplate<String,Object> redisTemplate){
		Boolean aBoolean = redisTemplate.hasKey(key);
		return aBoolean;
	}

	@Override
	public List<Object> hasKeys(List<Object> keys, RedisTemplate<String,Object> redisTemplate){
		RedisSerializer keySerializer = redisTemplate.getKeySerializer();
		/* 批量获取多条数据 */
		List<Object> objects = redisTemplate.executePipelined(new RedisCallback<String>() {
			@Override
			public String doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.openPipeline();
                for (Object key : keys) {
                	redisConnection.exists(keySerializer.serialize(key));
                }
				return null;
			}
		});

		List<Object> collect = objects.stream().map(val -> String.valueOf(val)).collect(Collectors.toList());

		return collect;
	}


	@Override
	public void set(String key, Object obj, int minute, RedisTemplate<String,Object> redisTemplate) {
		set(key, obj, minute,TimeUnit.MINUTES,redisTemplate);
	}

	@Override
	public void set(String key, Object obj, int time, TimeUnit timeUnit, RedisTemplate<String,Object> redisTemplate) {
		try {
			redisTemplate.opsForValue().set(key, obj, time, timeUnit);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void setHashModel(String key, String HK, Object HV, RedisTemplate<String,Object> redisTemplate){
		redisTemplate.opsForHash().put(key,HK,HV);
	}

	@Override
	public void setHashModel(String key, String HK, RedisTemplate<String,Object> redisTemplate){
		setHashModel(key,HK,HK,redisTemplate);
	}

	@Override
	public void setHashMap(String key, Map<String,Object> map, RedisTemplate<String,Object> redisTemplate){
		redisTemplate.opsForHash().putAll(key,map);
	}

    @Override
    public void setHashMap(String key, Map<String, Object> map, Integer num, TimeUnit timeUnit, RedisTemplate<String,Object> redisTemplate) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, num, timeUnit);
    }

    @Override
	public void deleteHashModel(String key, String price, RedisTemplate<String,Object> redisTemplate){
		redisTemplate.opsForHash().delete(key,price);
	}

	@Override
	public Object getHashModel(String key, String hk, RedisTemplate<String,Object> redisTemplate){
		Object obj = redisTemplate.opsForHash().get(key,hk);
		return obj;
	}

	@Override
	public Object getHashModel(String key, RedisTemplate<String,Object> redisTemplate) {
        return redisTemplate.opsForHash().entries(key);
	}

	@Override
	public Object getEntries(String key, RedisTemplate<String, Object> redisTemplate) {
		Object obj = redisTemplate.opsForHash().entries(key);
		return obj;
	}

	@Override
	public void setSortSet(String key, String value, Double score, RedisTemplate<String,Object> redisTemplate) {
		redisTemplate.opsForZSet().add(key,value,score);
	}

	@Override
	public Set<Object> getAllSortSet(String sort, String key, RedisTemplate<String,Object> redisTemplate) {
		if(sort==null){
			sort = "asc";
		}
		if(sort.equals("asc")){
			//正序
			return redisTemplate.opsForZSet().range(key, 0, -1);
		}else{
			//倒叙
			return redisTemplate.opsForZSet().reverseRange(key,0,-1);
		}
	}

	@Override
	public Set<ZSetOperations.TypedTuple<Object>> getAllSortSetWithScor(String sort, String key, RedisTemplate<String,Object> redisTemplate) {
		if(sort==null){
			sort = "asc";
		}
		if(sort.equals("asc")){
			//正序
			return redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
		}else{
			//倒叙
			return redisTemplate.opsForZSet().reverseRangeWithScores(key,0,-1);
		}
	}

	@Override
	public Set<Object> getAllSortSetByPage(String sort, String key, Integer pageSize, Integer pageNum, RedisTemplate<String,Object> redisTemplate) {
		if(sort==null){
			sort = "asc";
		}
        //Long size = redisTemplate.opsForZSet().size(key);
        Long start= (pageNum-1)*pageSize.longValue();
		Long end= start+pageSize-1;
		if(sort.equals("asc")){
			//正序
			return redisTemplate.opsForZSet().range(key, start, end);
		}else{
			//倒叙
			return redisTemplate.opsForZSet().reverseRange(key,start,end);
		}
	}

	@Override
	public Long removeSortSetByScore(String key, Long min, Long max, RedisTemplate<String,Object> redisTemplate) {
		return redisTemplate.opsForZSet().removeRangeByScore(key, min.doubleValue(), max.doubleValue());
	}

	@Override
	public Long removeSortSetByIndex(String key, Long start, Long end, RedisTemplate<String,Object> redisTemplate) {
		return redisTemplate.opsForZSet().removeRange(key,start,end);
	}

	@Override
	public Long unionAndStore(String key, String otherKey, String destkey, RedisTemplate<String,Object> redisTemplate) {
		return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destkey);
	}

    @Override
    public Long unionAndStore(String key, List<String> otherKey, String destkey, RedisTemplate<String,Object> redisTemplate) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destkey);
    }

	@Override
	public Long getSoetSetSize(String key,RedisTemplate<String,Object> redisTemplate) {
		return redisTemplate.opsForZSet().zCard(key);
	}

	@Override
	public void setSetData(String key, RedisTemplate<String,Object> redisTemplate, Object... obj) {
		redisTemplate.opsForSet().add(key, obj);
	}

	//自增
	@Override
	public Double incrData(String key,Double incrNum,RedisTemplate<String,Object> redisTemplate){
		Double increment = redisTemplate.opsForValue().increment(key, incrNum);
		return increment;
	}


	@Override
	public Set<Object> getAllSet(String key, RedisTemplate<String,Object> redisTemplate) {
		return redisTemplate.opsForSet().members(key);
	}

	@Override
	public Long getSetSize(String key, RedisTemplate<String, Object> redisTemplate) {
		return redisTemplate.opsForSet().size(key);
	}


}
