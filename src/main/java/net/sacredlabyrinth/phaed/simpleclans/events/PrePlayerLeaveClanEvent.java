package net.sacredlabyrinth.phaed.simpleclans.events;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * @author ThiagoROX
 */
public class PrePlayerLeaveClanEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private boolean cancelled;

    public PrePlayerLeaveClanEvent(CommandSender sender, Player player) {
        super(player);
        if (sender == null) {
            sender = Bukkit.getConsoleSender();
        }
        this.sender = sender;
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
