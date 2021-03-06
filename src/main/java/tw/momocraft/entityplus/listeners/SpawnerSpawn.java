package tw.momocraft.entityplus.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import tw.momocraft.entityplus.handlers.ConfigHandler;
import tw.momocraft.entityplus.handlers.ServerHandler;
import tw.momocraft.entityplus.utils.ConfigPath;
import tw.momocraft.entityplus.utils.CustomCommands;
import tw.momocraft.entityplus.utils.ResidenceUtils;
import tw.momocraft.entityplus.utils.entities.SpawnerMap;

import java.util.*;

public class SpawnerSpawn implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerSpawn(SpawnerSpawnEvent e) {
        if (!ConfigHandler.getConfigPath().isSpawner()) {
            return;
        }
        CreatureSpawner spawner = e.getSpawner();
        String entityType;
        try {
            entityType = spawner.getSpawnedType().name();
        } catch (Exception ex) {
            e.setCancelled(true);
            return;
        }
        Location loc = e.getLocation();
        String worldName = loc.getWorld().getName();
        // Checking the enable worlds.
        Map<String, SpawnerMap> spawnerProp = ConfigHandler.getConfigPath().getSpawnerProp().get(worldName);
        if (spawnerProp != null) {
            boolean resFlag = ConfigHandler.getConfigPath().isSpawnerResFlag();
            SpawnerMap spawnerMap;
            for (String groupName : spawnerProp.keySet()) {
                spawnerMap = spawnerProp.get(groupName);
                // Checking the allow entities.
                if (spawnerMap.getAllowList().contains(entityType)) {
                    continue;
                }
                // Checking the spawn "location".
                if (!ConfigPath.getLocationUtils().checkLocation(loc, spawnerMap.getLocMaps())) {
                    ServerHandler.sendFeatureMessage("Spawner", entityType, "Location", "continue", groupName,
                            new Throwable().getStackTrace()[0]);
                    continue;
                }
                // Checking the "blocks" nearby the spawn location.
                if (!ConfigPath.getBlocksUtils().checkBlocks(loc, spawnerMap.getBlocksMaps())) {
                    ServerHandler.sendFeatureMessage("Spawner", entityType, "Blocks", "continue", groupName,
                            new Throwable().getStackTrace()[0]);
                    continue;
                }
                // Checking the spawn "Residence-Flag".
                if (!ResidenceUtils.checkResFlag(loc, resFlag, "spawnerbypass")) {
                    ServerHandler.sendFeatureMessage("Spawner", entityType, "Residence-Flag", "continue", groupName,
                            new Throwable().getStackTrace()[0]);
                    continue;
                }
                // Removing the spawner.
                if (spawnerMap.isRemove()) {
                    e.setCancelled(true);
                    spawner.getBlock().setType(Material.AIR);
                    executeCommands(e, entityType, "AIR", spawnerMap.getCommands());
                    ServerHandler.sendFeatureMessage("Spawner", entityType, "Remove", "remove",
                            new Throwable().getStackTrace()[0]);
                    return;
                }
                // Changing the type of spawner.
                Map<String, Long> changeMap = spawnerMap.getChangeMap();
                if (changeMap != null) {
                    long totalChance = 0;
                    long chance;
                    for (String changeType : changeMap.keySet()) {
                        chance = changeMap.get(changeType);
                        totalChance += chance;
                    }
                    double randTotalChange = Math.random() * totalChance;
                    for (String changeType : changeMap.keySet()) {
                        chance = changeMap.get(changeType);
                        if (chance >= randTotalChange) {
                            spawner.setSpawnedType(EntityType.valueOf(changeType));
                            spawner.update();
                            executeCommands(e, entityType, changeType, spawnerMap.getCommands());
                            ServerHandler.sendFeatureMessage("Spawner", entityType, changeType, "change",
                                    new Throwable().getStackTrace()[0]);
                            e.setCancelled(true);
                            return;
                        }
                        randTotalChange -= chance;
                    }
                }
                ServerHandler.sendFeatureMessage("Spawner", entityType, "Final", "return",
                        new Throwable().getStackTrace()[0]);
            }
        }
    }

    /**
     * @param e          SpawnerSpawnEven
     * @param group      the world group.
     * @param changeType the type of spawner which will change.
     */
    private void executeCommands(SpawnerSpawnEvent e, String group, String changeType, List<String> commands) {
        if (!commands.isEmpty()) {
            CreatureSpawner spawner = e.getSpawner();
            Location loc = spawner.getLocation();
            List<Player> nearbyPlayers = getNearbyPlayers(e.getSpawner().getBlock());
            for (String command : commands) {
                command = command.replace("%spawner%", spawner.getSpawnedType().name())
                        .replace("%new_spawner%", changeType)
                        .replace("%world%", loc.getWorld().getName())
                        .replace("%loc%", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                        .replace("%loc_x%", String.valueOf(loc.getBlockX()))
                        .replace("%loc_y%", String.valueOf(loc.getBlockY()))
                        .replace("%loc_z%", String.valueOf(loc.getBlockZ()))
                        .replace("%nearbyplayers%", getNearbyPlayersString(nearbyPlayers));
                if (command.startsWith("nearby-")) {
                    command = command.replace("nearby-", "");
                    for (Player p : nearbyPlayers) {
                        CustomCommands.executeCommands(p, command);
                    }
                    continue;
                }
                CustomCommands.executeCommands(command);
            }
        }
    }

    /**
     * @param block the center block.
     * @return list the players near the block.
     */
    private List<Player> getNearbyPlayers(Block block) {
        List<Player> nearbyPlayers = new ArrayList<>();
        if (block != null) {
            int range = ConfigHandler.getConfigPath().getNearbyPlayerRange();
            Collection<Entity> nearbyEntities = block.getWorld().getNearbyEntities(block.getLocation(), range, range, range);
            for (Entity en : nearbyEntities) {
                if (en instanceof Player) {
                    nearbyPlayers.add(((Player) en).getPlayer());
                }
            }
        }
        return nearbyPlayers;
    }

    /**
     * @return list the players' name near the block.
     */
    private String getNearbyPlayersString(List<Player> players) {
        StringBuilder playersString = new StringBuilder();
        for (Player player : players) {
            playersString.append(", ").append(player.getName());
        }
        return playersString.toString();
    }
}
