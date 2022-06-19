package com.loohp.chatpreview;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatPreview extends JavaPlugin {

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

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(PacketAdapter.params().plugin(plugin).listenerPriority(ListenerPriority.MONITOR).types(PacketType.Login.Client.START, PacketType.Play.Client.CHAT_PREVIEW)) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isPlayerTemporary() || event.isCancelled()) {
                    return;
                }

                PacketType packetType = event.getPacketType();
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();

                if (packetType.equals(PacketType.Login.Client.START)) {
                    packet.getModifier().write(1, Optional.empty());
                } else if (packetType.equals(PacketType.Play.Client.CHAT_PREVIEW)) {
                    int id = packet.getIntegers().read(0);
                    String chat = packet.getStrings().read(0);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        PacketContainer previewPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CHAT_PREVIEW);
                        previewPacket.getIntegers().write(0, id);

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
                        previewPacket.getChatComponents().write(0, WrappedChatComponent.fromLegacyText(result));

                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, previewPacket);
                    });
                }
            }

            @Override
            public void onPacketSending(PacketEvent event) {
                //do nothing
            }
        });

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "ChatPreview has been enabled!");
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
