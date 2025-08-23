package net.sacredlabyrinth.phaed.simpleclans.commands.clan;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpSearchTags;
import co.aikar.commands.annotation.Name;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanInput;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanPlayerInput;
import net.sacredlabyrinth.phaed.simpleclans.conversation.ResignPrompt;
import net.sacredlabyrinth.phaed.simpleclans.conversation.SCConversation;
import net.sacredlabyrinth.phaed.simpleclans.events.PrePlayerKickedClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PrePlayerLeaveClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.TagChangeEvent;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.PermissionsManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.ProtectionManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.RequestManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.StorageManager;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import net.sacredlabyrinth.phaed.simpleclans.utils.CurrencyFormat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.CLAN_MAX_ALLIANCES;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.CLAN_MAX_DESCRIPTION_LENGTH;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.CLAN_MAX_MEMBERS;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.CLAN_MIN_DESCRIPTION_LENGTH;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.ECONOMY_MAX_MEMBER_FEE;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.WAR_START_REQUEST_ENABLED;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.RED;

@CommandAlias("%clan")
@Conditions("%basic_conditions")
public class ClanCommands extends BaseCommand {
    @Dependency
    private SimpleClans plugin;
    @Dependency
    private SettingsManager settings;
    @Dependency
    private ClanManager cm;
    @Dependency
    private StorageManager storage;
    @Dependency
    private PermissionsManager permissions;
    @Dependency
    private RequestManager requestManager;
    @Dependency
    private ProtectionManager protectionManager;

    @Subcommand("%war %start")
    @CommandPermission("simpleclans.leader.war")
    @Conditions("verified|rank:name=WAR_START")
    @Description("{@@command.description.war.start}")
    @CommandCompletion("@rivals")
    public void startWar(final Player player, final ClanPlayer requester, final Clan requestClan, @Conditions("can_war_target") @Name("clan") final ClanInput targetClanInput) {
        final Clan targetClan = targetClanInput.getClan();

        final List<ClanPlayer> onlineLeaders = Helper.stripOffLinePlayers(requestClan.getLeaders());

        if (settings.is(WAR_START_REQUEST_ENABLED)) {
            if (!onlineLeaders.isEmpty()) {
                requestManager.addWarStartRequest(requester, targetClan, requestClan);
                ChatBlock.sendMessage(player, AQUA + lang("leaders.have.been.asked.to.accept.the.war.request",
                        player, targetClan.getName()));
            } else {
                ChatBlock.sendMessage(player, RED + lang("at.least.one.leader.accept.the.alliance", player));
            }
        } else {
            protectionManager.addWar(requester, requestClan, targetClan);
        }
    }

    @Subcommand("%war %end")
    @CommandPermission("simpleclans.leader.war")
    @Conditions("verified|rank:name=WAR_END")
    @Description("{@@command.description.war.end}")
    @CommandCompletion("@warring_clans")
    public void endWar(final ClanPlayer cp, final Clan issuerClan, @Name("clan") final ClanInput other) {
        final Clan war = other.getClan();
        if (issuerClan.isWarring(war.getTag())) {
            requestManager.addWarEndRequest(cp, war, issuerClan);
            ChatBlock.sendMessage(cp, AQUA + lang("leaders.asked.to.end.rivalry", cp, war.getName()));
        } else {
            ChatBlock.sendMessage(cp, RED + lang("clans.not.at.war", cp));
        }
    }

    @Subcommand("%modtag")
    @CommandPermission("simpleclans.leader.modtag")
    @Conditions("verified|rank:name=MODTAG")
    @Description("{@@command.description.modtag}")
    public void modtag(final Player player, final Clan clan, @Single @Name("tag") String tag) {
        final TagChangeEvent event = new TagChangeEvent(player, clan, tag);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        tag = event.getNewTag();
        final String cleanTag = Helper.cleanTag(tag);

        final Optional<String> validationError = plugin.getTagValidator().validate(player, tag);
        if (validationError.isPresent()) {
            ChatBlock.sendMessage(player, validationError.get());
            return;
        }

        if (!cleanTag.equals(clan.getTag())) {
            ChatBlock.sendMessage(player, RED + lang("you.can.only.modify.the.color.and.case.of.the.tag",
                    player));
            return;
        }

        clan.addBb(player.getName(), lang("tag.changed.to.0", ChatUtils.parseColors(tag)));
        clan.changeClanTag(tag);
        cm.updateDisplayName(player);
    }

    @Subcommand("%setbanner")
    @CommandPermission("simpleclans.leader.setbanner")
    @Conditions("verified|rank:name=SETBANNER")
    @Description("{@@command.description.setbanner}")
    public void setbanner(final Player player, final Clan clan) {
        @SuppressWarnings("deprecation") final ItemStack hand = player.getItemInHand();
        if (!hand.getType().toString().contains("BANNER")) {
            ChatBlock.sendMessageKey(player, "you.must.hold.a.banner");
            return;
        }

        clan.setBanner(hand);
        storage.updateClan(clan);
        ChatBlock.sendMessageKey(player, "you.changed.clan.banner");
    }

    @Subcommand("%invite")
    @CommandPermission("simpleclans.leader.invite")
    @CommandCompletion("@non_members:ignore_vanished")
    @Conditions("rank:name=INVITE")
    @Description("{@@command.description.invite}")
    public void invite(final Player sender, final ClanPlayer cp, final Clan clan,
                       @Conditions("not_banned|not_in_clan|online:ignore_vanished") @Name("player") final ClanPlayerInput invited) {
        if (!invited.getClanPlayer().isInviteEnabled()) {
            ChatBlock.sendMessage(sender, RED + lang("invitedplayer.invite.off", sender));
            return;
        }
        final Player invitedPlayer = invited.getClanPlayer().toPlayer();
        if (invitedPlayer == null) return;
        if (!permissions.has(invitedPlayer, "simpleclans.member.can-join")) {
            ChatBlock.sendMessage(sender, RED +
                    lang("the.player.doesn.t.not.have.the.permissions.to.join.clans", sender));
            return;
        }
        if (invitedPlayer.getUniqueId().equals(sender.getUniqueId())) {
            ChatBlock.sendMessage(sender, RED + lang("you.cannot.invite.yourself", sender));
            return;
        }
        final long minutesBeforeRejoin = cm.getMinutesBeforeRejoin(invited.getClanPlayer(), clan);
        if (minutesBeforeRejoin > 0L) {
            ChatBlock.sendMessage(sender, RED +
                    lang("the.player.must.wait.0.before.joining.your.clan.again", sender, minutesBeforeRejoin));
            return;
        }

        if (clan.getSize() >= settings.getInt(CLAN_MAX_MEMBERS) && settings.getInt(CLAN_MAX_MEMBERS) > 0) {
            ChatBlock.sendMessage(sender, RED + lang("the.clan.members.reached.limit", sender));
            return;
        }
        if (!cm.purchaseInvite(sender)) {
            return;
        }

        requestManager.addInviteRequest(cp, invitedPlayer.getName(), clan);
        ChatBlock.sendMessage(sender, AQUA + lang("has.been.asked.to.join",
                sender, invitedPlayer.getName(), clan.getName()));
    }

    @Subcommand("%fee %check")
    @Conditions("member_fee_enabled|verified")
    @CommandPermission("simpleclans.member.fee-check")
    @Description("{@@command.description.fee.check}")
    public void checkFee(final Player player, final Clan clan) {
        ChatBlock.sendMessage(player, AQUA
                + lang("the.fee.is.0.and.its.current.value.is.1", player, clan.isMemberFeeEnabled() ?
                        lang("fee.enabled", player) : lang("fee.disabled", player),
                clan.getMemberFee()
        ));
    }

    @Subcommand("%fee %set")
    @CommandPermission("simpleclans.leader.fee")
    @Conditions("rank:name=FEE_SET|change_fee")
    @Description("{@@command.description.fee.set}")
    public void setFee(final Player player, final Clan clan, @Name("fee") double fee) {
        fee = Math.abs(fee);
        final double maxFee = settings.getDouble(ECONOMY_MAX_MEMBER_FEE);
        if (fee > maxFee) {
            ChatBlock.sendMessage(player, RED
                    + lang("max.fee.allowed.is.0", player, CurrencyFormat.format(maxFee)));
            return;
        }
        if (cm.purchaseMemberFeeSet(player)) {
            clan.setMemberFee(fee);
            clan.addBb(player.getName(), lang("bb.fee.set", CurrencyFormat.format(fee)));
            ChatBlock.sendMessage(player, AQUA + lang("fee.set", player));
            storage.updateClan(clan);
        }
    }

    @Subcommand("%clanff %allow")
    @CommandPermission("simpleclans.leader.ff")
    @Conditions("rank:name=FRIENDLYFIRE")
    @Description("{@@command.description.clanff.allow}")
    public void allowClanFf(final Player player, final Clan clan) {
        clan.addBb(player.getName(), lang("clan.wide.friendly.fire.is.allowed"));
        clan.setFriendlyFire(true);
        storage.updateClan(clan);
    }

    @Subcommand("%clanff %block")
    @CommandPermission("simpleclans.leader.ff")
    @Description("{@@command.description.clanff.block}")
    public void blockClanFf(final Player player, final Clan clan) {
        clan.addBb(player.getName(), lang("clan.wide.friendly.fire.blocked"));
        clan.setFriendlyFire(false);
        storage.updateClan(clan);
    }

    @Subcommand("%description")
    @CommandPermission("simpleclans.leader.description")
    @Conditions("verified|rank:name=DESCRIPTION")
    @Description("{@@command.description.description}")
    public void setDescription(final Player player, final Clan clan, @Name("description") final String description) {
        if (description.length() < settings.getInt(CLAN_MIN_DESCRIPTION_LENGTH)) {
            ChatBlock.sendMessage(player, RED + lang("your.clan.description.must.be.longer.than",
                    player, settings.getInt(CLAN_MIN_DESCRIPTION_LENGTH)));
            return;
        }
        if (description.length() > settings.getInt(CLAN_MAX_DESCRIPTION_LENGTH)) {
            ChatBlock.sendMessage(player, RED + lang("your.clan.description.cannot.be.longer.than",
                    player, settings.getInt(CLAN_MAX_DESCRIPTION_LENGTH)));
            return;
        }
        clan.setDescription(description);
        ChatBlock.sendMessage(player, AQUA + lang("description.changed", player));
        storage.updateClan(clan);
    }

    @Subcommand("%rival %add")
    @CommandPermission("simpleclans.leader.rival")
    @Conditions("verified|rivable|minimum_to_rival|rank:name=RIVAL_ADD")
    @CommandCompletion("@clans:hide_own")
    @Description("{@@command.description.rival.add}")
    public void addRival(final Player player, final Clan issuerClan, @Conditions("verified|different") @Name("clan") final ClanInput rival) {
        final Clan rivalInput = rival.getClan();
        if (settings.isUnrivable(rivalInput.getTag())) {
            ChatBlock.sendMessage(player, RED + lang("the.clan.cannot.be.rivaled", player));
            return;
        }
        if (!issuerClan.reachedRivalLimit()) {
            if (!issuerClan.isRival(rivalInput.getTag())) {
                issuerClan.addRival(rivalInput);
                rivalInput.addBb(player.getName(), lang("has.initiated.a.rivalry", issuerClan.getName(),
                        rivalInput.getName()), false);
                issuerClan.addBb(player.getName(), lang("has.initiated.a.rivalry", player.getName(),
                        rivalInput.getName()));
            } else {
                ChatBlock.sendMessage(player, RED + lang("your.clans.are.already.rivals", player));
            }
        } else {
            ChatBlock.sendMessage(player, RED + lang("rival.limit.reached", player));
        }
    }

    @Subcommand("%rival %remove")
    @CommandPermission("simpleclans.leader.rival")
    @Conditions("verified|rank:name=RIVAL_REMOVE")
    @CommandCompletion("@rivals")
    @Description("{@@command.description.rival.remove}")
    public void removeRival(final Player player,
                            final ClanPlayer cp,
                            final Clan issuerClan,
                            @Conditions("different") @Name("clan") final ClanInput rival) {
        final Clan rivalInput = rival.getClan();
        if (issuerClan.isRival(rivalInput.getTag())) {
            requestManager.addRivalryBreakRequest(cp, rivalInput, issuerClan);
            ChatBlock.sendMessage(player, AQUA + lang("leaders.asked.to.end.rivalry", player,
                    rivalInput.getName()));
        } else {
            ChatBlock.sendMessage(player, RED + lang("your.clans.are.not.rivals", player));
        }
    }

    @Subcommand("%ally %add")
    @CommandPermission("simpleclans.leader.ally")
    @Conditions("verified|rank:name=ALLY_ADD|minimum_to_ally")
    @CommandCompletion("@clans:hide_own")
    @Description("{@@command.description.ally.add}")
    public void addAlly(final Player player,
                        final ClanPlayer cp,
                        final Clan issuerClan,
                        @Conditions("verified|different") @Name("clan") final ClanInput other) {
        final Clan input = other.getClan();
        if (issuerClan.isAlly(input.getTag())) {
            ChatBlock.sendMessage(player, RED + lang("your.clans.are.already.allies", player));
            return;
        }
        final int maxAlliances = settings.getInt(CLAN_MAX_ALLIANCES);
        if (maxAlliances != -1) {
            if (issuerClan.getAllies().size() >= maxAlliances) {
                ChatBlock.sendMessage(player, lang("your.clan.reached.max.alliances", player));
                return;
            }
            if (input.getAllies().size() >= maxAlliances) {
                ChatBlock.sendMessage(player, lang("other.clan.reached.max.alliances", player));
                return;
            }
        }

        final List<ClanPlayer> onlineLeaders = Helper.stripOffLinePlayers(issuerClan.getLeaders());
        if (onlineLeaders.isEmpty()) {
            ChatBlock.sendMessage(player, RED + lang("at.least.one.leader.accept.the.alliance",
                    player));
            return;
        }

        requestManager.addAllyRequest(cp, input, issuerClan);
        ChatBlock.sendMessage(player, AQUA + lang("leaders.have.been.asked.for.an.alliance",
                player, input.getName()));
    }

    @Subcommand("%ally %remove")
    @Conditions("verified|rank:name=ALLY_REMOVE")
    @CommandPermission("simpleclans.leader.ally")
    @Description("{@@command.description.ally.remove}")
    @CommandCompletion("@allied_clans")
    public void removeAlly(final Player player, final Clan issuerClan, @Conditions("different|allied_clan") @Name("clan") final ClanInput ally) {
        final Clan allyInput = ally.getClan();
        issuerClan.removeAlly(allyInput);
        allyInput.addBb(player.getName(), lang("has.broken.the.alliance", issuerClan.getName(),
                allyInput.getName()), false);
        issuerClan.addBb(player.getName(), lang("has.broken.the.alliance", player.getName(),
                allyInput.getName()));
    }

    @Subcommand("%kick")
    @CommandPermission("simpleclans.leader.kick")
    @CommandCompletion("@clan_members:hide_own")
    @Conditions("rank:name=KICK")
    @Description("{@@command.description.kick}")
    public void kick(@Conditions("clan_member") final Player sender,
                     @Conditions("same_clan") @Name("member") final ClanPlayerInput other) {
        final ClanPlayer clanPlayer = other.getClanPlayer();
        if (sender.getUniqueId().equals(clanPlayer.getUniqueId())) {
            ChatBlock.sendMessage(sender, RED + lang("you.cannot.kick.yourself", sender));
            return;
        }
        final Clan clan = cm.getClanByPlayerUniqueId(sender.getUniqueId());
        if (Objects.requireNonNull(clan).isLeader(clanPlayer.getUniqueId())) {
            ChatBlock.sendMessage(sender, RED + lang("you.cannot.kick.another.leader", sender));
            return;
        }
        if (!new PrePlayerKickedClanEvent(clan, clanPlayer).callEvent()) {
            ChatBlock.sendMessage(sender, RED + lang("error.event.cancelled", sender));
            return;
        }
        clan.addBb(sender.getName(), lang("has.been.kicked.by", clanPlayer.getName(), sender.getName(), sender));
        clan.removePlayerFromClan(clanPlayer.getUniqueId());
    }

    @Subcommand("%resign %confirm")
    @CommandPermission("simpleclans.member.resign")
    @Description("{@@command.description.resign}")
    @HelpSearchTags("leave")
    public void resignConfirm(final Player player, final ClanPlayer cp, final Clan clan) {
        if (!new PrePlayerLeaveClanEvent(player, player).callEvent()) {
            ChatBlock.sendMessage(player, RED + lang("error.event.cancelled", player));
        } else if (clan.isPermanent() || !clan.isLeader(player) || clan.getLeaders().size() > 1) {
            clan.addBb(player.getName(), lang("0.has.resigned", player.getName()));
            cp.addResignTime(clan.getTag());
            clan.removePlayerFromClan(player.getUniqueId());
            ChatBlock.sendMessage(cp, AQUA + lang("resign.success", player));
        } else if (clan.isLeader(player) && clan.getLeaders().size() == 1) {
            clan.disband(player, true, false);
            ChatBlock.sendMessage(cp, RED + lang("clan.has.been.disbanded", player, clan.getName()));
        } else {
            ChatBlock.sendMessage(cp, RED + lang("last.leader.cannot.resign.you.must.appoint.another.leader.or.disband.the.clan", player));
        }
    }

    @Subcommand("%resign")
    @CommandPermission("simpleclans.member.resign")
    @Description("{@@command.description.resign}")
    @HelpSearchTags("leave")
    public void resign(@Conditions("clan_member") final Player player) {
        if (!new PrePlayerLeaveClanEvent(player, player).callEvent()) {
            ChatBlock.sendMessage(player, RED + lang("error.event.cancelled", player));
            return;
        }
        new SCConversation(plugin, player, new ResignPrompt()).begin();
    }
}
