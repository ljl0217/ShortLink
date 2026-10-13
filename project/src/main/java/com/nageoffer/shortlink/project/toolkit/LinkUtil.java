package com.nageoffer.shortlink.project.toolkit;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_EXPIRE_TIME;

public class LinkUtil {
    /**
     * 设置 redis 过期时间
     * @param ValidDate 创建时间
     * @return 过期时间
     */
    public static long getCacheExpireTime(LocalDateTime ValidDate) {
        return Optional.ofNullable(ValidDate)
                .map(each -> Duration.between(LocalDateTime.now(), each).toMillis())
                        .orElse(DEFAULT_EXPIRE_TIME);
    }

    /**
     * 获取请求IP
     * @param request 用户请求
     * @return 用户IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多个 IP 时取第一个
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 默认回退到 request.getRemoteAddr()
        ip = request.getRemoteAddr();

        // IPv6 本地地址转回 127.0.0.1
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 获取操心系统
     * @param request 用户请求
     * @return 用户操作系统
     */
    public static String getOSInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) return "Unknown OS";

        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "macOS";
        } else if (userAgent.toLowerCase().contains("x11")) {
            return "Unix";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("iphone")) {
            return "iOS";
        } else {
            return "Other";
        }
    }

    /**
     * 获取浏览器
     * @param request 用户请求
     * @return 浏览器
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown Browser";

        if (userAgent.contains("Edg/")) return "Microsoft Edge";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) return "Google Chrome";
        if (userAgent.contains("Firefox/")) return "Mozilla Firefox";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) return "Safari";
        if (userAgent.contains("Opera") || userAgent.contains("OPR/")) return "Opera";
        if (userAgent.contains("Trident/") || userAgent.contains("MSIE")) return "Internet Explorer";

        return "Other Browser";
    }

    /**
     * 获取设备种类
     * @param request 请求
     * @return 设备
     */
    public static String getDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        }
        return "PC";
    }

    public static String getNetwork(HttpServletRequest request) {
        String actualIp = getClientIp(request);
        return actualIp.startsWith("192.168.") || actualIp.startsWith("10.") ? "WIFI" : "Mobile";
    }

    /**
     * 获取原始链接中的域名
     * 如果原始链接包含 www 开头的话需要去掉
     *
     * @param url 创建或者修改短链接的原始链接
     * @return 原始链接中的域名
     */
    public static String extractDomain(String url) {
        String domain = null;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (StrUtil.isNotBlank(host)) {
                domain = host;
                if (domain.startsWith("www.")) {
                    domain = host.substring(4);
                }
            }
        } catch (Exception ignored) {
        }
        return domain;
    }
}
