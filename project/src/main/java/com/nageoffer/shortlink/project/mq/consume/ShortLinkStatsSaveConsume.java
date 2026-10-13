package com.nageoffer.shortlink.project.mq.consume;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.config.GaodeProperties;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.mq.Idempotent.MessageQueueIdempotentHandler;
import com.nageoffer.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveConsume implements StreamListener<String, MapRecord<String, String, String>> {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final GaodeProperties gaodeProperties;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;
    private final ShortLinkMapper shortLinkMapper;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        if (!messageQueueIdempotentHandler.isMessageProcessed(id.toString())) {
            if (messageQueueIdempotentHandler.isAccomplish(id.toString())) {
                return;
            }
            throw new ServiceException("消息未完成流程，需要消息队列重试");
        }
        try {
            Map<String, String> producerMap = message.getValue();
            String fullShortUrl = producerMap.get("fullShortUrl");
            if (StrUtil.isNotBlank(fullShortUrl)) {
                String gid = producerMap.get("gid");
                ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
                if (statsRecord != null) {  // 确保 statsRecord 不为 null
                    actualSaveShortLinkStats(fullShortUrl, gid, statsRecord);
                } else {
                    // 处理 statsRecord 为 null 的情况
                    log.warn("StatsRecord is null for fullShortUrl: {}", fullShortUrl);
                }
            }
            stringRedisTemplate.opsForStream().delete(Objects.requireNonNull(stream), id.getValue());
        } catch (Throwable ex) {
            messageQueueIdempotentHandler.delMessageProcessed(id.toString());
            log.error("记录短链接监控消费异常", ex);
        }
        messageQueueIdempotentHandler.setAccomplish(id.toString());
    }

    public void actualSaveShortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        try {
            if (!rLock.tryLock(3, 10, TimeUnit.SECONDS)) {
                delayShortLinkStatsProducer.send(statsRecord);
                return;
            }
            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            int hour = LocalDateTime.now().getHour();
            int week = LocalDateTime.now().getDayOfWeek().getValue();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .hour(hour)
                    .weekday(week)
                    .fullShortUrl(fullShortUrl)
                    .date(LocalDate.now())
                    .build();
            linkAccessStatsMapper.linkAccessStatsInsert(linkAccessStatsDO);
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", gaodeProperties.getKey());
            localeParamMap.put("ip", statsRecord.getRemoteAddr());
            String localeResult = HttpUtil.get(gaodeProperties.getUrl(), localeParamMap);
            JSONObject localeJson = JSONObject.parseObject(localeResult);
            String infoCode = localeJson.getString("infocode");
            String actualProvince = "未知";
            String actualCity = "未知";
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                String province = localeJson.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(actualProvince = unknownFlag ? "未知" : province)
                        .city(actualCity = unknownFlag ? "未知" : localeJson.getString("city"))
                        .adcode(unknownFlag ? "未知" : localeJson.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .date(LocalDate.now())
                        .build();
                linkLocaleStatsMapper.linkLocaleStatsInsert(linkLocaleStatsDO);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .os(statsRecord.getOs())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(LocalDate.now())
                        .build();
                linkOsStatsMapper.linkOsStatsInsert(linkOsStatsDO);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(statsRecord.getBrowser())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(LocalDate.now())
                        .build();
                linkBrowserStatsMapper.linkBrowserStatsInsert(linkBrowserStatsDO);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .device(statsRecord.getDevice())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(LocalDate.now())
                        .build();
                linkDeviceStatsMapper.linkDeviceStatsInsert(linkDeviceStatsDO);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .network(statsRecord.getNetwork())
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .date(LocalDate.now())
                        .build();
                linkNetworkStatsMapper.linkNetworkStatsInsert(linkNetworkStatsDO);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .user(statsRecord.getUv())
                        .ip(statsRecord.getRemoteAddr())
                        .browser(statsRecord.getBrowser())
                        .os(statsRecord.getOs())
                        .network(statsRecord.getNetwork())
                        .device(statsRecord.getDevice())
                        .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                        .fullShortUrl(fullShortUrl)
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);
                shortLinkMapper.incrementStats(gid, fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0 ,statsRecord.getUipFirstFlag()? 1 : 0);
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                        .todayPv(1)
                        .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                        .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                        .fullShortUrl(fullShortUrl)
                        .date(LocalDate.now())
                        .build();
                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("短链接访问量统计异常");
        } finally {
            rLock.unlock();
        }
    }
}
