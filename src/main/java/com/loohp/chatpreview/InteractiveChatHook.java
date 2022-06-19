package com.loohp.chatpreview;

import com.loohp.interactivechat.utils.ChatColorUtils;
import org.bukkit.entity.Player;

public class InteractiveChatHook {

    public static String translateColors(Player player, String chatMessage) {
        if (player.hasPermission("interactivechat.chatcolor.translate")) {
            return ChatColorUtils.translateAlternateColorCodes('&', chatMessage);
        }
        return chatMessage;
    }

}
