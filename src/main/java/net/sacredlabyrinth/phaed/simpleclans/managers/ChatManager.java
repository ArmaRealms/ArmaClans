package net.sacredlabyrinth.phaed.simpleclans.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import me.clip.placeholderapi.PlaceholderAPI;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.chat.ChatHandler;
import net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage;
import net.sacredlabyrinth.phaed.simpleclans.hooks.discord.DiscordHook;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static net.sacredlabyrinth.phaed.simpleclans.ClanPlayer.Channel;
import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source;
import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source.DISCORD;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.*;
import static org.bukkit.Bukkit.getPluginManager;

public final class ChatManager {

    private final SimpleClans plugin;
    private final Set<ChatHandler> handlers = new HashSet<>();
    private DiscordHook discordHook;

    public ChatManager(SimpleClans plugin) {
        this.plugin = plugin;
        registerHandlers();
        if (isDiscordHookEnabled()) {
            DiscordSRV.api.subscribe(this);
        }
    }

    @Subscribe
    public void registerDiscord(DiscordReadyEvent event) {
        discordHook = new DiscordHook(plugin);
        DiscordSRV.api.subscribe(discordHook);
        getPluginManager().registerEvents(discordHook, plugin);
    }

    @Nullable
    public DiscordHook getDiscordHook() {
        if (isDiscordHookEnabled()) {
            // Manually instantiate, if JDA did load faster than SC
            if (discordHook == null && DiscordSRV.getPlugin().getJda().getStatus() == JDA.Status.CONNECTED) {
                registerDiscord(new DiscordReadyEvent());
            }
        }

        return discordHook;
    }

    public void processChat(@NotNull SCMessage message) {
        Clan clan = Objects.requireNonNull(message.getSender().getClan(), "Clan cannot be null");

        List<ClanPlayer> receivers = new ArrayList<>();
        switch (message.getChannel()) {
            case ALLY:
                if (!plugin.getSettingsManager().is(ALLYCHAT_ENABLE)) {
                    return;
                }

                receivers.addAll(getOnlineAllyMembers(clan).stream().filter(allyMember ->
                        !allyMember.isMutedAlly()).collect(Collectors.toList()));
                receivers.addAll(clan.getOnlineMembers().stream().filter(onlineMember ->
                        !onlineMember.isMutedAlly()).collect(Collectors.toList()));
                break;
            case CLAN:
                if (!plugin.getSettingsManager().is(CLANCHAT_ENABLE)) {
                    return;
                }

                receivers.addAll(clan.getOnlineMembers().stream().filter(member -> !member.isMuted()).
                        collect(Collectors.toList()));
        }
        message.setReceivers(receivers);

        for (ChatHandler ch : handlers) {
            if (ch.canHandle(message.getSource())) {
                ch.sendMessage(message.clone());
            }
        }
    }

    public void processChat(@NotNull Source source, @NotNull Channel channel,
                            @NotNull ClanPlayer clanPlayer, String message) {
        Objects.requireNonNull(clanPlayer.getClan(), "Clan cannot be null");
        processChat(new SCMessage(source, channel, clanPlayer, message));
    }

    public String parseChatFormat(String format, SCMessage message) {
        return parseChatFormat(format, message, new HashMap<>());
    }

    public String parseChatFormat(String format, SCMessage message, Map<String, String> placeholders) {
        SettingsManager sm = plugin.getSettingsManager();
        ClanPlayer sender = message.getSender();

        String leaderColor = sm.getColored(ConfigField.valueOf(message.getChannel() + "CHAT_LEADER_COLOR"));
        String memberColor = sm.getColored(ConfigField.valueOf(message.getChannel() + "CHAT_MEMBER_COLOR"));
        String trustedColor = sm.getColored(ConfigField.valueOf(message.getChannel() + "CHAT_TRUSTED_COLOR"));

        String rank = sender.getRankId().isEmpty() ? null : ChatUtils.parseColors(sender.getRankDisplayName());
        ConfigField configField = ConfigField.valueOf(String.format("%sCHAT_RANK",
                message.getSource() == DISCORD ? "DISCORD" : message.getChannel()));
        String rankFormat = (rank != null) ? sm.getColored(configField).replace("%rank%", rank) : "";

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                format = format.replace("%" + e.getKey() + "%", e.getValue());
            }
        }

        String parsedFormat = ChatUtils.parseColors(format)
                .replace("%clan%", Objects.requireNonNull(sender.getClan()).getColorTag())
                .replace("%clean-tag%", sender.getClan().getTag())
                .replace("%nick-color%",
                        (sender.isLeader() ? leaderColor : sender.isTrusted() ? trustedColor : memberColor))
                .replace("%player%", sender.getName())
                .replace("%rank%", rankFormat);
        parsedFormat = parseWithPapi(message.getSender(), parsedFormat)
                .replace("%message%", message.getContent());

        return parsedFormat;
    }

    public boolean isDiscordHookEnabled() {
        return getPluginManager().getPlugin("DiscordSRV") != null && plugin.getSettingsManager().is(DISCORDCHAT_ENABLE);
    }

    private String parseWithPapi(ClanPlayer cp, String message) {
        if (getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return message;
        }
        OfflinePlayer sender = Bukkit.getOfflinePlayer(cp.getUniqueId());
        message = PlaceholderAPI.setPlaceholders(sender, message);

        // If there are still placeholders left, try to parse them
        // E.g. if the user has a placeholder as LuckPerms prefix/suffix
        if (message.contains("%")) {
            message = PlaceholderAPI.setPlaceholders(sender, message);
        }
        return message;
    }

    private void registerHandlers() {
        Set<Class<? extends ChatHandler>> chatHandlers =
                Helper.getSubTypesOf("net.sacredlabyrinth.phaed.simpleclans.chat.handlers", ChatHandler.class);
        plugin.getLogger().log(Level.INFO, "Registering {0} chat handlers...", chatHandlers.size());

        for (Class<? extends ChatHandler> handler : chatHandlers) {
            try {
                handlers.add(handler.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error while trying to register {0}: " +
                        ex.getMessage(), handler.getSimpleName());
            }
        }
    }

    private List<ClanPlayer> getOnlineAllyMembers(Clan clan) {
        return clan.getAllAllyMembers().stream().
                filter(allyPlayer -> allyPlayer.toPlayer() != null).
                collect(Collectors.toList());
    }
}
