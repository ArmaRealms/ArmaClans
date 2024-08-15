package net.sacredlabyrinth.phaed.simpleclans.listeners;

import mineverse.Aust1n46.chat.api.events.ChannelJoinEvent;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.StorageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class VentureChatListener implements Listener {
    private final ClanManager clanManager;
    private final StorageManager storageManager;

    public VentureChatListener(@NotNull SimpleClans plugin) {
        this.clanManager = plugin.getClanManager();
        this.storageManager = plugin.getStorageManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVentureChat(@NotNull ChannelJoinEvent event) {
        ClanPlayer clanPlayer = clanManager.getClanPlayer(event.getPlayer());
        if (clanPlayer == null) return;
        if (clanPlayer.getChannel() == ClanPlayer.Channel.NONE) return;
        clanPlayer.setChannel(ClanPlayer.Channel.NONE);
        storageManager.updateClanPlayer(clanPlayer);
    }

}
