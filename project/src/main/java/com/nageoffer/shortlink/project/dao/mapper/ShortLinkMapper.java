package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.rep.RecycleBinPageReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkPageRepDTO;
import com.nageoffer.shortlink.project.toolkit.BigIntToLocalDateTimeHandler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;

/**
 * 短链接数据持久层
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    /**
     * 短链接访问统计自增
     */
    void incrementStats(@Param("gid") String gid,
                        @Param("fullShortUrl") String fullShortUrl,
                        @Param("totalPv") Integer totalPv,
                        @Param("totalUv") Integer totalUv,
                        @Param("totalUip") Integer totalUip);

    /**
     * 分页统计短链接
     */
    @Result(column = "del_time", property = "delTime", typeHandler = BigIntToLocalDateTimeHandler.class)
    IPage<ShortLinkDO> pageLink(ShortLinkPageRepDTO requestParam);

    /**
     * 分页统计回收站短链接
     */
    IPage<ShortLinkDO> pageRecycleBinLink(RecycleBinPageReqDTO requestParam);
}
