package com.neutron.usermatchbackend.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zzs
 * @date 2023/4/2 13:48
 */
@Data
public class TeamCreateRequest implements Serializable {

    private static final long serialVersionUID = -2344000361971056303L;

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
     * 入队密码
     */
    private String teamPassword;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

}
