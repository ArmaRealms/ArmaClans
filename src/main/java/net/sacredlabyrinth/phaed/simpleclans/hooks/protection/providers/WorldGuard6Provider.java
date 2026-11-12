package net.sacredlabyrinth.phaed.simpleclans.hooks.protection.providers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Coordinate;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Land;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.ProtectionProvider;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class WorldGuard6Provider implements ProtectionProvider {

    private RegionContainer regionContainer;

    @Override
    public void setup() {
        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    private @Nullable RegionManager getRegionManager(@Nullable World world) {
        if (world == null) {
            return null;
        }
        return regionContainer.get(BukkitAdapter.adapt(world));
    }

    @NotNull
    private Land getLand(ProtectedRegion region) {
        List<Coordinate> coordinates = region.getPoints().stream()
                .map(point -> new Coordinate(point.x(), point.z()))
                .toList();
        return new Land(getIdPrefix() + region.getId(), region.getOwners().getUniqueIds(), coordinates);
    }

    @Override
    public @NotNull Set<Land> getLandsAt(@NotNull Location location) {
        HashSet<Land> lands = new HashSet<>();
        RegionManager regionManager = getRegionManager(location.getWorld());

        if (regionManager != null) {
            BlockVector3 position = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            for (ProtectedRegion region : regionManager.getApplicableRegions(position)) {
                lands.add(getLand(region));
            }
        }
        return lands;
    }

    @Override
    public @NotNull Set<Land> getLandsOf(@NotNull OfflinePlayer player, @NotNull World world) {
        HashSet<Land> lands = new HashSet<>();
        RegionManager regionManager = getRegionManager(world);

        if (regionManager != null) {
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (!region.getOwners().getUniqueIds().contains(player.getUniqueId())) {
                    continue;
                }
                lands.add(getLand(region));
            }
        }
        return lands;
    }

    @Override
    public @NotNull String getIdPrefix() {
        return "wg";
    }

    @Override
    public void deleteLand(@NotNull String id, @NotNull World world) {
        id = id.replaceFirst(getIdPrefix(), "");
        RegionManager regionManager = getRegionManager(world);
        if (regionManager != null) {
            regionManager.removeRegion(id);
        }
    }

    @Override
    public @Nullable Class<? extends Event> getCreateLandEvent() {
        return null;
    }

    @Override
    public @Nullable Player getPlayer(Event event) {
        return null;
    }

    @Override
    public @Nullable String getRequiredPluginName() {
        return "WorldGuard";
    }
}
