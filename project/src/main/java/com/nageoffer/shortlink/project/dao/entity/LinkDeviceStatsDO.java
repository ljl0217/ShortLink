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
 * 设备访问类
 *
 * @author makejava
 * @since 2025-07-30 21:13:31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_link_device_stats")
public class LinkDeviceStatsDO extends BaseDO {
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
     * 访问设备
     */
    private String device;
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

