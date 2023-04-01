package com.neutron.usermatchbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neutron.usermatchbackend.mapper.TagsMapper;
import com.neutron.usermatchbackend.model.entity.Tags;
import com.neutron.usermatchbackend.service.TagsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author zzs
* @description 针对表【tags(标签表)】的数据库操作Service实现
* @createDate 2023-04-01 11:19:37
*/
@Service
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags>
    implements TagsService{

}




