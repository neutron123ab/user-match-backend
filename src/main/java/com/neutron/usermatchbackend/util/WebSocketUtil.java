package com.neutron.usermatchbackend.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neutron.usermatchbackend.enums.MsgStatusEnum;
import com.neutron.usermatchbackend.model.entity.ChatInfo;
import com.neutron.usermatchbackend.model.entity.UserTeam;
import com.neutron.usermatchbackend.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author zzs
 * @date 2023/4/5 14:19
 */
@Slf4j
@Component
@ServerEndpoint(value = "/websocket/{userId}")
public class WebSocketUtil {

    private Session session;

    private Long userId;

    private static Map<Long, WebSocketUtil> webSocketMap = new ConcurrentHashMap<>();

    @Resource
    private UserTeamService userTeamService;

    /**
     * 监听连接建立
     *
     * @param session 会话
     * @param userId  用户id
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.session = session;
        this.userId = userId;
        webSocketMap.remove(userId);
        webSocketMap.put(userId, this);
        log.info("[连接ID: {}] 建立连接，当前连接数：{}", userId, webSocketMap.size());
    }

    /**
     * 监听连接关闭
     */
    @OnClose
    public void onClose() {
        webSocketMap.remove(userId);
        log.info("[连接ID: {}] 断开连接，当前连接数：{}", userId, webSocketMap.size());
    }

    /**
     * 监听消息发送
     *
     * @param message 客户端发送的消息
     */
    @OnMessage
    public void onMessage(String message) {
        ChatInfo chatInfo = ConvertMsgUtil.getChatInfo(message);
        Long toUserId = chatInfo.getToUserId();
        String chatMessage = chatInfo.getMessage();
        int typeValue = chatInfo.getType();
        MsgStatusEnum enumByValue = MsgStatusEnum.getEnumByValue(typeValue);

        //私聊
        if (enumByValue.equals(MsgStatusEnum.SINGLE)) {
            sendMessageSingle(toUserId, chatMessage);
        }
        //群聊
        if (enumByValue.equals(MsgStatusEnum.GROUP)) {
            sendMessageGroup(toUserId, chatMessage);
        }
        //系统消息
        if (enumByValue.equals(MsgStatusEnum.SYSTEM)) {
            sendMessageToAll(chatMessage);
        }
    }

    /**
     * 发送消息方法
     */
    public void sendMessageSingle(Long toUserId, String message) {
        WebSocketUtil webSocketUtil = webSocketMap.get(toUserId);
        try {
            webSocketUtil.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("[{} 向 {} 发送消息] 内容：{}", userId, toUserId, message);
    }

    /**
     * 用户向所在群聊发送消息
     *
     * @param teamId 队伍id
     * @param message 消息内容
     */
    public void sendMessageGroup(Long teamId, String message) {
        Set<Map.Entry<Long, WebSocketUtil>> entries = webSocketMap.entrySet();

        //根据teamId取出所有同一组的用户session
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("team_id", teamId);
        userTeamQueryWrapper.select("user_id");
        List<Long> userIdList = userTeamService.list(userTeamQueryWrapper)
                .stream()
                .map(UserTeam::getUserId)
                .collect(Collectors.toList());
        for (Map.Entry<Long, WebSocketUtil> entry : entries) {
            Long key = entry.getKey();
            WebSocketUtil value = entry.getValue();
            //避免发送给自己
            if (key.equals(userId)) {
                continue;
            }
            if (userIdList.contains(key)) {
                try {
                    value.session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        log.info("[连接ID: {}][用户{}群发] 内容：{}", userId, userId, message);
    }

    /**
     * 系统向所有人发送消息
     *
     * @param message 消息内容
     */
    public static void sendMessageToAll(String message) {
        Set<Map.Entry<Long, WebSocketUtil>> entries = webSocketMap.entrySet();
        for (Map.Entry<Long, WebSocketUtil> entry : entries) {
            log.info("[连接ID: {}][系统消息] 内容：{}", entry.getKey(), message);
            WebSocketUtil value = entry.getValue();
            try {
                value.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
