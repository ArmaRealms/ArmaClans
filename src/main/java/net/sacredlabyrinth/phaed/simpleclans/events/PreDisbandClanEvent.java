package net.sacredlabyrinth.phaed.simpleclans.events;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author ThiagoROX
 */
public class PreDisbandClanEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private final Clan clan;
    private boolean cancelled;

    public PreDisbandClanEvent(CommandSender sender, Clan clan) {
        if (sender == null) {
            sender = Bukkit.getConsoleSender();
        }
        this.sender = sender;
        this.clan = clan;
    }

    public Clan getClan() {
        return this.clan;
    }

    public CommandSender getSender() {
        return sender;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
