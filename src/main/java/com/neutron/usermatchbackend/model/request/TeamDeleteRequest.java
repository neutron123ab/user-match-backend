package com.neutron.usermatchbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/4/4 19:56
 */
@Data
public class TeamDeleteRequest implements Serializable {
    private static final long serialVersionUID = -4260946386822499232L;

    private long teamId;
}
