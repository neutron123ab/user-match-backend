package com.neutron.usermatchbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.enums.TeamStatusEnum;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.Team;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.model.entity.UserTeam;
import com.neutron.usermatchbackend.model.request.TeamCreateRequest;
import com.neutron.usermatchbackend.model.request.TeamJoinRequest;
import com.neutron.usermatchbackend.model.request.TeamQueryRequest;
import com.neutron.usermatchbackend.model.request.TeamUpdateRequest;
import com.neutron.usermatchbackend.model.vo.TeamUserVO;
import com.neutron.usermatchbackend.model.vo.UserVO;
import com.neutron.usermatchbackend.service.TeamService;
import com.neutron.usermatchbackend.mapper.TeamMapper;
import com.neutron.usermatchbackend.service.UserService;
import com.neutron.usermatchbackend.service.UserTeamService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

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

    @Override
    public List<TeamUserVO> getTeams(TeamQueryRequest teamQueryRequest) {
        if(teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();

        //组合查询条件
        //根据id查找队伍
        Long id = teamQueryRequest.getId();
        if(id != null && id > 0) {
            teamQueryWrapper.eq("id", id);
        }
        //根据队名查找队伍
        String teamName = teamQueryRequest.getTeamName();
        if(StrUtil.isNotBlank(teamName)) {
            teamQueryWrapper.like("team_name", teamName);
        }
        //根据队伍描述查找用户
        String teamDescription = teamQueryRequest.getTeamDescription();
        if(StrUtil.isNotBlank(teamDescription)) {
            teamQueryWrapper.like("team_description", teamDescription);
        }
        //根据队伍状态查找用户
        Integer teamStatus = teamQueryRequest.getTeamStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(teamStatus);
        if(enumByValue == null) {
            enumByValue = TeamStatusEnum.PUBLIC;
        }
        //当查找的是私有队伍时直接报错
        if(enumByValue.equals(TeamStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        teamQueryWrapper.eq("team_status", enumByValue.getValue());
        //根据队长id查找队伍
        Long captainId = teamQueryRequest.getCaptainId();
        if(captainId != null && captainId > 0) {
            teamQueryWrapper.eq("captain_id", captainId);
        }
        //根据队伍最大人数查找队伍
        Integer maxNum = teamQueryRequest.getMaxNum();
        if(maxNum != null && maxNum > 0) {
            teamQueryWrapper.eq("max_num", maxNum);
        }
        //组合条件查询结束
        //去除掉已过期的队伍
        //当过期时间在当前时间之后或者过期时间为空时才找出数据
        teamQueryWrapper.and(qw -> qw.gt("expire_time", new Date()).or().isNull("expire_time"));

        List<Team> teamList = list(teamQueryWrapper);
        ArrayList<TeamUserVO> resultList = new ArrayList<>();
        //查找创建人的信息
        for (Team team : teamList) {
            Long teamCaptainId = team.getCaptainId();
            if(teamCaptainId == null) {
                continue;
            }
            User captain = userService.getById(teamCaptainId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtil.copyProperties(team, teamUserVO);
            //用户信息脱敏
            if(captain != null) {
                UserVO userVO = new UserVO();
                BeanUtil.copyProperties(captain, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            resultList.add(teamUserVO);
        }
        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, UserDTO loginUser) {

        if(BeanUtil.hasNullField(teamJoinRequest)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long teamId = teamJoinRequest.getTeamId();
        String password = teamJoinRequest.getPassword();
        Team team = getById(teamId);
        Date expireTime = team.getExpireTime();
        if(expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍已过期");
        }
        Integer teamStatus = team.getTeamStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(teamStatus);
        if(enumByValue.equals(TeamStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        if(enumByValue.equals(TeamStatusEnum.SECRET)) {
            if(password == null) {
                throw new BusinessException(ErrorCode.NULL_ERROR);
            }
            String encryptPassword = SecureUtil.md5(password + SALT);
            if(!encryptPassword.equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        Long userId = loginUser.getId();
        RLock lock = redissonClient.getLock("user:match:join_team");
        try {
            while(true) {
                //获取锁成功
                if(lock.tryLock(0, TimeUnit.SECONDS)) {
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("user_id", userId);
                    long count = userTeamService.count(userTeamQueryWrapper);
                    if(count > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多加入5个队伍");
                    }
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("user_id", userId).eq("team_id", teamId);
                    long userJoinedNum = userTeamService.count(userTeamQueryWrapper);
                    if (userJoinedNum > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入了该队伍");
                    }
                    Integer membersNum = team.getMembersNum();
                    if(membersNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    //用户加入队伍
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    boolean save = userTeamService.save(userTeam);
                    if(!save) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
                    }
                    team.setMembersNum(++membersNum);
                    return updateById(team);
                }
            }
        } catch (InterruptedException e) {
            return false;
        } finally {
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long teamId, UserDTO loginUser) {
        if(teamId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long userId = loginUser.getId();
        Team team = getById(teamId);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId).eq("team_id", teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if(count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户未加入队伍");
        }
        //如果队伍中只有一个人，则直接移除队伍
        if(team.getMembersNum() == 1) {
            removeById(teamId);
        } else {
            //如果当前用户是队长，则将队长移交给第二个加入队伍的用户
            if(team.getCaptainId().equals(userId)) {
                QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("team_id", teamId);
                queryWrapper.orderByAsc("join_time").last("limit 2");
                List<UserTeam> list = userTeamService.list(queryWrapper);
                if(CollUtil.isEmpty(list) || list.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam userTeam = list.get(1);
                team.setCaptainId(userTeam.getUserId());
                boolean update = updateById(team);
                if(!update) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队长操作失败");
                }
            }
            //将队伍总人数减一
            Integer membersNum = team.getMembersNum();
            team.setMembersNum(--membersNum);
            boolean update = updateById(team);
            if(!update) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍人数失败");
            }
        }
        return userTeamService.remove(userTeamQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(Long teamId, UserDTO loginUser) {
        if(teamId == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = getById(teamId);
        Long userId = loginUser.getId();
        if(!team.getCaptainId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "只有队长能删除队伍");
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("team_id", teamId);
        boolean remove = userTeamService.remove(userTeamQueryWrapper);
        if(!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return removeById(teamId);
    }
}






