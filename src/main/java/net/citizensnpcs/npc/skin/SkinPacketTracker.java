package net.citizensnpcs.npc.skin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;

import net.citizensnpcs.Settings;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.util.NMS;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles and synchronizes add and remove packets for Player type NPC's
 * in order to properly apply the NPC skin.
 *
 * <p>Used as one instance per NPC entity.</p>
 */
public class SkinPacketTracker {

    private final SkinnableEntity entity;
    private final Map<UUID, PlayerEntry> inProgress =
            new HashMap<UUID, PlayerEntry>(Bukkit.getMaxPlayers() / 2);

    private Skin skin;
    private boolean isRemoved;

    /**
     * Constructor.
     *
     * @param entity  The human NPC entity the instance belongs to.
     */
    public SkinPacketTracker(SkinnableEntity entity) {
        Preconditions.checkNotNull(entity);

        this.entity = entity;
        this.skin = Skin.get(entity);

        if (LISTENER == null) {
            LISTENER = new PlayerListener();
            Bukkit.getPluginManager().registerEvents(LISTENER, CitizensAPI.getPlugin());
        }
    }

    /**
     * Get the NPC skin.
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Notify that the NPC skin has been changed.
     */
    public void notifySkinChange() {
        this.skin = Skin.get(entity);
        skin.applyAndRespawn(entity);
    }

    /**
     * Notify the tracker that a remove packet has been sent to the
     * specified player.
     *
     * @param playerId  The ID of the player.
     */
    void notifyRemovePacketSent(UUID playerId) {

        PlayerEntry entry = inProgress.get(playerId);
        if (entry == null)
            return;

        if (entry.removeCount == 0)
            return;

        entry.removeCount -= 1;
        if (entry.removeCount == 0) {
            inProgress.remove(playerId);
        }
        else {
            scheduleRemovePacket(entry);
        }
    }

    /**
     * Notify the tracker that a remove packet has been sent to the
     * specified player.
     *
     * @param playerId  The ID of the player.
     */
    void notifyRemovePacketCancelled(UUID playerId) {
        inProgress.remove(playerId);
    }

    /**
     * Send skin related packets to a player.
     *
     * @param player  The player.
     */
    public void updateViewer(final Player player) {
        Preconditions.checkNotNull(player);

        if (player.hasMetadata("NPC"))
            return;

        if (isRemoved || inProgress.containsKey(player.getUniqueId()))
            return;

        PlayerEntry entry = new PlayerEntry(player);
        inProgress.put(player.getUniqueId(), entry);

        PLAYER_LIST_REMOVER.cancelPackets(player, entity);

        skin.apply(entity);

        NMS.sendPlayerListAdd(player, entity.getBukkitEntity());

        scheduleRemovePacket(entry, 2);
    }

    /**
     * Send skin related packets to all nearby players within the specified block radius.
     *
     * @param radius  The radius.
     */
    public void updateNearbyViewers(double radius) {

        radius *= radius;

        org.bukkit.World world = entity.getBukkitEntity().getWorld();
        Player from = entity.getBukkitEntity();
        Location location = from.getLocation();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {

            if (player == null || player.hasMetadata("NPC"))
                continue;

            if (world != player.getWorld() || !player.canSee(from))
                continue;

            if (location.distanceSquared(player.getLocation(CACHE_LOCATION)) > radius)
                continue;

            updateViewer(player);
        }
    }

    /**
     * Invoke when the NPC entity is removed.
     *
     * <p>Sends remove packets to all players.</p>
     */
    public void onRemoveNPC() {

        isRemoved = true;

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        for (Player player : players) {

            if (player.hasMetadata("NPC"))
                continue;

            // send packet now and later to ensure removal from player list
            NMS.sendPlayerListRemove(player, entity.getBukkitEntity());
            PLAYER_LIST_REMOVER.sendPacket(player, entity);
        }
    }

    private void scheduleRemovePacket(PlayerEntry entry, int count) {

        if (!shouldRemoveFromPlayerList())
            return;

        entry.removeCount = count;
        scheduleRemovePacket(entry);
    }

    private void scheduleRemovePacket(final PlayerEntry entry) {

        if (isRemoved)
            return;

        Bukkit.getScheduler().runTaskLater(CitizensAPI.getPlugin(),
                new Runnable() {

                    @Override
                    public void run() {

                        if (shouldRemoveFromPlayerList()) {
                            PLAYER_LIST_REMOVER.sendPacket(entry.player, entity);
                        }
                    }
                }, PACKET_DELAY_REMOVE);
    }

    private boolean shouldRemoveFromPlayerList() {

        boolean isTablistDisabled = Settings.Setting.DISABLE_TABLIST.asBoolean();
        boolean isNpcRemoved = entity.getNPC().data().get("removefromplayerlist",
                Settings.Setting.REMOVE_PLAYERS_FROM_PLAYER_LIST.asBoolean());

        return isNpcRemoved && isTablistDisabled;
    }

    private class PlayerEntry {
        Player player;
        int removeCount;
        PlayerEntry (Player player) {
            this.player = player;
        }
    }

    private static class PlayerListener implements Listener {

        @EventHandler
        private void onPlayerQuit(PlayerQuitEvent event) {

            // this also causes any entries in the "inProgress" field to
            // be removed.
            PLAYER_LIST_REMOVER.cancelPackets(event.getPlayer());
        }
    }

    private static final Location CACHE_LOCATION = new Location(null, 0, 0, 0);
    private static final int PACKET_DELAY_REMOVE = 1;
    private static final PlayerListRemover PLAYER_LIST_REMOVER = new PlayerListRemover();
    private static PlayerListener LISTENER;
}
