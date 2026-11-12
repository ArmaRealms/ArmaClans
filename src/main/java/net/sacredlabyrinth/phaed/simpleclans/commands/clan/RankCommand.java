package net.sacredlabyrinth.phaed.simpleclans.commands.clan;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.sacredlabyrinth.phaed.simpleclans.*;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanPlayerInput;
import net.sacredlabyrinth.phaed.simpleclans.conversation.CreateRankNamePrompt;
import net.sacredlabyrinth.phaed.simpleclans.conversation.RequestCanceller;
import net.sacredlabyrinth.phaed.simpleclans.conversation.SCConversation;
import net.sacredlabyrinth.phaed.simpleclans.events.DeleteRankEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerRankUpdateEvent;
import net.sacredlabyrinth.phaed.simpleclans.managers.PermissionsManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.StorageManager;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.RED;

@CommandAlias("%clan")
@Subcommand("%rank")
@Conditions("%basic_conditions|verified")
public class RankCommand extends BaseCommand {

    @Dependency
    private SimpleClans plugin;
    @Dependency
    private StorageManager storage;
    @Dependency
    private PermissionsManager permissions;

    @Subcommand("%assign")
    @CommandPermission("simpleclans.leader.rank.assign")
    @CommandCompletion("@clan_members @ranks")
    @Description("{@@command.description.rank.assign}")
    public void assign(Player player,
                       ClanPlayer actor,
                       Clan clan,
                       @Name("member") @Conditions("same_clan") ClanPlayerInput member,
                       @Name("rank") Rank rank) {
        updateRank(player, actor, clan, member.getClanPlayer(), rank, RankPermission.RANK_ASSIGN);
    }

    @Subcommand("%unassign")
    @CommandPermission("simpleclans.leader.rank.unassign")
    @CommandCompletion("@clan_members")
    @Description("{@@command.description.rank.unassign}")
    public void unassign(Player player, ClanPlayer actor, Clan clan,
                         @Conditions("same_clan") @Name("member") ClanPlayerInput member) {
        updateRank(player, actor, clan, member.getClanPlayer(), null, RankPermission.RANK_UNASSIGN);
    }

    private void updateRank(Player player, ClanPlayer actor, Clan clan, ClanPlayer member, @Nullable Rank rank,
                            RankPermission permission) {
        if (!canUpdateRank(player, actor, clan, member, rank, permission)) {
            return;
        }
        Rank oldRank = clan.getRank(member.getRankId());
        if (!new PlayerRankUpdateEvent(actor, member, clan, oldRank, rank).callEvent()) {
            ChatBlock.sendMessage(player, RED + lang("error.event.cancelled", player));
            return;
        }
        if (!canUpdateRank(player, actor, clan, member, rank, permission)) {
            return;
        }
        if (rank != null && member.getRankId().equals(rank.getName())) {
            ChatBlock.sendMessage(player, lang("player.already.has.that.rank", player));
            return;
        }

        member.setRank(rank == null ? null : rank.getName());
        storage.updateClanPlayer(member);
        String message = rank == null ? "player.unassigned.from.rank" : "player.rank.changed";
        ChatBlock.sendMessage(player, AQUA + lang(message, player));
    }

    private boolean canUpdateRank(Player player, ClanPlayer actor, Clan clan, ClanPlayer member, @Nullable Rank rank,
                                  RankPermission permission) {
        if (!clan.isVerified()) {
            ChatBlock.sendMessage(player, RED + lang("clan.is.not.verified", player));
            return false;
        }
        if (!permissions.canManageClanMember(player, actor, clan, member, permission)) {
            return false;
        }
        if (rank != null && !clan.isAbove(actor, rank)) {
            ChatBlock.sendMessage(player, RED + lang("rank.hierarchy.destination.not.below", player));
            return false;
        }
        return true;
    }

    @Subcommand("%create")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.create")
    @Description("{@@command.description.rank.create}")
    public void create(Player player, Clan clan) {
        SCConversation conversation = new SCConversation(plugin, player, new CreateRankNamePrompt(), 60);
        conversation.addConversationCanceller(new RequestCanceller(player, RED + lang("rank.create.request.cancelled", player)));
        conversation.getContext().setSessionData("clan", clan);
        conversation.begin();
    }

    @Subcommand("%delete")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.delete")
    @CommandCompletion("@ranks")
    @Description("{@@command.description.rank.delete}")
    public void delete(Player player, Clan clan, @Name("rank") Rank rank) {
        if (!permissions.canEditClanRanks(player, clan, "simpleclans.leader.rank.delete")) {
            return;
        }
        DeleteRankEvent event = new DeleteRankEvent(player, clan, rank);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled() && permissions.canEditClanRanks(player, clan, "simpleclans.leader.rank.delete")
                && clan.getRank(rank.getName()) == rank) {
            clan.deleteRank(rank.getName());
            storage.updateClan(clan, true);
            ChatBlock.sendMessage(player, AQUA + lang("rank.0.deleted", player, rank.getDisplayName()));
        }
    }

    @Subcommand("%list")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.list")
    @Description("{@@command.description.rank.list}")
    public void list(Player player, Clan clan) {
        List<Rank> ranks = clan.getRanksInOrder();

        if (ranks.isEmpty()) {
            ChatBlock.sendMessage(player, RED + lang("no.ranks", player));
            return;
        }

        ChatBlock.sendMessage(player, AQUA + lang("clans.ranks", player));
        for (Rank rank : ranks) {
            ChatBlock.sendMessage(player, AQUA + lang("ranks.list.item", player, rank.getPosition(),
                    ChatUtils.parseColors(rank.getDisplayName()) + AQUA, rank.getName()));
        }
    }

    @Subcommand("%moveup")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.reorder")
    @CommandCompletion("@ranks")
    @Description("{@@command.description.rank.moveup}")
    public void moveUp(Player player, Clan clan, @Name("rank") Rank rank) {
        moveRank(player, clan, rank, true);
    }

    @Subcommand("%movedown")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.reorder")
    @CommandCompletion("@ranks")
    @Description("{@@command.description.rank.movedown}")
    public void moveDown(Player player, Clan clan, @Name("rank") Rank rank) {
        moveRank(player, clan, rank, false);
    }

    private void moveRank(Player player, Clan clan, Rank rank, boolean up) {
        if (!permissions.canEditClanRanks(player, clan, "simpleclans.leader.rank.reorder")) {
            return;
        }
        if (!clan.moveRank(rank, up)) {
            ChatBlock.sendMessage(player, RED + lang("rank.hierarchy.cannot.move", player));
            return;
        }
        storage.updateClan(clan);
        ChatBlock.sendMessage(player, AQUA + lang("rank.hierarchy.moved", player, rank.getDisplayName(), rank.getPosition()));
    }

    @Subcommand("%setdisplayname")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.setdisplayname")
    @CommandCompletion("@ranks @nothing")
    @Description("{@@command.description.rank.setdisplayname}")
    public void setDisplayName(Player player, Clan clan, @Name("rank") Rank rank, @Name("displayname") String displayName) {
        if (displayName.contains("&") && !permissions.has(player, "simpleclans.leader.coloredrank")) {
            ChatBlock.sendMessage(player, RED + lang("you.cannot.set.colored.ranks", player));
            return;
        }
        rank.setDisplayName(displayName);
        storage.updateClan(clan, true);
        ChatBlock.sendMessage(player, ChatColor.AQUA + lang("rank.displayname.updated", player));
    }

    @Subcommand("%setdefault")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.setdefault")
    @CommandCompletion("@ranks")
    @Description("{@@command.description.rank.setdefault}")
    public void setDefault(Player player, Clan clan, @Name("rank") Rank rank) {
        clan.setDefaultRank(rank.getName());
        ChatBlock.sendMessage(player, AQUA + lang("rank.setdefault", player, rank.getDisplayName()));
    }

    @Subcommand("%removedefault")
    @Conditions("leader")
    @CommandPermission("simpleclans.leader.rank.removedefault")
    @Description("{@@command.description.rank.removedefault}")
    public void removeDefault(Player player, Clan clan) {
        clan.setDefaultRank(null);
        ChatBlock.sendMessage(player, AQUA + lang("rank.removedefault", player));
    }

    @Subcommand("%permissions")
    @Conditions("leader")
    public class PermissionsCommand extends BaseCommand {
        private final String validPermissionsToMessage = String.join(",", Helper.fromPermissionArray());

        @Default
        @CommandPermission("simpleclans.leader.rank.permissions.available")
        @Description("{@@command.description.rank.permissions.available}")
        public void availablePermissions(Player player) {
            ChatBlock.sendMessage(player, AQUA + lang("available.rank.permissions", player));
            ChatBlock.sendMessage(player, AQUA + validPermissionsToMessage);
        }

        @Default
        @CommandPermission("simpleclans.leader.rank.permissions.list")
        @CommandCompletion("@ranks")
        @Description("{@@command.description.rank.permissions.rank}")
        public void list(Player player, @Name("rank") Rank rank) {
            Set<String> permissions = rank.getPermissions();
            if (permissions.isEmpty()) {
                ChatBlock.sendMessage(player, RED + lang("rank.no.permissions", player));
                return;
            }
            ChatBlock.sendMessage(player, AQUA + lang("rank.0.permissions", player, rank.getDisplayName()));
            ChatBlock.sendMessage(player, AQUA + String.join(",", permissions));
        }

        @Subcommand("%add")
        @CommandPermission("simpleclans.leader.rank.permissions.add")
        @CommandCompletion("@ranks @rank_permissions")
        @Description("{@@command.description.rank.permissions.add}")
        public void add(Player player,
                        Clan clan,
                        @Name("rank") Rank rank,
                        @Values("@rank_permissions") @Name("permission") String permission) {
            Set<String> permissions = rank.getPermissions();
            permissions.add(permission);
            ChatBlock.sendMessage(player, AQUA + lang("permission.0.added.to.rank.1", player, permission,
                    rank.getDisplayName()));
            storage.updateClan(clan, true);
        }

        @Subcommand("%remove")
        @CommandCompletion("@ranks @rank_permissions")
        @CommandPermission("simpleclans.leader.rank.permissions.remove")
        @Description("{@@command.description.rank.permissions.remove}")
        public void remove(Player player,
                           Clan clan,
                           @Name("rank") Rank rank,
                           @Values("@rank_permissions") @Name("permission") String permission) {
            Set<String> permissions = rank.getPermissions();
            permissions.remove(permission);
            ChatBlock.sendMessage(player, AQUA + lang("permission.0.removed.from.rank.1", player,
                    permission, rank.getDisplayName()));
            storage.updateClan(clan, true);
        }
    }
}
