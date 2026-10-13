package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkPageRepDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkUpdateRepDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 短链接服务层
 */
public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
     * 创建短链接
     * @param requestParam 短链接请求参数
     * @return 短链接请求返回参数
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     * 短链接分页
     * @param requestParam 请求参数
     * @return 分页
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageRepDTO requestParam);

    /**
     * 短链接分组数量查询
     * @param requestParam 请求分组标识
     * @return 返回分组数量
     */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

    /**
     * 修改短链接
     * @param requestParam 请求参数
     */
    void updateShortLink(ShortLinkUpdateRepDTO requestParam);

    /**
     * 短链接跳转
     * @param shortUri 短链接后缀
     * @param request 请求
     * @param response 响应
     */
    void restoreUri(String shortUri, ServletRequest request, ServletResponse response) throws IOException;

    /**
     * 批量创建短链接
     * @param requestParam 请求参数
     * @return 返回响应
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);
    /**
     * 短链接统计
     *
     * @param statsRecordDTO 短链接统计实体参数
     */
    void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecordDTO);
}
