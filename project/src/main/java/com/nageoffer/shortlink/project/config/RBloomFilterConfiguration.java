package com.nageoffer.shortlink.project.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 添加布隆过滤器
 */
@Configuration
public class RBloomFilterConfiguration {
    /**
     * 防止用户注册查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> shortUrlCreateCachePenetrationBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter("shortUrlCreateCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(10000000L, 0.001);
        return cachePenetrationBloomFilter;
    }
}
