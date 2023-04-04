package com.neutron.usermatchbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/4/4 19:45
 */
@Data
public class TeamQuitRequest implements Serializable {
    private static final long serialVersionUID = 5146853208872548996L;

    private long teamId;
}
