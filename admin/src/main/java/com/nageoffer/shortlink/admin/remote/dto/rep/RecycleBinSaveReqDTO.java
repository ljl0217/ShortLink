package com.nageoffer.shortlink.admin.remote.dto.rep;

import lombok.Data;

@Data
public class RecycleBinSaveReqDTO {
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 短链接完整
     */
    private String fullShortUrl;
}
