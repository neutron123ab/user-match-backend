package com.neutron.usermatchbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.neutron.usermatchbackend.common.BaseResponse;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.common.ResultUtils;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.TeamDTO;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Team;
import com.neutron.usermatchbackend.model.request.TeamCreateRequest;
import com.neutron.usermatchbackend.model.request.TeamUpdateRequest;
import com.neutron.usermatchbackend.service.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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



}
