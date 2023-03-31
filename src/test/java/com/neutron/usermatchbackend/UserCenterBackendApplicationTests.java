package com.neutron.usermatchbackend;
import java.util.*;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.StopWatch;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.once.InsertUsers;
import com.neutron.usermatchbackend.service.UserService;
import com.neutron.usermatchbackend.util.AlgorithmUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
class UserMatchBackendApplicationTests {

    @Resource
    private UserService userService;

    @Resource
    private InsertUsers insertUsers;

    @Test
    void contextLoads() throws InterruptedException {
        Set<String> word1 = new HashSet<>();
        word1.add("java");
        word1.add("game");
        word1.add("soccer");
        Set<String> word2 = new HashSet<>();
        word2.add("java");

        int minDistance = AlgorithmUtil.minDistance(new ArrayList<>(word1), new ArrayList<>(word2));
        System.out.println("minDistance = " + minDistance);
    }

}
