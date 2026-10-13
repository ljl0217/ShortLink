package com.nageoffer.shortlink.project.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 短链接创建请求返回参数
 */
@Data
@Builder
public class ShortLinkCreateRespDTO {

    /**
     * 短链接分组ID
     */
    private String gid;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 完整短链接
     */
    private String fullShortLink;
}
