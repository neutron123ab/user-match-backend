package com.neutron.usermatchbackend.once;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import cn.hutool.core.date.StopWatch;
import com.neutron.usermatchbackend.mapper.UserMapper;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author zzs
 * @date 2023/3/29 13:43
 */
@Slf4j
@Component
public class InsertUsers {

    @Resource
    private UserService userService;

    private final int INSERT_NUM = 1000000;

    private final ExecutorService executorService = new ThreadPoolExecutor(100, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 每次只导入一条数据
     */
    public void insertUsersOne() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = getUser();
            userService.save(user);
        }
        long stop = System.currentTimeMillis();
        log.info("execute time(insert one by one): {}", stop - start);
    }

    /**
     * 批量导入数据
     */
    public void insertUsersBatch() {
        final int batch = 10000;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            User user = getUser();
            userList.add(user);
        }
        userService.saveBatch(userList, 10000);
        stopWatch.stop();
        log.info("execute time(insert batch): {}", stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发插入数据
     */
    public void insertUsersConcurrent() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<User> userList = new ArrayList<>();
            while(true) {
                j++;
                User user = getUser();
                userList.add(user);
                if(j % 10000 == 0) {
                    break;
                }
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, 10000);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println("execute time(concurrent): " + stopWatch.getTotalTimeMillis());
    }

    private static User getUser() {
        User user = new User();
        user.setUsername("demo");
        user.setUserAccount("假数据");
        user.setAvatarUrl("https://neutron-1311154792.cos.ap-shanghai.myqcloud.com/-42e71e85137282a1.jpg");
        user.setGender(0);
        user.setUserPassword("12345678");
        user.setPhone("1234");
        user.setEmail("123");
        user.setUserStatus(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        user.setTags(new HashSet<>());
        user.setUserRole(0);
        return user;
    }

}
