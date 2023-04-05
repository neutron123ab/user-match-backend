package com.neutron.usermatchbackend.enums;

/**
 * @author zzs
 * @date 2023/4/5 15:49
 */
public enum MsgStatusEnum {

    /**
     * 私聊
     */
    SINGLE(0, "私聊"),

    /**
     * 群聊
     */
    GROUP(1, "群聊"),

    /**
     * 系统消息
     */
    SYSTEM(2, "系统消息");

    private final int value;

    private final String text;

    MsgStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static MsgStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        MsgStatusEnum[] values = MsgStatusEnum.values();
        for (MsgStatusEnum msgStatusEnum : values) {
            if(msgStatusEnum.getValue() == value) {
                return msgStatusEnum;
            }
        }
        return null;
    }
}
