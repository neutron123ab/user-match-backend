package com.neutron.usermatchbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/3/22 16:06
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 1933960119582265550L;

    private String userAccount;

    private String userPassword;
}
