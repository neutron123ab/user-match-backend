package com.neutron.usermatchbackend.constant;

/**
 * @author zzs
 * @date 2023/4/2 14:03
 */
public final class TeamConstant {

    private TeamConstant() {
    }

    public static final Integer MAX_TEAM_NAME_LENGTH = 20;

    public static final Integer MAX_TEAM_DESCRIPTION_LENGTH = 512;

    public static final Integer MAX_TEAM_MEMBERS = 20;

    public static final Integer MIN_TEAM_MEMBERS = 1;

    public static final Integer MAX_TEAM_PASSWORD_LEN = 20;

    public static final Integer MIN_TEAM_PASSWORD_LEN = 6;

    public static final Integer MAX_TEAM_OWN = 5;
}
