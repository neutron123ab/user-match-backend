package com.neutron.usermatchbackend.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author zzs
 * @date 2023/4/1 21:54
 */
@Data
public class TeamDTO implements Serializable {

    private static final long serialVersionUID = 4322254807044368549L;
    /**
     *
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

}
