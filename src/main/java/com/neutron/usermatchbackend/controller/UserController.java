package com.neutron.usermatchbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neutron.usermatchbackend.common.BaseResponse;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.common.ResultUtils;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.request.UserLoginRequest;
import com.neutron.usermatchbackend.model.request.UserRegisterRequest;
import com.neutron.usermatchbackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

import static com.neutron.usermatchbackend.constant.UserConstant.ADMIN_ROLE;
import static com.neutron.usermatchbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zzs
 * @date 2023/3/21 15:58
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegisterController(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = BeanUtil.hasNullField(userRegisterRequest);
        if (flag) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        long userId = userService.userRegister(userRegisterRequest);

        return ResultUtils.success(userId, "注册成功");
    }

    @PostMapping("/login")
    public BaseResponse<UserDTO> userLoginController(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = BeanUtil.hasNullField(userLoginRequest);
        if (flag) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        UserDTO userDTO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userDTO, "登录成功");
    }

    @GetMapping("/getUserInfo")
    public BaseResponse<UserDTO> getUserInfoController(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserDTO userInfo = (UserDTO) userObj;
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return ResultUtils.success(userInfo);
    }

    @GetMapping("/search")
    public BaseResponse<List<UserDTO>> searchUsers(String username, HttpServletRequest request) {
        if(!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(CharSequenceUtil.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> list = userService.list(queryWrapper);
        List<UserDTO> userList = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());

        return ResultUtils.success(userList);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if(!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean flag = userService.removeById(id);
        return ResultUtils.success(flag);
    }

    /**
     * 判断是否为管理员
     *
     * @param request 请求
     * @return  true：是管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        UserDTO userInfo = (UserDTO) userObj;
        return userInfo != null && userInfo.getUserRole() == ADMIN_ROLE;
    }


}
