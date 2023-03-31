package com.neutron.usermatchbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author zzs
 */
@MapperScan("com.neutron.usermatchbackend.mapper")
@EnableScheduling
@SpringBootApplication
public class UserMatchBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserMatchBackendApplication.class, args);
    }

}
