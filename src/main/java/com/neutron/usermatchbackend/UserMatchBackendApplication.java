package com.neutron.usermatchbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author zzs
 */
@MapperScan("com.neutron.usermatchbackend.mapper")
@SpringBootApplication
public class UserMatchBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserMatchBackendApplication.class, args);
    }

}
