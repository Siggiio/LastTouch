package io.siggi.lasttouch;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class LTICommand implements CommandExecutor, TabExecutor {

    private final LastTouch plugin;

    LTICommand(LastTouch plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length > 0 && args[0].equals("purge")) {
            for (World world : Bukkit.getWorlds()) {
                LTWorld ltWorld = plugin.getWorld(world);
                ltWorld.purge();
            }
            plugin.sendMessage(player, "Purged all data to disk");
            return true;
        }
        if (plugin.isInspector(player)) {
            plugin.setInspector(player, false);
            plugin.sendMessage(player, "Inspector mode disabled");
        } else {
            plugin.setInspector(player, true);
            plugin.sendMessage(player, "Inspector mode enabled");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
}
