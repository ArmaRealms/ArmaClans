package net.sacredlabyrinth.phaed.simpleclans.ui.frames;

import com.cryptomorin.xseries.XMaterial;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Rank;
import net.sacredlabyrinth.phaed.simpleclans.RankPermission;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.ui.*;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import net.sacredlabyrinth.phaed.simpleclans.utils.Paginator;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;

public class RanksFrame extends SCFrame {
	private final Paginator paginator;
	private final OfflinePlayer toEdit;
	private final Clan clan;
	private final List<Rank> ranks = new ArrayList<>();

	public RanksFrame(SCFrame parent, Player viewer, Clan subject, @Nullable OfflinePlayer toEdit) {
		super(parent, viewer);
		this.toEdit = toEdit;
		this.clan = subject;
		paginator = new Paginator(getSize() - 9, ranks);
	}

	@Override
	public void createComponents() {
		ranks.clear();
		if (clan != null) {
			ranks.addAll(clan.getRanksInOrder());
		}
		while (!paginator.isValidIndex(paginator.getMinIndex()) && paginator.previousPage()) {
			// A deletion can remove the current page.
		}
		for (int slot = 0; slot < 9; slot++) {
			if (slot == 2 || slot == 6 || slot == 7 || (slot == 4 && toEdit == null))
				continue;
			add(Components.getPanelComponent(slot));
		}
		
		add(Components.getBackComponent(getParent(), 2, getViewer()));

		if (toEdit == null) {
			SCComponent create = new SCComponentImpl(lang("gui.ranks.create.title", getViewer()),
					Collections.singletonList(lang("gui.ranks.create.lore", getViewer())), XMaterial.WHITE_WOOL, 4);
			create.setVerifiedOnly(ClickType.LEFT);
			create.setListener(ClickType.LEFT, () -> InventoryController.runClanSubcommand(getViewer(), clan, "rank create", false));
			create.setPermission(ClickType.LEFT, "simpleclans.leader.rank.create");
			add(create);
		}

		add(Components.getPreviousPageComponent(6, this::previousPage, paginator, getViewer()));
		add(Components.getNextPageComponent(7, this::nextPage, paginator, getViewer()));
		ClanPlayer member = toEdit == null ? null : SimpleClans.getInstance().getClanManager().getAnyClanPlayer(toEdit.getUniqueId());
		String memberName = member == null ? "" : member.getName();

		int slot = 9;
		for (int i = paginator.getMinIndex(); paginator.isValidIndex(i); i++) {
			Rank rank = ranks.get(i);
			List<String> lore = new ArrayList<>();
			lore.add(lang("gui.ranks.rank.displayname.lore", getViewer(), ChatUtils.parseColors(rank.getDisplayName())));
			lore.add(lang("gui.ranks.rank.position.lore", getViewer(), rank.getPosition()));
			if (toEdit == null) {
				lore.add(lang("gui.ranks.rank.edit.permissions.lore", getViewer()));
				lore.add(lang("gui.ranks.rank.remove.lore", getViewer()));
				if (i > 0) lore.add(lang("gui.ranks.rank.moveup.lore", getViewer()));
				if (i < ranks.size() - 1) lore.add(lang("gui.ranks.rank.movedown.lore", getViewer()));
			} else {
				lore.add(lang("gui.ranks.rank.assign.lore", getViewer(), memberName));
			}
			SCComponent c = new SCComponentImpl(lang("gui.ranks.rank.title",getViewer(), rank.getName()), lore,
					XMaterial.FILLED_MAP, slot);
			if (toEdit != null) {
				c.setListener(ClickType.LEFT, () -> InventoryController.runMemberSubcommand(getViewer(), clan,
						toEdit.getUniqueId(), "rank assign", rank.getName()));
				c.setConfirmationRequired(ClickType.LEFT);
				c.setPermission(ClickType.LEFT, RankPermission.RANK_ASSIGN);
			} else {
				c.setListener(ClickType.LEFT, () -> InventoryDrawer.open(new PermissionsFrame(this, getViewer(), clan, rank)));
				c.setPermission(ClickType.LEFT, "simpleclans.leader.rank.permissions.list");
				c.setListener(ClickType.RIGHT, () -> InventoryController.runClanSubcommand(getViewer(), clan,
						"rank delete", true, rank.getName()));
				c.setConfirmationRequired(ClickType.RIGHT);
				c.setPermission(ClickType.RIGHT, "simpleclans.leader.rank.delete");
				if (i > 0) addMoveAction(c, rank, ClickType.SHIFT_LEFT, "rank moveup");
				if (i < ranks.size() - 1) addMoveAction(c, rank, ClickType.SHIFT_RIGHT, "rank movedown");
			}
			c.setVerifiedOnly(ClickType.LEFT);
			c.setVerifiedOnly(ClickType.RIGHT);
			add(c);
			slot++;
		}
	}

	private void addMoveAction(SCComponent component, Rank rank, ClickType click, String command) {
		component.setListener(click, () -> InventoryController.runClanSubcommand(getViewer(), clan, command, true, rank.getName()));
		component.setPermission(click, "simpleclans.leader.rank.reorder");
		component.setVerifiedOnly(click);
	}

	private void previousPage() {
		if (paginator.previousPage()) {
			updateFrame();
		}
	}

	private void nextPage() {
		if (paginator.nextPage()) {
			updateFrame();
		}
	}

	private void updateFrame() {
		InventoryDrawer.open(this);
	}

	@Override
	public @NotNull String getTitle() {
		if (toEdit != null) {
			ClanPlayer member = SimpleClans.getInstance().getClanManager().getAnyClanPlayer(toEdit.getUniqueId());
			String rank = member == null ? "" : member.getRankId();
			return lang("gui.ranks.title.set.rank",getViewer(), rank);
		}
		return lang("gui.ranks.title",getViewer());
	}

	@Override
	public int getSize() {
		return 3 * 9;
	}

}
