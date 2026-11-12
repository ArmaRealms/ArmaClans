package net.sacredlabyrinth.phaed.simpleclans;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RankHierarchyTest {

    @Test
    void migratesLegacyOrderAndPersistsPositionsIndependentlyOfRankProperties() {
        String legacy = """
                {"ranks":[
                    {"name":"recruiter","displayName":"Recruiter","permissions":["kick"]},
                    {"name":"admin","displayName":"Admin","permissions":[]}
                ],"defaultRank":"recruiter"}
                """;
        Clan clan = new Clan();
        clan.setRanks(Helper.ranksFromJson(legacy));
        clan.setDefaultRank(Helper.defaultRankFromJson(legacy));
        Rank recruiter = clan.getRank("recruiter");
        Rank admin = clan.getRank("admin");

        assertEquals(1, recruiter.getPosition());
        assertEquals(2, admin.getPosition());
        assertEquals(Set.of("kick"), recruiter.getPermissions());
        assertTrue(clan.moveRank(admin, true));
        admin.setName("manager");
        admin.setDisplayName("&bManager");
        recruiter.getPermissions().add("invite");
        clan.createRank("new");

        // Explicit positions survive even if the JSON array order changes.
        String saved = Helper.ranksToJson(List.of(recruiter, clan.getRank("new"), admin), clan.getDefaultRank());
        Clan reloaded = new Clan();
        reloaded.setRanks(Helper.ranksFromJson(saved));
        reloaded.setDefaultRank(Helper.defaultRankFromJson(saved));

        assertEquals(List.of("manager", "recruiter", "new"), reloaded.getRanksInOrder().stream().map(Rank::getName).toList());
        assertEquals(1, reloaded.getRank("manager").getPosition());
        assertEquals(3, reloaded.getRank("new").getPosition());
        assertEquals("&bManager", reloaded.getRank("manager").getDisplayName());
        assertEquals(Set.of("kick", "invite"), reloaded.getRank("recruiter").getPermissions());
        assertEquals("recruiter", reloaded.getDefaultRank());
        assertFalse(reloaded.moveRank(reloaded.getRank("manager"), true));
        assertFalse(reloaded.moveRank(reloaded.getRank("new"), false));
    }

    @Test
    void comparesCurrentClanMembersAndDestinationRanks() {
        Clan clan = new Clan();
        clan.createRank("leader");
        clan.createRank("recruiter");
        ClanPlayer leader = member(clan, null);
        leader.setLeader(true);
        ClanPlayer top = member(clan, "leader");
        ClanPlayer lower = member(clan, "recruiter");
        ClanPlayer peer = member(clan, "recruiter");
        ClanPlayer unranked = member(clan, null);
        ClanPlayer missingRank = member(clan, "deleted");
        ClanPlayer outsider = member(new Clan(), null);

        assertTrue(clan.isAbove(leader, top));
        assertTrue(clan.isAbove(top, lower));
        assertTrue(clan.isAbove(lower, unranked));
        assertTrue(clan.isAbove(lower, missingRank));
        assertFalse(clan.isAbove(top, leader));
        assertFalse(clan.isAbove(lower, top));
        assertFalse(clan.isAbove(lower, peer));
        assertFalse(clan.isAbove(top, top));
        assertFalse(clan.isAbove(unranked, missingRank));
        assertFalse(clan.isAbove(leader, outsider));
        assertTrue(clan.isAbove(top, clan.getRank("recruiter")));
        assertFalse(clan.isAbove(lower, clan.getRank("recruiter")));
        assertFalse(clan.isAbove(lower, clan.getRank("leader")));
        assertFalse(clan.isAbove(leader, new Rank("recruiter")));

        assertTrue(clan.moveRank(clan.getRank("recruiter"), true));
        assertFalse(clan.isAbove(top, lower));
        assertTrue(clan.isAbove(lower, top));
        assertFalse(top.isLeader());
    }

    private ClanPlayer member(Clan clan, String rank) {
        ClanPlayer player = new ClanPlayer();
        player.setUniqueId(UUID.randomUUID());
        player.setName(player.getUniqueId().toString());
        player.setClan(clan);
        player.setRank(rank);
        clan.importMember(player);
        return player;
    }
}
