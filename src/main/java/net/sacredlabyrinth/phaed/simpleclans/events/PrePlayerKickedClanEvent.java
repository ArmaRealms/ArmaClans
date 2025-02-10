package net.sacredlabyrinth.phaed.simpleclans.events;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author ThiagoROX
 */
public class PrePlayerKickedClanEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Clan clan;
    private final ClanPlayer target;
    private boolean cancelled;

    public PrePlayerKickedClanEvent(Clan clan, ClanPlayer target) {
        this.clan = clan;
        this.target = target;
    }

    public Clan getClan() {
        return this.clan;
    }

    public ClanPlayer getClanPlayer() {
        return this.target;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
