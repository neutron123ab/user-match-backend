package com.neutron.usermatchbackend.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zzs
 * @date 2023/4/1 21:11
 */
@Data
public class TeamUpdateRequest implements Serializable {


    private static final long serialVersionUID = -8005942106491000823L;

    @TableId(type = IdType.AUTO)
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
