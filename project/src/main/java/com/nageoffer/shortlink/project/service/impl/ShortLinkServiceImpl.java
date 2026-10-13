package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValidDateType;
import com.nageoffer.shortlink.project.config.BaseProperties;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkPageRepDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkUpdateRepDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;

/**
 * 短链接服务实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    @Value("${short-link.domain.default}")
    private String defaultDomain;
    private final RBloomFilter<String> shortUrlCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ShortLinkMapper shortLinkMapper;
    private final BaseProperties baseProperties;

    /**
     * 创建短链接
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        String shortUrlSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(defaultDomain)
                .append("/")
                .append(shortUrlSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(defaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .shortUri(shortUrlSuffix)
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDate(requestParam.getValidDate())
                .validDateType(requestParam.getValidDateType())
                .describe(requestParam.getDescribe())
                .enableStatus(0)
                .favicon(getFaviconUrl(requestParam.getOriginUrl()))
                .fullShortUrl(fullShortUrl)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .delTime(0L)
                .build();
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .gid(requestParam.getGid())
                .fullShortUrl(fullShortUrl)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException ex) {
                throw new ServiceException(String.format("短链接%s生成重复", fullShortUrl));
        }
        stringRedisTemplate.opsForValue()
                .set(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), requestParam.getOriginUrl(), LinkUtil.getCacheExpireTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS);
        shortUrlCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .fullShortLink("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    /**
     * 短链接分页
     */
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageRepDTO requestParam) {
       IPage<ShortLinkDO> resultPage = shortLinkMapper.pageLink(requestParam);
        return resultPage.convert(each -> {
                    ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
                    result.setDomain("http://" +  each.getDomain());
                    return result;
                });
    }

    /**
     * 短链接分组数量
     */
    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .eq("enable_status", 0)
                .eq("del_flag", 0)
                .eq("del_time", 0L)
                .in("gid", requestParam)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
    }

    /**
     * 修改短链接
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShortLink(ShortLinkUpdateRepDTO requestParam) {
        Wrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO valiableShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (valiableShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        if (Objects.equals(valiableShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValidDateType.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(valiableShortLinkDO.getDomain())
                    .shortUri(valiableShortLinkDO.getShortUri())
                    .favicon(valiableShortLinkDO.getFavicon())
                    .createdType(valiableShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDate(requestParam.getValidDate())
                    .validDateType(requestParam.getValidDateType())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rlock = readWriteLock.writeLock();
            try {
                if (!rlock.tryLock(3, 10, TimeUnit.SECONDS)) {
                    throw new ServiceException("短链接正在被访问，请稍后再试...");
                }
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, valiableShortLinkDO.getGid())
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getEnableStatus, 0)
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .set(ShortLinkDO::getDelFlag, 1)
                        .set(ShortLinkDO::getDelTime, LocalDateTime.now());
                baseMapper.update(null, linkUpdateWrapper);
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(defaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(valiableShortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(valiableShortLinkDO.getShortUri())
                        .enableStatus(valiableShortLinkDO.getEnableStatus())
                        .totalPv(valiableShortLinkDO.getTotalPv())
                        .totalUv(valiableShortLinkDO.getTotalUv())
                        .totalUip(valiableShortLinkDO.getTotalUip())
                        .fullShortUrl(valiableShortLinkDO.getFullShortUrl())
                        .favicon(getFaviconUrl(requestParam.getOriginUrl()))
                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO);
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, valiableShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("获取短链接锁失败，请重试");
            } finally {
                rlock.unlock();
            }
        }
        if (!Objects.equals(valiableShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(valiableShortLinkDO.getValidDate(), requestParam.getValidDate())
                || !Objects.equals(valiableShortLinkDO.getOriginUrl(), requestParam.getOriginUrl())) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            if (valiableShortLinkDO.getValidDate() != null && valiableShortLinkDO.getValidDate().isBefore(LocalDateTime.now())) {
                if (Objects.equals(requestParam.getValidDateType(), ValidDateType.PERMANENT.getType()) || requestParam.getValidDate().isAfter(LocalDateTime.now())) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }

        @Override
    public void restoreUri (String shortUri, ServletRequest request, ServletResponse response) throws IOException {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + serverPort + "/" + shortUri;
        String originUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originUrl)) {
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, null, statsRecord);
            ((HttpServletResponse)response).sendRedirect(originUrl);
            return;
        }
        if (!shortUrlCreateCachePenetrationBloomFilter.contains(fullShortUrl)) {
            ((HttpServletResponse)response).sendRedirect("/notfound/page");
            return;
        }
        String gotoIsNullSHortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullSHortLink)) {
            ((HttpServletResponse)response).sendRedirect("/notfound/page");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try {
            originUrl = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originUrl)) {
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStats(fullShortUrl, null, statsRecord);
                ((HttpServletResponse)response).sendRedirect(originUrl);
                return;
            }
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            if (shortLinkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/notfound/page");
                return;
            }
            LambdaQueryWrapper<ShortLinkDO> queryWrapper1 = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper1);
            if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().isBefore(LocalDateTime.now()))) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/notfound/page");
                return;
            }
            stringRedisTemplate.opsForValue()
                    .set(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), shortLinkDO.getOriginUrl(), LinkUtil.getCacheExpireTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), statsRecord);
            ((HttpServletResponse)response).sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean(false);
        AtomicReference<String> uv = new AtomicReference<>();
        Cookie[] cookies = ((HttpServletRequest)request).getCookies();

        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse)response).addCookie(uvCookie);
            RedisUvAdd(fullShortUrl, uvFirstFlag, uv);
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        RedisUvAdd(fullShortUrl, uvFirstFlag, uv);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String Ip = LinkUtil.getClientIp((HttpServletRequest) request);
        String os = LinkUtil.getOSInfo((HttpServletRequest) request);
        String browser = LinkUtil.getBrowser((HttpServletRequest) request);
        String device = LinkUtil.getDevice((HttpServletRequest) request);
        String network = LinkUtil.getNetwork((HttpServletRequest) request);
        boolean uipFirstFlag = RedisUipAdd(fullShortUrl, (HttpServletRequest) request);
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(Ip)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();
    }

    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }


    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortLink())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable e) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }



    private void RedisUvAdd(String fullShortUrl, AtomicBoolean uvFirstFlag, AtomicReference<String> uv) {
        Long addResult = stringRedisTemplate.opsForSet().add(String.format(SHORT_LINK_ACCESS_UV_KEY, fullShortUrl), uv.get());
        boolean isNew = addResult != null && addResult > 0L;
        uvFirstFlag.set(isNew);
        if (isNew) {
            long secondsTOMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
            stringRedisTemplate.expire(String.format(SHORT_LINK_ACCESS_UV_KEY, fullShortUrl), Duration.ofSeconds(secondsTOMidnight));
        }
    }

    private Boolean RedisUipAdd(String fullShortUrl, HttpServletRequest request) {
        String Ip = LinkUtil.getClientIp(request);
        Long addResult = stringRedisTemplate.opsForSet().add(String.format(SHORT_LINK_ACCESS_UIP_KEY, fullShortUrl), Ip);
        boolean uipFirstFlag = addResult != null && addResult > 0L;
        if (uipFirstFlag) {
            long secondsTOMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).getSeconds();
            stringRedisTemplate.expire(String.format(SHORT_LINK_ACCESS_UV_KEY, fullShortUrl), Duration.ofSeconds(secondsTOMidnight));
        }
        return uipFirstFlag;
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int customGenerateCount = 0;
        String shortUrl;
        while (true) {
            if (customGenerateCount > 10) {
                throw new ServiceException("短链接频繁生产，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl += UUID.randomUUID().toString();
            shortUrl = HashUtil.hashToBase62(originUrl);
            if (!shortUrlCreateCachePenetrationBloomFilter.contains(defaultDomain + "/" + shortUrl)) {
                break;
            }
            customGenerateCount++;
        }
        return shortUrl;
    }

    private String getFaviconUrl(String url) {
        try {
            // 请求页面
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            // 查找 rel="icon" 或 "shortcut icon"
            Element iconLink = doc.selectFirst("link[rel~=(?i)^(shortcut icon|icon)]");
            if (iconLink != null) {
                return iconLink.attr("abs:href");
            }

            // 如果找不到，则默认返回 /favicon.ico
            return url.endsWith("/") ? url + "favicon.ico" : url + "/favicon.ico";

        } catch (Exception e) {
            return "/default-favicon.icon";
        }
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = baseProperties.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details   = baseProperties.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("为避免恶意攻击，请使用一下网址生成跳转链接" + baseProperties.getNames());
        }
    }
}
