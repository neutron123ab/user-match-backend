package com.neutron.usermatchbackend.controller;

import com.neutron.usermatchbackend.model.entity.ChatInfo;
import com.neutron.usermatchbackend.util.ConvertMsgUtil;
import com.neutron.usermatchbackend.util.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author zzs
 * @date 2023/4/5 14:06
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @PostMapping("/jinDuTiao")
    public void jinDuTiao() throws InterruptedException {
        String msg = "";
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setType(2);
        for (int i = 0; i <= 100; i++) {
            msg = String.valueOf(i);
            Thread.sleep(100);
            chatInfo.setMessage(msg);
            String infoToJson = ConvertMsgUtil.convertInfoToJson(chatInfo);
        }
    }
}
