package io.siggi.lasttouch.util;

import java.util.concurrent.Executor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class BukkitExecutor implements Executor {
    private final JavaPlugin plugin;

    public BukkitExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(plugin);
    }
}
