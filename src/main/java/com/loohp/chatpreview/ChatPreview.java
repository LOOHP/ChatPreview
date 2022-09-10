package com.loohp.chatpreview;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatPreviewEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatPreview extends JavaPlugin implements Listener {

    public static final Pattern COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");

    public static String chatFormat = "";
    public static Map<String, String> commandFormats;

    @Override
    public void onEnable() {
        ChatPreview plugin = this;

        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();
        loadConfig();

        getCommand("chatpreviewreload").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "ChatPreview has been enabled!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatPreview(AsyncPlayerChatPreviewEvent event) {
        Player player = event.getPlayer();
        String chat = event.getMessage();
        String chatMessage = chat;
        String format = chatFormat;
        if (chat.startsWith("/")) {
            for (Entry<String, String> entry : commandFormats.entrySet()) {
                String command = entry.getKey();
                if (chat.startsWith(command)) {
                    format = entry.getValue();
                    chatMessage = chatMessage.substring(command.length());
                    if (chatMessage.charAt(0) == ' ') {
                        chatMessage = chatMessage.substring(1);
                    }
                    break;
                }
            }
        }

        if (hasInteractiveChat()) {
            chatMessage = InteractiveChatHook.translateColors(player, chatMessage);
        }

        String result = translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, format)).replace("{Message}", chatMessage);
        event.setMessage(result);
    }

    public void loadConfig() {
        chatFormat = getConfig().getString("ChatPreviewFormat");
        commandFormats = getConfig().getConfigurationSection("CommandPreviewFormats").getValues(false).entrySet().stream().collect(Collectors.toMap(each -> each.getKey(), each -> (String) each.getValue()));
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "ChatPreview had been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("chatpreviewreload")) {
            if (sender.hasPermission("chatpreview.reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("ReloadMessage")));
            }
        }
        return true;
    }

    public boolean hasInteractiveChat() {
        Plugin plugin = getServer().getPluginManager().getPlugin("InteractiveChat");
        return plugin != null && plugin.isEnabled();
    }

    public String translateAlternateColorCodes(char character, String str) {
        str = ChatColor.translateAlternateColorCodes(character, str);
        Matcher matcher = COLOR_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, ChatColor.COLOR_CHAR + "x" + ChatColor.COLOR_CHAR + matcher.group(1) + ChatColor.COLOR_CHAR + matcher.group(2) + ChatColor.COLOR_CHAR + matcher.group(3) + ChatColor.COLOR_CHAR + matcher.group(4) + ChatColor.COLOR_CHAR + matcher.group(5) + ChatColor.COLOR_CHAR + matcher.group(6));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
