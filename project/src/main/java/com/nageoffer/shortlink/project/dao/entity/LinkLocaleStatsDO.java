package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 地区访问量
 *
 * @author makejava
 * @since 2025-07-28 22:20:51
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_link_locale_stats")
public class LinkLocaleStatsDO extends BaseDO {
    /**
     * ID
     */
    private Long id;
    /**
     * 完整短链接
     */
    private String fullShortUrl;
    /**
     * 日期
     */
    private LocalDate date;
    /**
     * 访问量
     */
    private Integer cnt;
    /**
     * 省份名称
     */
    private String province;
    /**
     * 市名称
     */
    private String city;
    /**
     * 城市编码
     */
    private String adcode;
    /**
     * 国家标识
     */
    private String country;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;

}

