package com.neutron.usermatchbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/4/2 20:12
 */
@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = 1251912438342129396L;

    private Long teamId;

    private String password;
}
