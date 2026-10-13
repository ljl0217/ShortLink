package com.nageoffer.shortlink.admin.remote.dto.rep;

import lombok.Data;

@Data
public class RecycleBinRecoverReqDTO {
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
