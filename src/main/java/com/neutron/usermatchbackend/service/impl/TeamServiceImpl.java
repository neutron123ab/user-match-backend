package com.neutron.usermatchbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.model.entity.Team;
import com.neutron.usermatchbackend.service.TeamService;
import com.neutron.usermatchbackend.mapper.TeamMapper;
import org.springframework.stereotype.Service;

/**
* @author zzs
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-03-31 21:44:24
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

}




