package Kassis.hardcoreMultiplayer;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HardcoreCommandHandler implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /hc <enable | disable | reset | attempts | time>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                if (!HardcoreState.hardcoreEnabled) {
                    HardcoreState.enable();
                    sender.sendMessage("Hardcore mode ENABLED. If one dies, all die.");

                    if (sender instanceof Player) {
                        String name = ((Player) sender).getName();
                        WebhookManager.sendWebhook(":arrow_forward: **Hardcore ENABLED by " + name + "**\nTimer started");
                    }
                } else {
                    sender.sendMessage("Hardcore mode was already enabled. Re-applying survival mode to all players.");
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
                break;
            case "disable":
                HardcoreState.disable();
                sender.sendMessage("Hardcore mode DISABLED. Normal gameplay resumed.");

                if (sender instanceof Player) {
                    String name = ((Player) sender).getName();
                    WebhookManager.sendWebhook(":stop_button: **Hardcore DISABLED by " + name + "**\nTimer stopped");
                }
                break;

            case "reset":
                if (!HardcoreState.worldEnded) {
                    sender.sendMessage("World hasn't ended yet.");
                    break;
                }
                HardcoreState.isResetting = true;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.kickPlayer("§6World is resetting...");
                }
                Bukkit.getScheduler().runTaskLater(HardcoreMultiplayer.getInstance(), () -> {
                    try {
                        WorldResetManager.resetWorld();
                    } catch (IOException e) {
                        HardcoreMultiplayer.getInstance().getLogger().severe("World reset failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, 40L);
                break;

            case "time":
                if (args.length == 1) {
                    long millis = HardcoreState.getElapsedMillis();
                    long totalMinutes = millis / (1000 * 60);
                    long hours = totalMinutes / 60;
                    long minutes = totalMinutes % 60;
                    sender.sendMessage("Hardcore time: " + hours + "h " + minutes + "m");
                } else if (args.length == 2 && args[1].equalsIgnoreCase("reset")) {
                    HardcoreState.resetTimer();
                    HardcoreState.saveTimer();
                    sender.sendMessage("Timer has been reset.");
                } else if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                    try {
                        int hours = Integer.parseInt(args[2]);
                        int minutes = Integer.parseInt(args[3]);
                        if (hours < 0 || minutes < 0) {
                            sender.sendMessage(ChatColor.RED + "Time must be non-negative.");
                            return true;
                        }
                        HardcoreState.totalElapsed = (hours * 60L + minutes) * 60_000L;
                        HardcoreState.activeStartTime = 0;
                        HardcoreState.isTimerRunning = false;
                        HardcoreState.saveTimer();
                        sender.sendMessage("Timer set to " + hours + "h " + minutes + "m.");
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Usage: /hc time set <hours> <minutes>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage:");
                    sender.sendMessage(ChatColor.YELLOW + "/hc time" + ChatColor.GRAY + " - show time");
                    sender.sendMessage(ChatColor.YELLOW + "/hc time reset" + ChatColor.GRAY + " - reset time");
                    sender.sendMessage(ChatColor.YELLOW + "/hc time set <hours> <minutes>" + ChatColor.GRAY + " - set time manually");
                }
                break;

            case "attempts":
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    HardcoreState.resetCount = 1;
                    HardcoreMultiplayer.getInstance().getConfig().set("resetCount", 1);
                    HardcoreMultiplayer.getInstance().saveConfig();
                    sender.sendMessage("Attempt counter has been reset.");
                } else {
                    sender.sendMessage("This is attempt #" + HardcoreState.resetCount);
                }
                break;

            case "world":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Console can't use this command.");
                    break;
                }

                Player worldPlayer = (Player) sender;
                World w = worldPlayer.getWorld();
                sender.sendMessage(ChatColor.YELLOW + "You're in world: " + ChatColor.WHITE + w.getName());
                sender.sendMessage(ChatColor.YELLOW + "Current attempt: #" + HardcoreState.resetCount);
                break;

            case "fix":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Console can't use this command.");
                    break;
                }

                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                        break;
                    }
                } else {
                    target = (Player) sender;
                }

                String baseName = "hardcore_" + HardcoreState.resetCount;
                World overworld = Bukkit.getWorld(baseName);

                if (overworld == null) {
                    sender.sendMessage(ChatColor.RED + "Current overworld isn't loaded.");
                    break;
                }

                target.teleport(overworld.getSpawnLocation());
                target.setGameMode(GameMode.SURVIVAL);
                sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to overworld spawn.");
                break;


            case "damagelogging":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /hc damagelogging <enable|disable>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("enable")) {
                    HardcoreState.damageLoggingEnabled = true;
                    sender.sendMessage(ChatColor.GREEN + "Damage logging ENABLED.");
                } else if (args[1].equalsIgnoreCase("disable")) {
                    HardcoreState.damageLoggingEnabled = false;
                    sender.sendMessage(ChatColor.RED + "Damage logging DISABLED.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /hc damagelogging <enable|disable>");
                }
                break;

            case "locate":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console cannot use this command.");
                    break;
                }

                if (args.length == 1) {
                    // Group players by proximity
                    List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    Set<Player> processed = new HashSet<>();

                    sender.sendMessage(ChatColor.YELLOW + "Player locations:");

                    for (Player p : allPlayers) {
                        if (processed.contains(p)) continue;

                        List<Player> group = new ArrayList<>();
                        group.add(p);

                        for (Player other : allPlayers) {
                            if (p == other || processed.contains(other)) continue;
                            if (p.getWorld().equals(other.getWorld()) && p.getLocation().distance(other.getLocation()) <= 20) {
                                group.add(other);
                            }
                        }

                        processed.addAll(group);

                        if (group.size() == 1) {
                            Player solo = group.get(0);
                            Location loc = solo.getLocation();
                            sender.sendMessage(ChatColor.GRAY + "- " + solo.getName() + " @ " +
                                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                        } else {
                            StringBuilder names = new StringBuilder();
                            for (Player grouped : group) {
                                names.append(grouped.getName()).append(", ");
                            }
                            names.setLength(names.length() - 2); // remove trailing comma

                            Location center = group.get(0).getLocation();
                            sender.sendMessage(ChatColor.GRAY + "- [" + names + "] nearby @ " +
                                    center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());
                        }
                    }
                } else if (args.length == 2) {
                    Player locatedPlayer = Bukkit.getPlayer(args[1]);
                    if (locatedPlayer == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found or not online.");
                        break;
                    }

                    Location loc = locatedPlayer.getLocation();
                    sender.sendMessage(ChatColor.YELLOW + locatedPlayer.getName() + "'s location: " +
                            ChatColor.GRAY + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /hc locate [player]");
                }
                break;


            default:
                sender.sendMessage("Usage: /hc <enable | disable | reset | attempts | time | world | fix>");
                break;
        }
        return true;
    }

    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("enable", "disable", "reset", "attempts", "time", "world", "fix", "damagelogging");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fix")) {
            java.util.List<String> playerNames = new java.util.ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return playerNames;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("damagelogging")) {
            return java.util.Arrays.asList("enable", "disable");
        }
        return java.util.Collections.emptyList();
    }
}
