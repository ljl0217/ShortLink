package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.rep.RecycleBinPageReqDTO;
import com.nageoffer.shortlink.project.dto.rep.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.rep.RecycleBinRemoveReqDTO;
import com.nageoffer.shortlink.project.dto.rep.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 回收站接口层
 */
public interface RecycleBinService extends IService<ShortLinkDO> {
    /**
     * 回收站保存
     * @param requestParam 请求参数
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    /**
     * 回收站分页
     * @param requestParam 请求分页数据
     * @return 分页列表
     */
    IPage<ShortLinkPageRespDTO> pageRecycleBin(RecycleBinPageReqDTO requestParam);

    /**
     * 回收站恢复
     * @param requestParam 恢复短链接参数
     */
    void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam);

    /**
     * 移除短链接
     * @param requestParam 移除短链接参数
     */
    void removeRecycleBin(RecycleBinRemoveReqDTO requestParam);
}
