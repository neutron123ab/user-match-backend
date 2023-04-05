package com.neutron.usermatchbackend.util;

import com.google.gson.Gson;
import com.neutron.usermatchbackend.model.entity.ChatInfo;

/**
 * @author zzs
 * @date 2023/4/5 15:55
 */
public class ConvertMsgUtil {

    private ConvertMsgUtil() {
    }

    /**
     * 将收到的json数据转换为ChatInfo
     *
     * @param message json格式的聊天信息
     * @return 聊天信息的实体类
     */
    public static ChatInfo getChatInfo(String message) {
        Gson gson = new Gson();
        return gson.fromJson(message, ChatInfo.class);
    }

    /**
     * 将ChatInfo转换为json
     *
     * @param chatInfo 聊天信息实体类
     * @return json
     */
    public static String convertInfoToJson(ChatInfo chatInfo) {
        Gson gson = new Gson();
        return gson.toJson(chatInfo);
    }

}
