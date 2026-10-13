package com.nageoffer.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nageoffer.shortlink.admin.common.database.BaseDO;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * (TUser)实体类
 *
 * @author makejava
 * @since 2025-06-08 14:28:18
 */
@TableName("t_user")
@Data
public class UserDO extends BaseDO {
    /**
     * ID
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 真实姓名
     */
    private String realName;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱
     */
    private String mail;
    /**
     * 注销时间戳
     */
    private LocalDateTime deletionTime;

}

