package com.ailu.exchange.service.redis;


import com.ailu.exchange.dao.vo.AnswerVoInZset;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ObjectRedisService {


    void set(String key, Object obj);

    void remove(String key);

    Object get(String key);

    void set(String key, Object obj, int time);

    void set(String key, Object obj, int time, TimeUnit timeUnit);

    void setHashModel(String key, String HK, Object HV);

    void setHashModel(String key, String HK);

    void setHashMap(String key, Map<String, Object> map);

    void setHashMap(String key, Map<String, Object> map, Integer num, TimeUnit timeUnit);

    void deleteHashModel(String key, String price);

    Object getHashModel(String key, String hk);

    Object getHashModel(String key);

    Object getEntries(String key);

    Boolean isMember(String key, String value);

    Set<Object> getAllSet(String key);

    //获取对应的排序值
    Double getSetScore(String key, String value);


    /**
     * @return void
     * @author YiYi
     * @Description 对有序的set操作
     * @Date 2019/5/9 17:20
     * @Param [key, value, score]
     **/
    void setSortSet(String key, String value, Double score);

    Long getSetLength(String key);

    Set<Object> getSortSetLimit(String sort, String key, Integer limit);


    Long getZSetCountByScore(String key, double min, double max);

    boolean isMemberOfSortSet(String key, String member);

    /**
     * @return java.lang.Object
     * @author YiYi
     * @Description 获取排序的set
     * @Date 2019/5/9 17:20
     * @Param [sort, key]
     **/
    Set<Object> getAllSortSet(String sort, String key);

    /**
     * @return java.lang.Object
     * @author YiYi
     * @Description 获取分页的排序的set
     * @Date 2019/5/9 17:20
     * @Param [sort, key, pageSize, pageNum]
     **/
    Set<Object> getAllSortSetByPage(String sort, String key, Integer pageSize, Integer pageNum);

    //删除某些值
    Long removeSortSetByValue(String key, Object... values);

    //删除一定排序范围的数据,返回删除个数
    Long removeSortSetByScore(String key, Long min, Long max);

    //删除一定排序范围的数据,返回删除个数
    Long removeSortSetByIndex(String key, Long start, Long end);

    //合并两个set,注意相同值的score会相加,返回合并后的个数
    Long unionAndStore(String key, String otherKey, String destkey);

    //合并多个set,注意相同值的score会相加,返回合并后的个数
    //destkey,合并后的key
    Long unionAndStore(String key, List<String> otherKey, String destkey);

    //获取有序set的集合的大小
    Long getSoetSetSize(String key);


    List<Object> setKeysZSetSomeDataNew(Map<String, Set<AnswerVoInZset>> map);

    List<Object> hasKeysSize(List<Object> keys);

    //判断普通set值
    Boolean checkNoSetValue(String key, String value);


    Map<Object, Object> getHashModelAll(String key);

    Map<Object, Object> getHashModelAllNew(String key);


    //------set结构
    //获取set值的个数
    Long getSetSize(String key);

    //set里面是否存在value
    Boolean isSetMember(String key, String value);

    //放入set结构的数据
    void setSetData(String key, String... value);

    //移除set里面得到值
    Long removeMember(String key, String... value);

}


