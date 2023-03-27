package com.neutron.usermatchbackend.service;

import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neutron.usermatchbackend.model.request.UserLoginRequest;
import com.neutron.usermatchbackend.model.request.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author zzs
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2023-03-21 15:12:31
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册实体类（账号、密码、校验密码、公司编号）
     * @return 新用户id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录实体类（账号、密码）
     * @param request 请求
     * @return 用户脱敏信息
     */
    UserDTO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 用户信息脱敏
     *
     * @param user 用户信息
     * @return 用户脱敏信息
     */
    UserDTO getSafetyUser(User user);
}
