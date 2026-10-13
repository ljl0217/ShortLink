package com.nageoffer.shortlink.project.dto.resp;

import lombok.Data;

/**
 * 短链接分组数量查询
 */
@Data
public class ShortLinkGroupCountQueryRespDTO {
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 分组数量
     */
    private Integer shortLinkCount;
}
