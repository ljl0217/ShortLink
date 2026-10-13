package com.nageoffer.shortlink.project.service;

/**
 * URL 标题获取接口层
 */
public interface UrlTitleService {
    /**
     * URL 标题获取
     * @param url 网址
     * @return 标题
     */
    String getUrlTitle(String url);
}
