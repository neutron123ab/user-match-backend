package com.neutron.usermatchbackend.controller;

import cn.hutool.core.collection.CollUtil;
import com.neutron.usermatchbackend.common.BaseResponse;
import com.neutron.usermatchbackend.common.ErrorCode;
import com.neutron.usermatchbackend.common.ResultUtils;
import com.neutron.usermatchbackend.exception.BusinessException;
import com.neutron.usermatchbackend.model.entity.Tags;
import com.neutron.usermatchbackend.service.TagsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.neutron.usermatchbackend.constant.TagsConstant.USER_MATCH_TAGS;

/**
 * @author zzs
 * @date 2023/4/1 13:24
 */
@Slf4j
@RestController
@RequestMapping("/tags")
public class TagsController {

    @Resource
    private TagsService tagsService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/getAllTags")
    public BaseResponse<List<String>> getAllTags() {
        Set<String> set = stringRedisTemplate.opsForSet().members(USER_MATCH_TAGS);
        //如果缓存中有该标签
        if(!CollUtil.isEmpty(set)) {
            List<String> tags = new ArrayList<>(set);
            return ResultUtils.success(tags);
        }
        //缓存中没有则去查数据库
        List<String> collect = tagsService.list()
                .stream()
                .map(Tags::getTagName)
                .collect(Collectors.toList());
        Long add = stringRedisTemplate.opsForSet().add(USER_MATCH_TAGS, collect.toArray(new String[0]));
        stringRedisTemplate.expire(USER_MATCH_TAGS, 1, TimeUnit.HOURS);
        if(add == null || add < collect.size()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "标签添加到缓存失败");
        }
        return ResultUtils.success(collect);
    }
}
