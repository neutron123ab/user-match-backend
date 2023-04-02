package com.neutron.usermatchbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.enums.TeamStatusEnum;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.TeamDTO;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Team;
import com.neutron.usermatchbackend.model.entity.UserTeam;
import com.neutron.usermatchbackend.model.request.TeamCreateRequest;
import com.neutron.usermatchbackend.model.request.TeamUpdateRequest;
import com.neutron.usermatchbackend.service.TeamService;
import com.neutron.usermatchbackend.mapper.TeamMapper;
import com.neutron.usermatchbackend.service.UserTeamService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

import static com.neutron.usermatchbackend.constant.TeamConstant.*;

/**
* @author zzs
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-03-31 21:44:24
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    private static final String SALT = "team";

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, UserDTO loginUser) {
        if(teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = query().eq("id", teamUpdateRequest.getId()).one();
        //如果不是队长和管理员则无权修改队伍
        if(!loginUser.getId().equals(team.getCaptainId()) || loginUser.getUserRole() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //获取队伍状况
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getTeamStatus());
        if(statusEnum.equals(TeamStatusEnum.SECRET)) {
            if(StrUtil.isEmptyIfStr(teamUpdateRequest.getTeamPassword())) {
                throw new BusinessException(ErrorCode.NULL_ERROR, "加密的队伍必须要设置密码");
            }
        }
        Team team1 = new Team();
        BeanUtil.copyProperties(teamUpdateRequest, team1);

        return updateById(team1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTeam(TeamCreateRequest teamCreateRequest, UserDTO loginUser) {
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if(teamCreateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //队名不为空且小于等于20个字符
        String teamName = teamCreateRequest.getTeamName();
        if(StrUtil.isEmptyIfStr(teamName) || teamName.length() > MAX_TEAM_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队名不满足要求");
        }
        //判断队伍描述信息
        String teamDescription = teamCreateRequest.getTeamDescription();
        if(StrUtil.isEmptyIfStr(teamDescription) || teamDescription.length() > MAX_TEAM_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //判断队伍状态是否存在
        Integer teamStatus = teamCreateRequest.getTeamStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(teamStatus);
        if(BeanUtil.isEmpty(enumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不存在");
        }
        //判断队伍最大人数
        Integer maxNum = teamCreateRequest.getMaxNum();
        if(maxNum < MIN_TEAM_MEMBERS || maxNum > MAX_TEAM_MEMBERS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最大人数不满足要求");
        }

        //如果队伍状态是加密，则要校验密码
        String teamPassword = null;
        if (enumByValue.equals(TeamStatusEnum.SECRET)) {
            teamPassword = teamCreateRequest.getTeamPassword();
            if(StrUtil.isEmptyIfStr(teamPassword) || teamPassword.length() < MIN_TEAM_PASSWORD_LEN || teamPassword.length() > MAX_TEAM_PASSWORD_LEN) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度要大于6位，小于20位");
            }
            teamPassword = SecureUtil.md5(teamPassword + SALT);
        }

        //判断过期时间
        Date expireTime = teamCreateRequest.getExpireTime();
        if(expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能在当前时间之前");
        }
        //用户最多创建5个队伍
        Long userId = loginUser.getId();
        Long count = query().eq("captain_id", userId).count();
        if(count > MAX_TEAM_OWN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户创建的队伍数已达上限");
        }

        //插入队伍信息到队伍表
        teamCreateRequest.setTeamPassword(teamPassword);
        Team team = new Team();
        BeanUtil.copyProperties(teamCreateRequest, team);
        boolean save = save(team);
        Long teamId = team.getId();
        if(!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        //插入信息到用户队伍关联表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        boolean flag = userTeamService.save(userTeam);
        if(!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return true;
    }
}




