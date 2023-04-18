package io.siggi.lasttouch;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class EventListener implements Listener {
    private final LastTouch plugin;

    public EventListener(LastTouch plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void worldLoaded(WorldLoadEvent event) {
        plugin.addWorld(event.getWorld());
    }

    @EventHandler
    public void worldUnloaded(WorldUnloadEvent event) {
        plugin.removeWorld(event.getWorld());
    }

    @EventHandler
    public void chunkLoaded(ChunkLoadEvent event) {
        plugin.getWorld(event.getWorld()).addChunk(event.getChunk());
    }

    @EventHandler
    public void chunkUnloaded(ChunkUnloadEvent event) {
        plugin.getWorld(event.getWorld()).removeChunk(event.getChunk());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        plugin.playerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInspector(player))
            return;
        Block blockToInspect;
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK:
                blockToInspect = event.getClickedBlock();
                break;
            case RIGHT_CLICK_BLOCK:
                blockToInspect = event.getClickedBlock().getRelative(event.getBlockFace());
                break;
            default:
                return;
        }
        event.setCancelled(true);
        LTChunk chunk = plugin.getChunk(blockToInspect.getChunk());
        if (chunk == null) {
            plugin.sendMessage(player, "The chunk that block is in is not available.");
            return;
        }
        String blockName = (blockToInspect.getType().name().toLowerCase());
        chunk.getData(blockToInspect).thenAccept((data) -> {
            if (data == null) {
                plugin.sendMessage(player, "No information on this " + ChatColor.AQUA + blockName + ChatColor.RESET + " was found.");
                return;
            }
            String time;
            long timeAgo = System.currentTimeMillis() - data.getTime();
            if (timeAgo < 60000L) {
                time = (timeAgo / 1000L) + "s";
            } else if (timeAgo < 3600000L) {
                time = (timeAgo / 60000L) + "m";
            } else if (timeAgo < 86400000L) {
                time = (timeAgo / 3600000L) + "h";
            } else {
                time = (timeAgo / 86400000L) + "d";
            }
            plugin.sendMessage(player,
                ChatColor.AQUA + blockName + ChatColor.RESET
                    + " was last changed " + ChatColor.YELLOW + time + " ago"
                    + ChatColor.RESET + " by "
                    + ChatColor.AQUA + plugin.getName(data.getUser())
                    + ChatColor.RESET + ".");
        }).exceptionally((t) -> {
            t.printStackTrace();
            plugin.sendMessage(player, "There was a problem reading the database.");
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bucketFill(PlayerBucketFillEvent event) {
        change(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bucketEmpty(PlayerBucketEmptyEvent event) {
        change(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreak(BlockBreakEvent event) {
        change(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockPlace(BlockPlaceEvent event) {
        change(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockMultiPlace(BlockMultiPlaceEvent event) {
        for (BlockState state : event.getReplacedBlockStates()) {
            change(state.getBlock(), event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void signChangeEvent(SignChangeEvent event) {
        change(event.getBlock(), event.getPlayer());
    }

    private void change(Block block, Player player) {
        LTChunk chunk = plugin.getChunk(block.getChunk());
        if (chunk != null) {
            chunk.recordChange(block, player.getUniqueId());
        }
    }
}
