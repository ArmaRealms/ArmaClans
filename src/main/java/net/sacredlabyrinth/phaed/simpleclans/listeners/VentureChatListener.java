package net.sacredlabyrinth.phaed.simpleclans.listeners;

import mineverse.Aust1n46.chat.api.events.ChannelJoinEvent;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public class VentureChatListener extends SCListener {

    public VentureChatListener(@NotNull final SimpleClans plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVentureChat(@NotNull final ChannelJoinEvent event) {
        final ClanPlayer clanPlayer = plugin.getClanManager().getClanPlayer(event.getPlayer());
        if (clanPlayer == null) return;
        if (clanPlayer.getChannel() == ClanPlayer.Channel.NONE) return;
        clanPlayer.setChannel(ClanPlayer.Channel.NONE);
        plugin.getStorageManager().updateClanPlayer(clanPlayer);
    }

}
