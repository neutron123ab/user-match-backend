package com.neutron.usermatchbackend.service;

import com.neutron.usermatchbackend.model.dto.TeamDTO;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.request.TeamCreateRequest;
import com.neutron.usermatchbackend.model.request.TeamQueryRequest;
import com.neutron.usermatchbackend.model.request.TeamUpdateRequest;
import com.neutron.usermatchbackend.model.vo.TeamUserVO;

import java.util.List;

/**
* @author zzs
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-03-31 21:44:24
*/
public interface TeamService extends IService<Team> {

    /**
     * 队长或管理员更新队伍
     * @param teamUpdateRequest 队伍更新请求
     * @param loginUser 当前用户
     * @return 是否更新成功
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, UserDTO loginUser);

    /**
     * 创建队伍
     *
     * @param teamCreateRequest 创建队伍请求
     * @param loginUser 当前登录用户
     * @return 是否创建成功
     */
    boolean createTeam(TeamCreateRequest teamCreateRequest, UserDTO loginUser);

    /**
     * 搜索队伍
     *
     * @param teamQueryRequest 搜索队伍请求
     * @return 搜索结果
     */
    List<TeamUserVO> getTeams(TeamQueryRequest teamQueryRequest);

}
