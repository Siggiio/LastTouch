package io.siggi.lasttouch;

import io.siggi.cubecore.CubeCore;
import io.siggi.lasttouch.util.BukkitExecutor;
import io.siggi.lasttouch.util.WorkerThread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LastTouch extends JavaPlugin {
    private WorkerThread workerExecutor;
    private BukkitExecutor bukkitExecutor;

    @Override
    public void onEnable() {
        workerExecutor = new WorkerThread();
        bukkitExecutor = new BukkitExecutor(this);
        getCommand("lti").setExecutor(new LTICommand(this));
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        for (World world : getServer().getWorlds()) {
            addWorld(world);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                ltTick();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        List<World> worlds = new ArrayList<>(this.worlds.keySet());
        for (World world : worlds) {
            removeWorld(world);
        }
        inspectors.clear();
        try {
            workerExecutor.closeAndWait();
        } catch (InterruptedException ignored) {
        }
    }

    private void ltTick() {
        long time = System.currentTimeMillis();
        for (LTWorld world : worlds.values()) {
            world.ltTick(time);
        }
    }

    private final Map<World, LTWorld> worlds = new HashMap<>();
    private final Set<Player> inspectors = new HashSet<>();

    void addWorld(World world) {
        LTWorld ltWorld = worlds.get(world);
        if (ltWorld != null) return;
        ltWorld = new LTWorld(this, world);
        worlds.put(world, ltWorld);
    }

    void removeWorld(World world) {
        LTWorld ltWorld = worlds.get(world);
        if (ltWorld == null) return;
        ltWorld.unload();
        worlds.remove(world);
    }

    public LTWorld getWorld(World world) {
        return worlds.get(world);
    }

    public LTChunk getChunk(Chunk chunk) {
        LTWorld world = getWorld(chunk.getWorld());
        if (world == null) return null;
        return world.getChunk(chunk);
    }

    boolean isInspector(Player player) {
        if (!player.hasPermission("lasttouch.inspect")) {
            inspectors.remove(player);
        }
        return inspectors.contains(player);
    }

    void setInspector(Player player, boolean inspector) {
        if (inspector) {
            inspectors.add(player);
        } else {
            inspectors.remove(player);
        }
    }

    void playerQuit(Player player) {
        inspectors.remove(player);
    }

    Executor getWorkerExecutor() {
        return workerExecutor;
    }

    Executor getBukkitExecutor() {
        return bukkitExecutor;
    }

    void sendMessage(Player player, String message) {
        player.sendMessage(ChatColor.GOLD + "[LastTouch] " + ChatColor.RESET + message);
    }

    public String getName(UUID user) {
        String name = CubeCore.getUserCache().getName(user);
        return name == null ? user.toString() : name;
    }
}
