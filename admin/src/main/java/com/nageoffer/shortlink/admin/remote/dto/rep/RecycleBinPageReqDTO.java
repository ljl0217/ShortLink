package com.nageoffer.shortlink.admin.remote.dto.rep;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
@Data
public class RecycleBinPageReqDTO extends Page {
    /**
     * 分组标识列表
     */
    private List<String> gidList;
}
