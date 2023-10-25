package net.sacredlabyrinth.phaed.simpleclans.commands.clan;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.sacredlabyrinth.phaed.simpleclans.*;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanPlayerInput;
import net.sacredlabyrinth.phaed.simpleclans.conversation.DisbandPrompt;
import net.sacredlabyrinth.phaed.simpleclans.conversation.SCConversation;
import net.sacredlabyrinth.phaed.simpleclans.hooks.discord.DiscordHook;
import net.sacredlabyrinth.phaed.simpleclans.hooks.discord.exceptions.DiscordHookException;
import net.sacredlabyrinth.phaed.simpleclans.managers.*;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Objects;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.events.EconomyTransactionEvent.Cause.DISCORD_CREATION;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.*;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.RED;

@CommandAlias("%clan")
@Conditions("%basic_conditions|leader")
public class LeaderCommands extends BaseCommand {

    @Dependency
    private SimpleClans plugin;
    @Dependency
    private StorageManager storage;
    @Dependency
    private PermissionsManager permissions;
    @Dependency
    private SettingsManager settings;
    @Dependency
    private RequestManager requestManager;
    @Dependency
    private ClanManager cm;
    @Dependency
    private ChatManager chatManager;

    @Subcommand("%demote")
    @CommandCompletion("@clan_leaders")
    @CommandPermission("simpleclans.leader.demote")
    @Description("{@@command.description.demote}")
    public void demote(Player player,
                       ClanPlayer cp,
                       Clan clan,
                       @Conditions("same_clan") @Name("leader") ClanPlayerInput other) {
        ClanPlayer otherCp = other.getClanPlayer();
        if (!clan.enoughLeadersOnlineToDemote(otherCp)) {
            ChatBlock.sendMessage(player, RED + lang("not.enough.leaders.online.to.vote.on.demotion", player));
            return;
        }
        if (!clan.isLeader(otherCp.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("player.is.not.a.leader.of.your.clan", player));
            return;
        }
        if (clan.getLeaders().size() > 2 && settings.is(CLAN_CONFIRMATION_FOR_DEMOTE)) {
            requestManager.addDemoteRequest(cp, otherCp.getName(), clan);
            ChatBlock.sendMessage(player, AQUA + lang("demotion.vote.has.been.requested.from.all.leaders",
                    player));
            return;
        }
        clan.addBb(player.getName(), lang("demoted.back.to.member", otherCp.getName()));
        clan.demote(otherCp.getUniqueId());
    }

    @Subcommand("%promote")
    @CommandPermission("simpleclans.leader.promote")
    @CommandCompletion("@clan_non_leaders")
    @Description("{@@command.description.promote}")
    public void promote(Player player,
                        Clan clan,
                        ClanPlayer cp,
                        @Conditions("online|same_clan") @Name("member") ClanPlayerInput other) {
        Player otherPl = Objects.requireNonNull(other.getClanPlayer().toPlayer());
        if (!permissions.has(otherPl, "simpleclans.leader.promotable")) {
            ChatBlock.sendMessage(player, RED + lang("the.player.does.not.have.the.permissions.to.lead.a.clan",
                    player));
            return;
        }
        if (otherPl.getUniqueId().equals(player.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("you.cannot.promote.yourself", player));
            return;
        }
        if (clan.isLeader(otherPl.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("the.player.is.already.a.leader", player));
            return;
        }
        if (settings.is(CLAN_CONFIRMATION_FOR_PROMOTE) && clan.getLeaders().size() > 1) {
            requestManager.requestAllLeaders(cp, ClanRequest.PROMOTE, otherPl.getName(), "asking.for.the.promotion",
                    player.getName(), otherPl.getName());
            ChatBlock.sendMessage(player, AQUA + lang("promotion.vote.has.been.requested.from.all.leaders",
                    player));
            return;
        }

        clan.addBb(player.getName(), lang("promoted.to.leader", otherPl.getName()));
        clan.promote(otherPl.getUniqueId());
    }

    @Subcommand("%disband")
    @CommandPermission("simpleclans.leader.disband")
    @Description("{@@command.description.disband}")
    public void disband(Player player, ClanPlayer cp, Clan clan) {
        if (clan.getLeaders().size() != 1) {
            requestManager.requestAllLeaders(cp, ClanRequest.DISBAND, clan.getTag(), "asking.to.disband",
                    player.getName());
            ChatBlock.sendMessage(player, AQUA +
                    lang("clan.disband.vote.has.been.requested.from.all.leaders", player));
            return;
        }

        new SCConversation(plugin, player, new DisbandPrompt()).begin();
    }

    @Subcommand("%verify")
    @CommandPermission("simpleclans.leader.verify")
    @Description("{@@command.description.verify}")
    public void verify(Player player, Clan clan) {
        if (clan.isVerified()) {
            ChatBlock.sendMessageKey(player, "your.clan.already.verified");
            return;
        }
        if (!settings.is(ECONOMY_PURCHASE_CLAN_VERIFY)) {
            ChatBlock.sendMessageKey(player, "staff.member.verify.clan");
            return;
        }
        int minToVerify = settings.getInt(CLAN_MIN_TO_VERIFY);
        if (minToVerify > clan.getMembers().size()) {
            ChatBlock.sendMessage(player, lang("your.clan.must.have.members.to.verify", player, minToVerify));
            return;
        }
        if (cm.purchaseVerification(player)) {
            clan.verifyClan();
            clan.addBb(player.getName(), lang("clan.0.has.been.verified", clan.getName()));
            ChatBlock.sendMessage(player, AQUA + lang("the.clan.has.been.verified", player));
        }
    }

    @Subcommand("%trust")
    @CommandPermission("simpleclans.leader.settrust")
    @CommandCompletion("@clan_members")
    @Description("{@@command.description.trust}")
    public void trust(Player player, Clan clan, @Conditions("same_clan") @Name("member") ClanPlayerInput trusted) {
        ClanPlayer trustedInput = trusted.getClanPlayer();
        if (player.getUniqueId().equals(trustedInput.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("you.cannot.trust.yourself", player));
            return;
        }
        if (clan.isLeader(trustedInput.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("leaders.are.already.trusted", player));
            return;
        }
        if (trustedInput.isTrusted()) {
            ChatBlock.sendMessage(player, ChatColor.RED + lang("this.player.is.already.trusted", player));
            return;
        }
        clan.addBb(player.getName(), lang("has.been.given.trusted.status.by", trustedInput.getName(),
                player.getName()));
        trustedInput.setTrusted(true);
        storage.updateClanPlayer(trustedInput);
    }

    @Subcommand("%untrust")
    @CommandPermission("simpleclans.leader.settrust")
    @CommandCompletion("@clan_members")
    @Description("{@@command.description.untrust}")
    public void untrust(Player player, Clan clan, @Conditions("same_clan") @Name("member") ClanPlayerInput trusted) {
        ClanPlayer trustedInput = trusted.getClanPlayer();
        if (trustedInput.getUniqueId().equals(player.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("you.cannot.untrust.yourself", player));
            return;
        }
        if (clan.isLeader(trustedInput.getUniqueId())) {
            ChatBlock.sendMessage(player, RED + lang("leaders.cannot.be.untrusted", player));
            return;
        }
        if (!trustedInput.isTrusted()) {
            ChatBlock.sendMessage(player, RED + lang("this.player.is.already.untrusted", player));
            return;
        }

        clan.addBb(player.getName(), lang("has.been.given.untrusted.status.by", trustedInput.getName(),
                player.getName()));
        trustedInput.setTrusted(false);
        storage.updateClanPlayer(trustedInput);
    }

    @Subcommand("%rename")
    @CommandPermission("simpleclans.leader.rename")
    @CommandCompletion("@nothing")
    @Description("{@@command.description.rename}")
    public void rename(Player player, ClanPlayer cp, Clan clan, @Name("name") String clanName) {
        if (clanName.contains("&")) {
            ChatBlock.sendMessageKey(cp, "your.clan.name.cannot.contain.color.codes");
            return;
        }
        boolean bypass = plugin.getPermissionsManager().has(player, "simpleclans.mod.bypass");
        if (!bypass) {
            if (ChatUtils.stripColors(clanName).length() > plugin.getSettingsManager().getInt(CLAN_MAX_LENGTH)) {
                ChatBlock.sendMessage(player, RED + lang("your.clan.name.cannot.be.longer.than.characters",
                        player, plugin.getSettingsManager().getInt(CLAN_MAX_LENGTH)));
                return;
            }
            if (ChatUtils.stripColors(clanName).length() <= plugin.getSettingsManager().getInt(CLAN_MIN_LENGTH)) {
                ChatBlock.sendMessage(player, RED + lang("your.clan.name.must.be.longer.than.characters",
                        player, plugin.getSettingsManager().getInt(CLAN_MIN_LENGTH)));
                return;
            }
        }

        if (clan.getLeaders().size() != 1) {
            requestManager.requestAllLeaders(cp, ClanRequest.RENAME, clanName, "asking.to.rename", cp.getName(), clanName);
            ChatBlock.sendMessageKey(cp, "rename.vote.has.been.requested.from.all.leaders");
            return;
        }

        clan.setName(clanName);
        storage.updateClan(clan);

        ChatBlock.sendMessageKey(cp, "you.have.successfully.renamed.your.clan", clanName);
    }

    @Subcommand("%discord %create")
    @CommandPermission("simpleclans.leader.discord.create")
    @Description("{@@command.description.discord.create}")
    @Conditions("verified")
    public void discord(Player player, Clan clan) {
        DiscordHook discordHook = chatManager.getDiscordHook();
        if (discordHook == null) {
            ChatBlock.sendMessageKey(player, "discordhook.is.disabled");
            return;
        }

        double amount = settings.getDouble(ECONOMY_DISCORD_CREATION_PRICE);
        if (settings.is(ECONOMY_PURCHASE_DISCORD_CREATE)) {
            if (!permissions.playerHasMoney(player, amount)) {
                player.sendMessage(AQUA + lang("not.sufficient.money", player, amount));
                return;
            }

            if (!permissions.chargePlayer(player, amount, DISCORD_CREATION)) {
                return;
            }
        }

        try {
            discordHook.createChannel(clan.getTag());
            ChatBlock.sendMessageKey(player, "discord.created.successfully");
        } catch (DiscordHookException ex) {
            // Return player's money if clan creation went wrong
            if (settings.is(ECONOMY_PURCHASE_DISCORD_CREATE)) {
                permissions.grantPlayer(player, amount, DISCORD_CREATION);
            }
            String messageKey = ex.getMessageKey();
            if (messageKey != null) {
                ChatBlock.sendMessage(player, RED + lang(messageKey));
            }
        }
    }
}
