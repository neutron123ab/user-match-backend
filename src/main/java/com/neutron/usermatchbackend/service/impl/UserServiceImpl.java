package com.neutron.usermatchbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.request.UserLoginRequest;
import com.neutron.usermatchbackend.model.request.UserRegisterRequest;
import com.neutron.usermatchbackend.service.UserService;
import com.neutron.usermatchbackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.neutron.usermatchbackend.constant.UserConstant.*;

/**
 * @author zzs
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-03-21 15:12:31
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private static final String SALT = "neutron";

    private static final String VALID_PATTERN = "^[a-zA-Z0-9]+$";

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        //1.接收到的参数是否为空
        if (BeanUtil.hasNullField(userRegisterRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        //2.账号长度 >= 4
        String userAccount = userRegisterRequest.getUserAccount();
        if(userAccount.length() < USER_ACCOUNT_MIN_LEN){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        //3.密码/校验密码长度 >= 8
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if(userPassword.length() < USER_PASSWORD_MIN_LEN
                || checkPassword.length() < USER_PASSWORD_MIN_LEN){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        //4.公司编号1-5位
        String userCode = userRegisterRequest.getUserCode();
        if(userCode.length() < USER_CODE_MIN_LEN || userCode.length() > USER_CODE_MAX_LEN){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户公司编号长度不在区间[1, 5]内");
        }
        //6.账号不能包含特殊字符
        Pattern pattern = Pattern.compile(VALID_PATTERN);
        Matcher matcher = pattern.matcher(userRegisterRequest.getUserAccount());
        if(!matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号不能包含特殊字符");
        }
        //7.判断密码与校验密码是否相同
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        //8.查数据库，账号不能重复
        Long count = query().eq("user_account", userAccount).count();
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账户已存在");
        }
        //9.查数据库，公司编号不能重复
        Long codeCount = query().eq("user_code", userCode).count();
        if(codeCount > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该公司编号已存在");
        }
        //10.密码加密
        String encodedPassword = SecureUtil.md5(userPassword + SALT);
        //11.插入数据库
        User user = new User();
        user.setUsername("user_"+ IdUtil.simpleUUID());
        user.setUserAccount(userAccount);
        user.setUserPassword(encodedPassword);
        user.setUserCode(userCode);
        boolean save = save(user);
        if(!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入数据库失败");
        }
        //12.返回新用户id

        return user.getId();
    }

    @Override
    public UserDTO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {

        //1.非空
        if (BeanUtil.hasNullField(userLoginRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        //2.账号长度不小于4位
        String userAccount = userLoginRequest.getUserAccount();
        if(userAccount.length() < USER_ACCOUNT_MIN_LEN){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        //3.密码长度不小于8位
        String userPassword = userLoginRequest.getUserPassword();
        if(userPassword.length() < USER_PASSWORD_MIN_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //4.账号不包含特殊字符
        Pattern pattern = Pattern.compile(VALID_PATTERN);
        Matcher matcher = pattern.matcher(userAccount);
        if(!matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能包含特殊字符");
        }
        //5.密码比对
        String encodedPassword = SecureUtil.md5(userPassword + SALT);
        User user = query().eq("user_password", encodedPassword)
                .eq("user_account", userAccount)
                .one();
        if(BeanUtil.isEmpty(user)){
            log.info("user login failed, userAccount cannot match password");
            throw new BusinessException(ErrorCode.NULL_ERROR, "找不到该用户");
        }
        //6.用户信息脱敏
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        //7.记录用户登录态（session）
        request.getSession().setAttribute(USER_LOGIN_STATE, userDTO);
        //8.返回脱敏后的用户信息
        return userDTO;
    }

    @Override
    public UserDTO getSafetyUser(User user) {
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        return userDTO;
    }


}




