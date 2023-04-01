package com.neutron.usermatchbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.model.entity.UserTeam;
import com.neutron.usermatchbackend.service.UserTeamService;
import com.neutron.usermatchbackend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author zzs
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-04-01 09:57:23
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




