package com.neutron.usermatchbackend.enums;

/**
 * @author zzs
 * @date 2023/4/1 21:30
 */
public enum TeamStatusEnum {

    /**
     * 队伍公开
     */
    PUBLIC(0, "公开"),
    /**
     * 队伍私有
     */
    PRIVATE(1, "私有"),
    /**
     * 队伍加密
     */
    SECRET(2, "加密");

    private final int value;

    private final String text;

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if(teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
