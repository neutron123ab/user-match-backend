package com.neutron.usermatchbackend;
import java.util.Date;

import cn.hutool.core.bean.BeanUtil;
import com.neutron.usermatchbackend.model.dto.UserDTO;
import com.neutron.usermatchbackend.model.entity.User;
import com.neutron.usermatchbackend.service.UserService;
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

    @Test
    void contextLoads() {
        User user = new User();
        user.setId(0L);
        user.setUsername("demo");
        user.setUserAccount("123");
        user.setAvatarUrl("12312313");
        user.setGender(0);
        user.setUserPassword("137911");
        user.setPhone("");
        user.setEmail("");
        user.setUserStatus(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        user.setUserRole(0);
        user.setUserCode("456");

        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        log.info(userDTO.toString());
        Assertions.assertNotNull(userDTO);


    }

}
