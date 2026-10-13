package com.nageoffer.shortlink.admin.dto.rep;

import lombok.Data;

@Data
public class ShortLinkGroupSortRepDTO {

    /**
     * 短链接分组gid
     */
    private String gid;

    /**
     * 短链接分组排序
     */
    private Integer sortOrder;
}
