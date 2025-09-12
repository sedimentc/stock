package com.ailu.exchange.dao.vo;

import org.springframework.data.redis.core.ZSetOperations;

/**
 * @Auther: YiYi
 * @Date: 2019/5/30 18:30
 * @Description: redis 的 zset的多个值批量
 */
    public class AnswerVoInZset implements ZSetOperations.TypedTuple<Object>{
    Object value;
    double score;
    public AnswerVoInZset(Object value, double score) {
        this.value = value;
        this.score = score;
    }
    @Override
    public Object getValue() {
        return value;
    }
    @Override
    public Double getScore() {
        return score;
    }
    @Override
    public int compareTo(ZSetOperations.TypedTuple<Object> o) {
        AnswerVoInZset o1 = (AnswerVoInZset) o;
        return new Double(this.score).compareTo(o1.getScore());
    }
}
