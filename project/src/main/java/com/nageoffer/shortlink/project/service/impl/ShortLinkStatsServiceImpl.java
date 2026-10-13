package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkGroupStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkGroupStatsReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkStatsReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 短链接监控接口实现层
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkGroupMapper linkGroupMapper;

    @Override
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(requestParam);
        if (CollUtil.isEmpty(listStatsByShortLink)) {
            return null;
        }
        //基础访问数据
        LinkAccessStatsDO pvUvUidStatsByShortLink = linkAccessLogsMapper.findPvUvUidStatsByShortLink(requestParam);
        //基础访问详情
        Map<String, LinkAccessStatsDO> StatsMap = listStatsByShortLink.stream()
                .collect(Collectors.toMap(item -> item.getDate().toString(), Function.identity()));
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()),
                        DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH)
                .stream()
                .map(date -> DateUtil.format(date, "yyyy-MM-dd"))
                .toList();
        System.out.println(requestParam.getStartDate());
        System.out.println(rangeDates);
        List<ShortLinkStatsAccessDailyRespDTO> dailyRespDTOList = rangeDates.stream()
                .map(data -> {
                    LinkAccessStatsDO statsDO = StatsMap.get(data);
                    return ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(data)
                            .pv(statsDO != null ? statsDO.getPv() : 0)
                            .uv(statsDO != null ? statsDO.getUv() : 0)
                            .uip(statsDO != null ? statsDO.getUip() : 0)
                            .build();
                })
                .toList();
        //地区访问详情
        List<ShortLinkStatsLocaleCNRespDTO> localeCNRespDTOS = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocalByShortLink = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);
        int localeCntSum = listedLocalByShortLink.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt())
                        .orElse(0))
                .sum();
        if (localeCntSum > 0) {
            localeCNRespDTOS = listedLocalByShortLink.stream()
                    .map(each -> {
                        double radio = (double) each.getCnt() / localeCntSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsLocaleCNRespDTO.builder()
                                .cnt(each.getCnt())
                                .locale(each.getProvince())
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //小时访问详情
        List<LinkAccessStatsDO> listedHourStatsByShortLink = linkAccessStatsMapper.listHourStatsByShortLink(requestParam);
        Map<Integer, LinkAccessStatsDO> hourStatsMap = listedHourStatsByShortLink.stream()
                .collect(Collectors.toMap(LinkAccessStatsDO::getHour, Function.identity()));
        List<Integer> hourStats = IntStream.range(0, 24)
                .map(i -> Optional.ofNullable(hourStatsMap.get(i)).map(LinkAccessStatsDO::getPv).orElse(0))
                .boxed()
                .toList();
        //高频Ip访问详情
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(requestParam);
        List<ShortLinkStatsTopIpRespDTO> topIpRespDTOS = listTopIpByShortLink.stream()
                .map(each ->
                    ShortLinkStatsTopIpRespDTO.builder()
                            .ip(each.get("ip").toString())
                            .cnt(Integer.parseInt(each.get("count").toString()))
                            .build())
                .toList();
        //一周访问详情
        List<LinkAccessStatsDO> listWeekdaysStatsByShortLink = linkAccessStatsMapper.listWeekdayStatsByShortLink(requestParam);
        Map<Integer, LinkAccessStatsDO> weekdaysStatsMap = listWeekdaysStatsByShortLink.stream()
                .collect(Collectors.toMap(LinkAccessStatsDO::getWeekday, Function.identity()));
        List<Integer> weekdaysStats = IntStream.range(0, 8)
                .map(i -> Optional.ofNullable(weekdaysStatsMap.get(i)).map(LinkAccessStatsDO::getPv).orElse(0))
                .boxed()
                .toList();
        //浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByShortLink = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        int browserSum = listBrowserStatsByShortLink.stream()
                .mapToInt(each -> Optional.ofNullable(each.get("count"))
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElse(0))
                .sum();
        if (browserSum > 0) {
            browserRespDTOS = listBrowserStatsByShortLink.stream()
                    .map(each -> {
                        int count = Optional.ofNullable(each.get("count"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(0);
                        double radio = (double) count / browserSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsBrowserRespDTO.builder()
                                .cnt(count)
                                .browser(Optional.ofNullable(each.get("browser")).map(Object::toString).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //操作系统访问详情
        List<ShortLinkStatsOsRespDTO> statsOsRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        int osSum = listOsStatsByShortLink.stream()
                .mapToInt(each -> Optional.ofNullable(each.get("count"))
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElse(0))
                .sum();
        if (osSum > 0) {
            statsOsRespDTOS = listOsStatsByShortLink.stream()
                    .map(each -> {
                        int count = Optional.ofNullable(each.get("count"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(0);
                        double radio = (double) count / osSum;
                        BigDecimal actual = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsOsRespDTO.builder()
                                .cnt(count)
                                .os(Optional.ofNullable(each.get("os")).map(Object::toString).orElse("Unknown"))
                                .ratio(actual)
                                .build();
                    })
                    .toList();
        }
        //访客访问详情
        List<ShortLinkStatsUvRespDTO> uvRespDTOS = new ArrayList<>();
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Optional.ofNullable(findUvTypeByShortLink)
                .map(each -> each.get("oldUserCnt"))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);
        int newUserCnt = Optional.ofNullable(findUvTypeByShortLink)
                .map(each -> each.get("newUserCnt"))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);
        int uvSum = oldUserCnt + newUserCnt;
        double oldRadio = (double) oldUserCnt / uvSum;
        BigDecimal actualOldRadio = BigDecimal.valueOf(oldRadio).setScale(2, RoundingMode.HALF_UP);
        double newRadio = (double) newUserCnt / uvSum;
        BigDecimal actualNewRadio = BigDecimal.valueOf(newRadio).setScale(2, RoundingMode.HALF_UP);
        ShortLinkStatsUvRespDTO newUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(actualNewRadio)
                .build();
        uvRespDTOS.add(newUvRespDTO);
        ShortLinkStatsUvRespDTO oldUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(actualOldRadio)
                .build();
        uvRespDTOS.add(oldUvRespDTO);
        //访问设备详情
        List<ShortLinkStatsDeviceRespDTO> deviceRespDTOS = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int deviceSum = listDeviceStatsByShortLink.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt()).orElse(0))
                .sum();
        if (deviceSum > 0) {
            deviceRespDTOS = listDeviceStatsByShortLink.stream()
                    .map(each -> {
                        int cnt = Optional.ofNullable(each.getCnt())
                                .orElse(0);
                        double radio = (double) cnt / deviceSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsDeviceRespDTO.builder()
                                .cnt(cnt)
                                .device(Optional.ofNullable(each.getDevice()).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkRespDTOS = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt()).orElse(0))
                .sum();
        if (networkSum > 0) {
            networkRespDTOS = listNetworkStatsByShortLink.stream()
                    .map(each -> {
                        int cnt = Optional.ofNullable(each.getCnt())
                                .orElse(0);
                        double radio = (double) cnt / networkSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsNetworkRespDTO.builder()
                                .cnt(cnt)
                                .network(Optional.ofNullable(each.getNetwork()).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        return ShortLinkStatsRespDTO.builder()
                .pv(pvUvUidStatsByShortLink.getPv())
                .uv(pvUvUidStatsByShortLink.getUv())
                .uip(pvUvUidStatsByShortLink.getUip())
                .daily(dailyRespDTOList)
                .localeCnStats(localeCNRespDTOS)
                .browserStats(browserRespDTOS)
                .deviceStats(deviceRespDTOS)
                .hourStats(hourStats)
                .networkStats(networkRespDTOS)
                .osStats(statsOsRespDTOS)
                .topIpStats(topIpRespDTOS)
                .uvTypeStats(uvRespDTOS)
                .weekdayStats(weekdaysStats)
                .build();
    }

    @Override
    public ShortLinkStatsRespDTO groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        List<LinkAccessStatsDO> listStatsByGroup = linkAccessStatsMapper.listStatsByGroup(requestParam);
        if (CollUtil.isEmpty(listStatsByGroup)) {
            return null;
        }
        //基础访问数据
        LinkAccessStatsDO pvUvUidStatsByGroup = linkAccessLogsMapper.findPvUvUidStatsByGroup(requestParam);
        //基础访问详情
        Map<String, LinkAccessStatsDO> StatsMap = listStatsByGroup.stream()
                .collect(Collectors.toMap(item -> item.getDate().toString(), Function.identity()));
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()),
                DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH)
                .stream()
                .map(date -> DateUtil.format(date, "yyyy-MM-dd"))
                .toList();
        List<ShortLinkStatsAccessDailyRespDTO> dailyRespDTOList = rangeDates.stream()
                .map(data -> {
                    LinkAccessStatsDO statsDO = StatsMap.get(data);
                    return ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(data)
                            .pv(statsDO != null ? statsDO.getPv() : 0)
                            .uv(statsDO != null ? statsDO.getUv() : 0)
                            .uip(statsDO != null ? statsDO.getUip() : 0)
                            .build();
                })
                .toList();
        //地区访问详情
        List<ShortLinkStatsLocaleCNRespDTO> localeCNRespDTOS = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocalByGroup = linkLocaleStatsMapper.listLocaleByGroup(requestParam);
        int localeCntSum = listedLocalByGroup.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt())
                        .orElse(0))
                .sum();
        if (localeCntSum > 0) {
            localeCNRespDTOS = listedLocalByGroup.stream()
                    .map(each -> {
                        double radio = (double) each.getCnt() / localeCntSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsLocaleCNRespDTO.builder()
                                .cnt(each.getCnt())
                                .locale(each.getProvince())
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //小时访问详情
        List<LinkAccessStatsDO> listedHourStatsByGroup = linkAccessStatsMapper.listHourStatsByGroup(requestParam);
        Map<Integer, LinkAccessStatsDO> hourStatsMap = listedHourStatsByGroup.stream()
                .collect(Collectors.toMap(LinkAccessStatsDO::getHour, Function.identity()));
        List<Integer> hourStats = IntStream.range(0, 24)
                .map(i -> Optional.ofNullable(hourStatsMap.get(i)).map(LinkAccessStatsDO::getPv).orElse(0))
                .boxed()
                .toList();
        //高频Ip访问详情
        List<HashMap<String, Object>> listTopIpByGroup = linkAccessLogsMapper.listTopIpByGroup(requestParam);
        List<ShortLinkStatsTopIpRespDTO> topIpRespDTOS = listTopIpByGroup.stream()
                .map(each ->
                        ShortLinkStatsTopIpRespDTO.builder()
                                .ip(each.get("ip").toString())
                                .cnt(Integer.parseInt(each.get("count").toString()))
                                .build())
                .toList();
        //一周访问详情
        List<LinkAccessStatsDO> listWeekdaysStatsByGroup = linkAccessStatsMapper.listWeekdayStatsByGroup(requestParam);
        Map<Integer, LinkAccessStatsDO> weekdaysStatsMap = listWeekdaysStatsByGroup.stream()
                .collect(Collectors.toMap(LinkAccessStatsDO::getWeekday, Function.identity()));
        List<Integer> weekdaysStats = IntStream.range(0, 8)
                .map(i -> Optional.ofNullable(weekdaysStatsMap.get(i)).map(LinkAccessStatsDO::getPv).orElse(0))
                .boxed()
                .toList();
        //浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByGroup = linkBrowserStatsMapper.listBrowserStatsByGroup(requestParam);
        int browserSum = listBrowserStatsByGroup.stream()
                .mapToInt(each -> Optional.ofNullable(each.get("count"))
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElse(0))
                .sum();
        if (browserSum > 0) {
            browserRespDTOS = listBrowserStatsByGroup.stream()
                    .map(each -> {
                        int count = Optional.ofNullable(each.get("count"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(0);
                        double radio = (double) count / browserSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsBrowserRespDTO.builder()
                                .cnt(count)
                                .browser(Optional.ofNullable(each.get("browser")).map(Object::toString).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //操作系统访问详情
        List<ShortLinkStatsOsRespDTO> statsOsRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByGroup = linkOsStatsMapper.listOsStatsByGroup(requestParam);
        int osSum = listOsStatsByGroup.stream()
                .mapToInt(each -> Optional.ofNullable(each.get("count"))
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElse(0))
                .sum();
        if (osSum > 0) {
            statsOsRespDTOS = listOsStatsByGroup.stream()
                    .map(each -> {
                        int count = Optional.ofNullable(each.get("count"))
                                .map(Object::toString)
                                .map(Integer::parseInt)
                                .orElse(0);
                        double radio = (double) count / osSum;
                        BigDecimal actual = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsOsRespDTO.builder()
                                .cnt(count)
                                .os(Optional.ofNullable(each.get("os")).map(Object::toString).orElse("Unknown"))
                                .ratio(actual)
                                .build();
                    })
                    .toList();
        }

        //访问设备详情
        List<ShortLinkStatsDeviceRespDTO> deviceRespDTOS = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByGroup = linkDeviceStatsMapper.listDeviceStatsByGroup(requestParam);
        int deviceSum = listDeviceStatsByGroup.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt()).orElse(0))
                .sum();
        if (deviceSum > 0) {
            deviceRespDTOS = listDeviceStatsByGroup.stream()
                    .map(each -> {
                        int cnt = Optional.ofNullable(each.getCnt())
                                .orElse(0);
                        double radio = (double) cnt / deviceSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsDeviceRespDTO.builder()
                                .cnt(cnt)
                                .device(Optional.ofNullable(each.getDevice()).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        //访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkRespDTOS = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByGroup = linkNetworkStatsMapper.listNetworkStatsByGroup(requestParam);
        int networkSum = listNetworkStatsByGroup.stream()
                .mapToInt(each -> Optional.ofNullable(each.getCnt()).orElse(0))
                .sum();
        if (networkSum > 0) {
            networkRespDTOS = listNetworkStatsByGroup.stream()
                    .map(each -> {
                        int cnt = Optional.ofNullable(each.getCnt())
                                .orElse(0);
                        double radio = (double) cnt / networkSum;
                        BigDecimal actualRadio = BigDecimal.valueOf(radio).setScale(2, RoundingMode.HALF_UP);
                        return ShortLinkStatsNetworkRespDTO.builder()
                                .cnt(cnt)
                                .network(Optional.ofNullable(each.getNetwork()).orElse("Unknown"))
                                .ratio(actualRadio)
                                .build();
                    })
                    .toList();
        }
        return ShortLinkStatsRespDTO.builder()
                .pv(pvUvUidStatsByGroup.getPv())
                .uv(pvUvUidStatsByGroup.getUv())
                .uip(pvUvUidStatsByGroup.getUip())
                .daily(dailyRespDTOList)
                .localeCnStats(localeCNRespDTOS)
                .browserStats(browserRespDTOS)
                .deviceStats(deviceRespDTOS)
                .hourStats(hourStats)
                .networkStats(networkRespDTOS)
                .osStats(statsOsRespDTOS)
                .topIpStats(topIpRespDTOS)
                .weekdayStats(weekdaysStats)
                .build();
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        LambdaQueryWrapper<LinkAccessLogsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
                .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                .between(LinkAccessLogsDO::getCreateTime, requestParam.getStartDate(), requestParam.getEndDate())
                .eq(LinkAccessLogsDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogsDO::getCreateTime);
        IPage<LinkAccessLogsDO> recordRespDTOIPage = linkAccessLogsMapper.selectPage(requestParam, queryWrapper);
        if (CollUtil.isEmpty(recordRespDTOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> result = recordRespDTOIPage
                .convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogList = result.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        if (CollUtil.isEmpty(userAccessLogList)) {
            return result;
        }
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectUvTypeByUsers(
                requestParam.getGid(),
                requestParam.getFullShortUrl(),
                requestParam.getEnableStatus(),
                requestParam.getStartDate().toString(),
                requestParam.getEndDate().toString(),
                userAccessLogList);
        Map<String, String> uvTypeMap = uvTypeList.stream()
                .collect(Collectors.toMap(item -> item.get("user").toString(),
                        item -> item.get("uvType").toString(),
                        (existing, replacement) -> existing));
        result.getRecords().forEach(each -> {
            String uvType = uvTypeMap.getOrDefault(each.getUser(), "旧访客");
            each.setUvType(uvType);
        });
        return result;
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectGroupPage(requestParam);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>();
        }
        IPage<ShortLinkStatsAccessRecordRespDTO> recordRespDTOIPage = linkAccessLogsDOIPage
                .convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class));
        List<String> userAccessLogList = recordRespDTOIPage.getRecords().stream()
                .map(ShortLinkStatsAccessRecordRespDTO::getUser)
                .toList();
        List<Map<String, Object>> uvTypeList = linkAccessLogsMapper.selectGroupUvTypeByUsers(
                requestParam.getGid(),
                requestParam.getStartDate().toString(),
                requestParam.getEndDate().toString(),
                userAccessLogList);
        Map<String, String> uvTypeMap = uvTypeList.stream()
                .collect(Collectors.toMap(item -> item.get("user").toString(),
                        item -> item.get("uvType").toString(),
                        (existing, replacement) -> existing));
        recordRespDTOIPage.getRecords().forEach(each -> {
            String uvType = uvTypeMap.getOrDefault(each.getUser(), "旧访客");
            each.setUvType(uvType);
        });
        return recordRespDTOIPage;
    }
}
