package com.nageoffer.shortlink.project.dto.rep;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

/**
 * 短链接分页请求参数
 */
@Data
public class ShortLinkPageRepDTO extends Page<ShortLinkDO> {

    /**
     * 短链接分组标识
     */
    private String gid;
    /**
     * 排序标识
     */
    private String orderTag;
}
