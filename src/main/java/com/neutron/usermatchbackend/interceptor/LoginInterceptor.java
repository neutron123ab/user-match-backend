package com.neutron.usermatchbackend.interceptor;

import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.neutron.usermatchbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zzs
 * @date 2023/4/3 15:30
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return true;
    }
}
