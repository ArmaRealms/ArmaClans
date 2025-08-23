package net.sacredlabyrinth.phaed.simpleclans.commands.general;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandParameter;
import co.aikar.commands.HelpEntry;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpSearchTags;
import co.aikar.commands.annotation.Name;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Values;
import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanInput;
import net.sacredlabyrinth.phaed.simpleclans.commands.ClanPlayerInput;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.Alliances;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.ClanList;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.ClanProfile;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.ClanRoster;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.Kills;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.Leaderboard;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.Lookup;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.MostKilled;
import net.sacredlabyrinth.phaed.simpleclans.commands.data.Rivalries;
import net.sacredlabyrinth.phaed.simpleclans.conversation.CreateClanTagPrompt;
import net.sacredlabyrinth.phaed.simpleclans.conversation.RequestCanceller;
import net.sacredlabyrinth.phaed.simpleclans.conversation.ResetKdrPrompt;
import net.sacredlabyrinth.phaed.simpleclans.conversation.SCConversation;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerResetKdrEvent;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.RequestManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager;
import net.sacredlabyrinth.phaed.simpleclans.managers.StorageManager;
import net.sacredlabyrinth.phaed.simpleclans.ui.InventoryDrawer;
import net.sacredlabyrinth.phaed.simpleclans.ui.frames.MainFrame;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.conversation.CreateClanNamePrompt.NAME_KEY;
import static net.sacredlabyrinth.phaed.simpleclans.conversation.CreateClanTagPrompt.TAG_KEY;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.ALLOW_RESET_KDR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.COMMANDS_MORE;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.ENABLE_GUI;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.LANGUAGE_SELECTOR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PAGE_CLAN_NAME_COLOR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PAGE_HEADINGS_COLOR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PAGE_SIZE;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.SERVER_NAME;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.TAG_BRACKET_COLOR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.TAG_BRACKET_LEFT;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.TAG_BRACKET_RIGHT;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;

@CommandAlias("%clan")
@Conditions("%basic_conditions")
public class GeneralCommands extends BaseCommand {

    @Dependency
    private SimpleClans plugin;
    @Dependency
    private ClanManager cm;
    @Dependency
    private SettingsManager settings;
    @Dependency
    private StorageManager storage;
    @Dependency
    private RequestManager requestManager;

    @Default
    @Description("{@@command.description.clan}")
    @HelpSearchTags("menu gui interface ui")
    public void main(final CommandSender sender) {
        if (sender instanceof Player && settings.is(ENABLE_GUI)) {
            InventoryDrawer.open(new MainFrame((Player) sender));
        } else {
            help(sender, new CommandHelp(getCurrentCommandManager(),
                    getCurrentCommandManager().getRootCommand(getName()), getCurrentCommandIssuer()));
        }
    }

    @Subcommand("%locale")
    @CommandPermission("simpleclans.anyone.locale")
    @Description("{@@command.description.locale}")
    @CommandCompletion("@locales")
    public void locale(final ClanPlayer cp, @Values("@locales") @Name("locale") @Single final String locale) {
        if (!settings.is(LANGUAGE_SELECTOR)) {
            ChatBlock.sendMessageKey(cp, "locale.is.prohibited");
            return;
        }

        cp.setLocale(Helper.forLanguageTag(locale.replace("_", "-")));
        plugin.getStorageManager().updateClanPlayer(cp);

        ChatBlock.sendMessageKey(cp, "locale.has.been.changed");
    }

    @Subcommand("%create")
    @CommandPermission("simpleclans.leader.create")
    @CommandCompletion("%compl:tag %compl:name")
    @Description("{@@command.description.create}")
    public void create(final Player player, @Optional @Name("tag") final String tag, @Optional @Name("name") final String name) {
        final ClanPlayer cp = cm.getAnyClanPlayer(player.getUniqueId());

        if (cp != null) {
            if (cp.getClan() != null) {
                ChatBlock.sendMessage(player, RED + lang("you.must.first.resign", player,
                        cp.getClan().getName()));
                return;
            }

            final long minutesBeforeAction = cm.getMinutesBeforeAction(cp);
            if (minutesBeforeAction > 0L) {
                ChatBlock.sendMessage(player, RED + lang("you.must.wait.0.before.creating.a.clan", player, minutesBeforeAction));
                return;
            }
        }

        final HashMap<Object, Object> initialData = new HashMap<>();
        initialData.put(TAG_KEY, tag);
        initialData.put(NAME_KEY, name);
        final SCConversation conversation = new SCConversation(plugin, player, new CreateClanTagPrompt(), initialData);
        conversation.addConversationCanceller(new RequestCanceller(player, RED + lang("clan.create.request.cancelled", player)));
        conversation.begin();
    }

    @Subcommand("%leaderboard")
    @CommandPermission("simpleclans.anyone.leaderboard")
    @Description("{@@command.description.leaderboard}")
    public void leaderboard(final CommandSender sender) {
        final Leaderboard l = new Leaderboard(plugin, sender);
        l.send();
    }

    @Subcommand("%lookup")
    @CommandCompletion("@players")
    @CommandPermission("simpleclans.anyone.lookup")
    @Description("{@@command.description.lookup.other}")
    public void lookup(final CommandSender sender, @Name("player") final ClanPlayerInput player) {
        final Lookup l = new Lookup(plugin, sender, player.getClanPlayer().getUniqueId());
        l.send();
    }

    @Subcommand("%lookup")
    @CommandPermission("simpleclans.member.lookup")
    @Description("{@@command.description.lookup}")
    public void lookup(final Player sender) {
        final Lookup l = new Lookup(plugin, sender, sender.getUniqueId());
        l.send();
    }

    @Subcommand("%kills")
    @CommandPermission("simpleclans.member.kills")
    @Conditions("verified|rank:name=KILLS")
    @CommandCompletion("@players")
    @Description("{@@command.description.kills}")
    public void kills(final Player sender, @Optional @Name("player") final ClanPlayerInput player) {
        String name = sender.getName();
        if (player != null) {
            name = player.getClanPlayer().getName();
        }
        final Kills k = new Kills(plugin, sender, name);
        k.send();
    }

    @Subcommand("%profile")
    @CommandPermission("simpleclans.anyone.profile")
    @CommandCompletion("@clans:hide_own")
    @Description("{@@command.description.profile.other}")
    public void profile(final CommandSender sender, @Conditions("verified") @Name("clan") final ClanInput clan) {
        final ClanProfile p = new ClanProfile(plugin, sender, clan.getClan());
        p.send();
    }

    @Subcommand("%roster")
    @CommandCompletion("@clans:hide_own")
    @CommandPermission("simpleclans.anyone.roster")
    @Description("{@@command.description.roster.other}")
    public void roster(final CommandSender sender, @Conditions("verified") @Name("clan") final ClanInput clan) {
        final ClanRoster r = new ClanRoster(plugin, sender, clan.getClan());
        r.send();
    }

    @Subcommand("%ff %allow")
    @CommandPermission("simpleclans.member.ff")
    @Description("{@@command.description.ff.allow}")
    public void allowPersonalFf(final Player player, final ClanPlayer cp) {
        cp.setFriendlyFire(true);
        storage.updateClanPlayer(cp);
        ChatBlock.sendMessage(player, AQUA + lang("personal.friendly.fire.is.set.to.allowed", player));
    }

    @Subcommand("%ff %auto")
    @CommandPermission("simpleclans.member.ff")
    @Description("{@@command.description.ff.auto}")
    public void autoPersonalFf(final Player player, final ClanPlayer cp) {
        cp.setFriendlyFire(false);
        storage.updateClanPlayer(cp);
        ChatBlock.sendMessage(player, AQUA + lang("friendy.fire.is.now.managed.by.your.clan", player));
    }

    @Subcommand("%resetkdr %confirm")
    @CommandPermission("simpleclans.vip.resetkdr")
    @Description("{@@command.description.resetkdr}")
    public void resetKdrConfirm(final Player player, final ClanPlayer cp) {
        if (!settings.is(ALLOW_RESET_KDR)) {
            ChatBlock.sendMessage(player, RED + lang("disabled.command", player));
            return;
        }
        final PlayerResetKdrEvent event = new PlayerResetKdrEvent(cp);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() && cm.purchaseResetKdr(player)) {
            cm.resetKdr(cp);
            ChatBlock.sendMessage(player, RED + lang("you.have.reseted.your.kdr", player));
        }
    }

    @Subcommand("%resetkdr")
    @CommandPermission("simpleclans.vip.resetkdr")
    @Description("{@@command.description.resetkdr}")
    public void resetKdr(final Player player, final ClanPlayer cp) {
        if (!settings.is(ALLOW_RESET_KDR)) {
            ChatBlock.sendMessage(player, RED + lang("disabled.command", player));
        } else {
            new SCConversation(plugin, player, new ResetKdrPrompt(cm), 60).begin();
        }
    }

    @CommandAlias("%accept")
    @Description("{@@command.description.accept}")
    @Conditions("can_vote")
    public void accept(final Player player, final ClanPlayer cp) {
        final Clan clan = cp.getClan();
        if (clan != null) {
            clan.leaderAnnounce(GREEN + lang("voted.to.accept", player.getName()));
        }
        requestManager.accept(cp);
    }

    @CommandAlias("%deny")
    @Description("{@@command.description.deny}")
    @Conditions("can_vote")
    public void deny(final Player player, final ClanPlayer cp) {
        final Clan clan = cp.getClan();
        if (clan != null) {
            clan.leaderAnnounce(RED + lang("has.voted.to.deny", player.getName()));
        }
        requestManager.deny(cp);
    }

    @CommandAlias("%more")
    @Description("{@@command.description.more}")
    public void more(final Player player) {
        final ChatBlock chatBlock = storage.getChatBlock(player);

        if (chatBlock == null || chatBlock.size() <= 0) {
            ChatBlock.sendMessage(player, RED + lang("nothing.more.to.see", player));
            return;
        }

        chatBlock.sendBlock(player, settings.getInt(PAGE_SIZE));

        if (chatBlock.size() > 0) {
            ChatBlock.sendBlank(player);
            ChatBlock.sendMessage(player, settings.getColored(PAGE_HEADINGS_COLOR) + lang("view.next.page", player,
                    settings.getString(COMMANDS_MORE)));
        }
        ChatBlock.sendBlank(player);
    }

    @CatchUnknown
    @Subcommand("%help")
    @Description("{@@command.description.help}")
    public void help(final CommandSender sender, final CommandHelp help) {
        final boolean inClan = sender instanceof final Player player && cm.getClanByPlayerUniqueId(player.getUniqueId()) != null;
        for (final HelpEntry helpEntry : help.getHelpEntries()) {
            for (@SuppressWarnings("rawtypes") final CommandParameter parameter : helpEntry.getParameters()) {
                if (parameter.getType().equals(Clan.class) && !inClan) {
                    helpEntry.setSearchScore(0);
                }
            }
        }
        help.showHelp();
    }

    @Subcommand("%mostkilled")
    @CommandPermission("simpleclans.mod.mostkilled")
    @Conditions("verified|rank:name=MOSTKILLED")
    @Description("{@@command.description.mostkilled}")
    public void mostKilled(final Player player) {
        final MostKilled mk = new MostKilled(plugin, player);
        mk.send();
    }

    @Subcommand("%list %balance")
    @CommandPermission("simpleclans.anyone.list.balance")
    @Description("{@@command.description.list.balance}")
    public void listBalance(final CommandSender sender) {
        final List<Clan> clans = cm.getClans();
        if (clans.isEmpty()) {
            sender.sendMessage(RED + lang("no.clans.have.been.created", sender));
            return;
        }
        clans.sort(Comparator.comparingDouble(Clan::getBalance).reversed());

        sender.sendMessage(lang("clan.list.balance.header", sender, settings.getColored(SERVER_NAME), clans.size()));
        final String lineFormat = lang("clan.list.balance.line", sender);

        final String leftBracket = settings.getColored(TAG_BRACKET_COLOR) + settings.getColored(TAG_BRACKET_LEFT);
        final String rightBracket = settings.getColored(TAG_BRACKET_COLOR) + settings.getColored(TAG_BRACKET_RIGHT);
        for (int i = 0; i < 10 && i < clans.size(); i++) {
            final Clan clan = clans.get(i);
            final String name = " " + (clan.isVerified() ? settings.getColored(PAGE_CLAN_NAME_COLOR) : GRAY) + clan.getName();
            final String line = MessageFormat.format(lineFormat, i + 1, leftBracket, clan.getColorTag(),
                    rightBracket, name, clan.getBalanceFormatted());
            sender.sendMessage(line);
        }
    }

    @Subcommand("%list")
    @CommandPermission("simpleclans.anyone.list")
    @Description("{@@command.description.list}")
    @CommandCompletion("@clan_list_type @order")
    public void list(final CommandSender sender, @Optional @Values("@clan_list_type") final String type,
                     @Optional @Single @Values("@order") final String order) {
        final ClanList list = new ClanList(plugin, sender, type, order);
        list.send();
    }

    @Subcommand("%rivalries")
    @CommandPermission("simpleclans.anyone.rivalries")
    @Description("{@@command.description.rivalries}")
    public void rivalries(final CommandSender sender) {
        final Rivalries rivalries = new Rivalries(plugin, sender);
        rivalries.send();
    }

    @Subcommand("%alliances")
    @CommandPermission("simpleclans.anyone.alliances")
    @Description("{@@command.description.alliances}")
    public void alliances(final CommandSender sender) {
        final Alliances a = new Alliances(plugin, sender);
        a.send();
    }

}
