package com.ailu.exchange.service.redis;

import com.ailu.exchange.dao.vo.AnswerVoInZset;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository("objectRedisService")
public class ObjectRedisServiceImpl implements ObjectRedisService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object obj) {
        redisTemplate.opsForValue().set(key, obj);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public Object get(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        return obj;
    }

    @Override
    public void set(String key, Object obj, int minute) {
        set(key, obj, minute, TimeUnit.MINUTES);
    }

    @Override
    public void set(String key, Object obj, int time, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, obj, time, timeUnit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setHashModel(String key, String HK, Object HV) {
        redisTemplate.opsForHash().put(key, HK, HV);
    }

    public void setHashModel(String key, String HK) {
        setHashModel(key, HK, HK);
    }

    public void setHashMap(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public void setHashMap(String key, Map<String, Object> map, Integer num, TimeUnit timeUnit) {
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, num, timeUnit);
    }

    public void deleteHashModel(String key, String price) {
        redisTemplate.opsForHash().delete(key, price);
    }

    @Override
    public Object getHashModel(String key, String hk) {
        Object obj = redisTemplate.opsForHash().get(key, hk);
        return obj;
    }

    @Override
    public Object getHashModel(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Object getEntries(String key) {
        Object obj = redisTemplate.opsForHash().entries(key);
        return obj;
    }

    @Override
    public Boolean isMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public Double getSetScore(String key, String value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public Set<Object> getAllSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public void setSortSet(String key, String value, Double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    @Override
    public Long getSetLength(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    @Override
    public Long getZSetCountByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    @Override
    public boolean isMemberOfSortSet(String zsetKey, String jsonString) {
        JSONObject jsonObject = JSONObject.parseObject(jsonString);
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(zsetKey, 0, 10);
        if (members != null) {
            List<String> dataNew = new ArrayList<>();
            List<String> collect = members.stream().map(String::valueOf).collect(Collectors.toList());
            for (String s : collect) {
                JSONObject jsonObject1 = JSONObject.parseObject(s);
                dataNew.add(jsonObject1.getString("data"));
            }
            return dataNew.contains(jsonObject.getString("data"));
        }

        return true;
    }


    @Override
    public Set<Object> getSortSetLimit(String sort, String key, Integer limit) {
        if (sort == null) {
            sort = "asc";
        }
        if (sort.equals("asc")) {
            //正序
            return redisTemplate.opsForZSet().range(key, 0, limit);
        } else {
            //倒叙
            return redisTemplate.opsForZSet().reverseRange(key, 0, limit);
        }
    }

    @Override
    public Set<Object> getAllSortSet(String sort, String key) {
        if (sort == null) {
            sort = "asc";
        }
        if (sort.equals("asc")) {
            //正序
            return redisTemplate.opsForZSet().range(key, 0, -1);
        } else {
            //倒叙
            return redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        }
    }

    @Override
    public Set<Object> getAllSortSetByPage(String sort, String key, Integer pageSize, Integer pageNum) {
        if (sort == null) {
            sort = "asc";
        }
        //Long size = redisTemplate.opsForZSet().size(key);
        Long start = (pageNum - 1) * pageSize.longValue();
        Long end = start + pageSize - 1;
        if (sort.equals("asc")) {
            //正序
            return redisTemplate.opsForZSet().range(key, start, end);
        } else {
            //倒叙
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        }
    }

    @Override
    public Long removeSortSetByScore(String key, Long min, Long max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min.doubleValue(), max.doubleValue());
    }

    @Override
    public Long removeSortSetByValue(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }


    @Override
    public Long removeSortSetByIndex(String key, Long start, Long end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    @Override
    public Long unionAndStore(String key, String otherKey, String destkey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destkey);
    }


    @Override
    public Map<Object, Object> getHashModelAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public Map<Object, Object> getHashModelAllNew(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public Long unionAndStore(String key, List<String> otherKey, String destkey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destkey);
    }

    @Override
    public Long getSoetSetSize(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }


    @Override
    public List<Object> setKeysZSetSomeDataNew(Map<String, Set<AnswerVoInZset>> map) {
        return redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                connection.openPipeline();
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
                map.forEach((key, vars) -> {
                    vars.forEach(var -> {
                        connection.zAdd(keySerializer.serialize(key), var.getScore(), valueSerializer.serialize(var.getValue()));
                    });
                });
                //connection.closePipeline();
                return null;
            }
        });
    }

    @Override
    public List<Object> hasKeysSize(List<Object> keys) {
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        /* 批量获取多条数据 */
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.openPipeline();
                for (Object key : keys) {
                    redisConnection.zCard(keySerializer.serialize(key));
                }
                return null;
            }
        });

        List<Object> collect = objects.stream().map(val -> String.valueOf(val)).collect(Collectors.toList());

        return collect;
    }

    @Override
    public Boolean checkNoSetValue(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public Long getSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    @Override
    public Boolean isSetMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public void setSetData(String key, String... value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @Override
    public Long removeMember(String key, String... value) {
        return redisTemplate.opsForSet().remove(key, value);
    }


}
