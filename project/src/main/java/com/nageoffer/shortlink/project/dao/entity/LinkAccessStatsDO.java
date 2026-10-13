package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.project.common.database.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 基础访问点击量
 *
 * @author makejava
 * @since 2025-07-23 20:52:11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_link_access_stats")
public class LinkAccessStatsDO extends BaseDO {
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
    private Integer pv;
    /**
     * 独立访客数
     */
    private Integer uv;
    /**
     * 独立IP数
     */
    private Integer uip;
    /**
     * 小时
     */
    private Integer hour;
    /**
     * 星期
     */
    private Integer weekday;
}

