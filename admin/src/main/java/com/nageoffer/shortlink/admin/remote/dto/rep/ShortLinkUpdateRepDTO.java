package com.nageoffer.shortlink.admin.remote.dto.rep;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ShortLinkUpdateRepDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 原始分组标识
     */
    private String originGid;
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 有效期类型 0：永久有效 1：自定义
     */
    private Integer validDateType;
    /**
     * 有效期
     */
    private LocalDate validDate;
    /**
     * 描述
     */
    @TableField("`describe`")
    private String describe;

}
