package com.nageoffer.shortlink.admin.dto.rep;

import lombok.Data;

/**
 * 用户登录请求参数
 */
@Data
public class UserLoginRepDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
