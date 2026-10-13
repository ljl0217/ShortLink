package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dto.rep.UserLoginRepDTO;
import com.nageoffer.shortlink.admin.dto.rep.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.rep.UserUpdateRepDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名查找用户信息
     * @param username 用户名
     * @return 返回用户实体信息
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户是否存在
     * @param username 用户名
     * @return 用户存在：true 用户不存在：false
     */
    Boolean hasUsername(String username);

    /**
     * 用户注册
     * @param requestParam 用户注册参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户信息
     * @param requestParam 用户修改参数
     */
    void update(UserUpdateRepDTO requestParam);

    /**
     * 用户登录
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 token
     */
    UserLoginRespDTO login(UserLoginRepDTO requestParam);

    /**
     * 检查用户是否登录
     * @param username 用户名
     * @param token 用户token
     * @return 用户是否登录
     */
    Boolean checkLogin(String username, String token);

    /**
     * 退出登录
     * @param username 用户名
     * @param token 用户token
     */
    void logout(String username, String token);
}
