package com.nageoffer.shortlink.project.common.constant;

/**
 * Redis key 常量类
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short_link:goto:%s";
    /**
     * 短链接跳转前缀key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short_link:is-null:goto:%s";

    /**
     * 短链接跳转锁前缀key
     */
    public static final String LOCK_SHORT_LINK_KEY = "short-link:lock:goto:%s";
    /**
     * 短链接UV访问量key
     */
    public static final String SHORT_LINK_ACCESS_UV_KEY = "short_link:access:uv:%s";
    /**
     * 短链接访UIP问量key
     */
    public static final String SHORT_LINK_ACCESS_UIP_KEY = "short_link:access:uip:%s";
    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short_link:lock:update-gid:%s";
    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link:delay-queue:stats";
    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short-link:stats-stream";

    /**
     * 短链接监控消息保存队列 Group 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short-link:stats-stream:only-group";
}
