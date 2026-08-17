
package com.ghostgirl;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class GhostGirlPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private final Set<Entity> ghosts = new HashSet<>();
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("ghostgirl")).setExecutor(this);
        Objects.requireNonNull(getCommand("ghostgirl")).setTabCompleter(this);
        startTask();
    }

    private void startTask() {
        long interval = getConfig().getLong("spawn.interval-minutes", 10) * 1200L;
        task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("spawn.enabled", true)) return;
            for (Player p : Bukkit.getOnlinePlayers()) spawnGhost(p);
        }, interval, interval);
    }

    private void spawnGhost(Player player) {
        Location base = player.getLocation();
        Random r = new Random();
        Location loc = base.clone().add(
                r.nextInt(11)-5, 0, r.nextInt(11)-5
        );
        loc.setY(base.getWorld().getHighestBlockYAt(loc) + 1);

        if (!loc.getChunk().isLoaded()) loc.getChunk().load();

        ArmorStand ghost = loc.getWorld().spawn(loc, ArmorStand.class, a -> {
            a.setInvisible(false);
            a.setCustomName("§7Ghost Girl");
            a.setCustomNameVisible(true);
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setAI(false);
        });

        ghosts.add(ghost);

        if (getConfig().getBoolean("effects.sound-enabled", true))
            player.playSound(loc, Sound.AMBIENT_CAVE, 1, 0.5f);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!ghost.isDead()) ghost.remove();
            ghosts.remove(ghost);
        }, getConfig().getLong("spawn.lifetime-seconds",10)*20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ghostgirl.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(getConfig().getString("messages.reload-success"));
            return true;
        }
        sender.sendMessage("§aGhostGirl running.");
        sender.sendMessage("§7Interval: " + getConfig().getLong("spawn.interval-minutes") + " minutes");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("reload");
        return Collections.emptyList();
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        ghosts.forEach(e -> { if (!e.isDead()) e.remove(); });
        ghosts.clear();
    }
}
