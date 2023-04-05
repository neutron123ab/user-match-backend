package com.neutron.usermatchbackend.model.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author zzs
 * @date 2023/4/5 15:47
 */
@Data
public class ChatInfo implements Serializable {
    private static final long serialVersionUID = 7745857809569020434L;

    /**
     * 发送消息的用户id
     */
    private Long userId;

    /**
     * 要发送到的用户，如果状态是群聊，则该字段表示群id
     */
    private Long toUserId;

    /**
     * 0-私聊
     * 1-群聊
     * 2-系统消息
     */
    private int type;

    /**
     * 发送的消息
     */
    private String message;
}
