package com.neutron.usermatchbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/3/21 19:56
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -8457843435886734984L;
    private String userAccount;

    private String userPassword;

    private String checkPassword;

    private String userCode;

}
