package com.neutron.usermatchbackend.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zzs
 * @date 2023/4/2 16:48
 */
@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = 2553719178689424180L;
    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 描述
     */
    private String teamDescription;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 队长id
     */
    private Long captainId;

    /**
     * 队伍状态（0 公开，1 私有，2 加密）
     */
    private Integer teamStatus;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 队伍当前人数
     */
    private Integer membersNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 创建人信息
     */
    private UserVO createUser;

    /**
     * 是否已加入队伍
     */
    private Boolean hasJoin;

}
