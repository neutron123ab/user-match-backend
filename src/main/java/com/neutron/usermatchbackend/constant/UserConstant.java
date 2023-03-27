package com.neutron.usermatchbackend.constant;

/**
 * @author zzs
 * @date 2023/3/21 21:32
 */
public final class UserConstant {

    private UserConstant() {
    }

    /**
     * 用户账号最小长度
     */
    public static final int USER_ACCOUNT_MIN_LEN = 4;

    /**
     * 用户密码最小长度
     */
    public static final int USER_PASSWORD_MIN_LEN = 8;

    /**
     * 用户公司编号最小长度
     */
    public static final int USER_CODE_MIN_LEN = 1;

    /**
     * 用户公司编号最大长度
     */
    public static final int USER_CODE_MAX_LEN = 5;

    /**
     * 用户登录态键
     */
    public static final String USER_LOGIN_STATE = "userLoginState";

    /**
     * 管理员权限
     */
    public static final int ADMIN_ROLE = 1;


}
