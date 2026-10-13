package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.rep.RecycleBinPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService {

    /**
     * 回收站分页
     * @param requestParam 请求分页大小
     * @return 返回分页数据
     */
    Result<Page<ShortLinkPageRespDTO>> pageRecycleBin(RecycleBinPageReqDTO requestParam);
}
