package com.nageoffer.shortlink.project.dto.rep;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

import java.util.List;

@Data
public class RecycleBinPageReqDTO extends Page<ShortLinkDO> {
    /**
     * 分组标识列表
     */
    private List<String> gidList;
}
