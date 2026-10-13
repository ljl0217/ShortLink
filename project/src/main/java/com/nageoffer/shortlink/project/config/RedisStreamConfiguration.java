package com.nageoffer.shortlink.project.config;

import com.nageoffer.shortlink.project.mq.consume.ShortLinkStatsSaveConsume;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;


/**
 * Redis Stream 消息队列配置
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfiguration {
    private final RedisConnectionFactory redisConnectionFactory;
    private final ShortLinkStatsSaveConsume shortLinkStatsSaveConsume;
    private final StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void initGroup() {
        try {
            // 先检查 stream 是否存在
            Boolean hasStream = stringRedisTemplate.hasKey(SHORT_LINK_STATS_STREAM_TOPIC_KEY);
            if (!hasStream) {
                // 如果 stream 不存在，必须先添加一条 dummy 数据，不然创建消费组会失败
                stringRedisTemplate.opsForStream().add(SHORT_LINK_STATS_STREAM_TOPIC_KEY, Collections.singletonMap("init", "0"));
                log.info("初始化创建 Stream: {}", SHORT_LINK_STATS_STREAM_TOPIC_KEY);
            }

            // 查询现有消费组
            List<StreamInfo.XInfoGroup> groups = stringRedisTemplate.opsForStream().groups(SHORT_LINK_STATS_STREAM_TOPIC_KEY).toList();

            boolean exists = groups.stream().anyMatch(g -> g.groupName().equals(SHORT_LINK_STATS_STREAM_GROUP_KEY));
            if (!exists) {
                stringRedisTemplate.opsForStream().createGroup(SHORT_LINK_STATS_STREAM_TOPIC_KEY, ReadOffset.from("0"), SHORT_LINK_STATS_STREAM_GROUP_KEY);
                log.info("创建消费组成功: stream={}, group={}", SHORT_LINK_STATS_STREAM_TOPIC_KEY, SHORT_LINK_STATS_STREAM_TOPIC_KEY);
            } else {
                log.info("消费组已存在: stream={}, group={}", SHORT_LINK_STATS_STREAM_TOPIC_KEY, SHORT_LINK_STATS_STREAM_GROUP_KEY);
            }
        } catch (Exception e) {
            log.error("初始化消费组失败, stream={}, group={}",SHORT_LINK_STATS_STREAM_TOPIC_KEY, SHORT_LINK_STATS_STREAM_GROUP_KEY, e);
        }
    }


    @Bean
    public ExecutorService asyncStreamConsume() {
        AtomicInteger index = new AtomicInteger();
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(processors,
                processors + (processors >> 1),
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setName("stream_consumer_short-link_stats_" + index.incrementAndGet());
                            thread.setDaemon(true);
                            return thread;
                        }
                  );
    }
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(ExecutorService asyncStreamConsume) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .batchSize(10)
                        .executor(asyncStreamConsume)
                        .pollTimeout(Duration.ofSeconds(3))
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);
        streamMessageListenerContainer.receiveAutoAck(Consumer.from(SHORT_LINK_STATS_STREAM_GROUP_KEY, "stats-consume"),
                StreamOffset.create(SHORT_LINK_STATS_STREAM_TOPIC_KEY, ReadOffset.lastConsumed()), shortLinkStatsSaveConsume);
        return streamMessageListenerContainer;
    }
}
