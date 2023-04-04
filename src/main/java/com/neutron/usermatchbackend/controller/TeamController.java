package com.neutron.usermatchbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neutron.usermatchbackend.common.BaseResponse;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.common.ResultUtils;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.TeamDTO;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Team;
import com.neutron.usermatchbackend.model.entity.UserTeam;
import com.neutron.usermatchbackend.model.request.*;
import com.neutron.usermatchbackend.model.vo.TeamUserVO;
import com.neutron.usermatchbackend.service.TeamService;
import com.neutron.usermatchbackend.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.neutron.usermatchbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author zzs
 * @date 2023/4/1 21:01
 */
@Slf4j
@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/create")
    public BaseResponse<Boolean> createTeam(@RequestBody TeamCreateRequest teamCreateRequest, HttpServletRequest request){

        if(teamCreateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍信息不能为空");
        }
        UserDTO user = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        boolean team = teamService.createTeam(teamCreateRequest, user);
        if(!team) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if(teamUpdateRequest == null) {
            throw  new BusinessException(ErrorCode.NULL_ERROR);
        }
        UserDTO userDto = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        boolean flag = teamService.updateTeam(teamUpdateRequest, userDto);
        if(!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/getTeamById")
    public BaseResponse<TeamDTO> getTeamById(long id) {
        if(id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        TeamDTO teamDTO = new TeamDTO();
        BeanUtil.copyProperties(team, teamDTO);
        return ResultUtils.success(teamDTO);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> getTeams(TeamQueryRequest teamQueryRequest, HttpServletRequest request){
        if(teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        List<TeamUserVO> teams = teamService.getTeams(teamQueryRequest);
        //判断当前用户是否加入了队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        List<Long> teamIdList = teams.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        userTeamQueryWrapper.eq("user_id", loginUser.getId());
        userTeamQueryWrapper.in("team_id", teamIdList);
        //得到该用户-队伍关联集合
        List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
        //得到已计入队伍的id集合
        Set<Long> idSet = list.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teams.forEach(team -> {
            boolean contains = idSet.contains(team.getId());
            team.setHasJoin(contains);
        });

        return ResultUtils.success(teams);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if(BeanUtil.hasNullField(teamJoinRequest)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        boolean flag = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(flag);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        long teamId = teamQuitRequest.getTeamId();
        if(teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        boolean flag = teamService.quitTeam(teamId, loginUser);
        return ResultUtils.success(flag);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody Long teamId, HttpServletRequest request) {
        if(teamId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        UserDTO loginUser = (UserDTO) request.getSession().getAttribute(USER_LOGIN_STATE);
        boolean flag = teamService.deleteTeam(teamId, loginUser);
        if(!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

}
