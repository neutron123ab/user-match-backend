package com.neutron.usermatchbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @author zzs
 * @date 2023/4/5 13:59
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    /**
     * 自动注册使用了@ServerEndPoint注解声明的WebSocket endpoint
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
