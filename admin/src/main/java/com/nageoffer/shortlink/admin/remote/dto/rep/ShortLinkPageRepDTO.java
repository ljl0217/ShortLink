package com.nageoffer.shortlink.admin.remote.dto.rep;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 短链接分页请求参数
 */
@Data
public class ShortLinkPageRepDTO extends Page {

    /**
     * 短链接分组标识
     */
    private String gid;
    /**
     * 排序标识
     */
    private String orderTag;
}
