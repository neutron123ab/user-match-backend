package com.neutron.usermatchbackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zzs
 * @date 2023/4/3 16:20
 */
@Slf4j
@Component
public class PreCacheJob {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    @Scheduled(cron = "0 30 7 * * *")
    public void preCacheRecommendUser(){
        RLock lock = redissonClient.getLock("user:match:preCache:lock");
        try {
            if (lock.tryLock(0, TimeUnit.SECONDS)) {
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.select("id");
                List<Long> userIdList = userService.list(queryWrapper)
                        .stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                for (Long userId : userIdList) {
                    QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                    userQueryWrapper.ne("id", userId);
                    Page<User> page = userService.page(new Page<>(1, 8), userQueryWrapper);
                    String redisKey = String.format("user:match:getAllUsers:%s:%s", userId, 1);
                    Gson gson = new Gson();
                    String pageJson = gson.toJson(page);
                    stringRedisTemplate.opsForValue().set(redisKey, pageJson, 30, TimeUnit.MINUTES);
                }
            }
        } catch (InterruptedException e) {
            log.error("preCacheRecommendUser: ", e);
        } finally {
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
