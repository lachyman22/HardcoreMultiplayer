package Kassis.hardcoreMultiplayer;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.UUID;

public class HardcoreEventListener implements Listener {

    private final HashMap<UUID, Long> lowHealthCooldowns = new HashMap<>();
    private final HashMap<UUID, Long> damageLogCooldowns = new HashMap<>();


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!HardcoreState.hardcoreEnabled || HardcoreState.worldEnded) return;

        HardcoreState.worldEnded = true;
        HardcoreState.pauseTimer();
        HardcoreState.saveTimer();

        long duration = HardcoreState.getElapsedMillis();
        long hours = duration / (1000 * 60 * 60);
        long minutes = (duration / (1000 * 60)) % 60;

        String message = String.format(
                ":skull: **__%s__ has ended the world!**\n" +
                        "Attempt #%d lasted %d hours %d minutes\n" +
                        "**Death: %s**\n",
                event.getEntity().getName(),
                HardcoreState.resetCount,
                hours,
                minutes,
                event.getDeathMessage().replace("\"", "'")
        );

        WebhookManager.sendWebhook(message);

        Bukkit.broadcastMessage("§c§lWORLD HAS ENDED - " + event.getDeathMessage());
        Bukkit.broadcastMessage("§7Attempt #" + HardcoreState.resetCount + " lasted " + hours + "h " + minutes + "m.");
        Bukkit.broadcastMessage("§eUse §l/hc reset§r§e to begin attempt #" + (HardcoreState.resetCount + 1));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(event.getEntity())) {
                player.setHealth(0);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String overworldName = "hardcore_" + HardcoreState.resetCount;
        World overworld = Bukkit.getWorld(overworldName);

        if (overworld == null) {
            overworld = new WorldCreator(overworldName).createWorld();
        }

        event.setRespawnLocation(overworld.getSpawnLocation());
        player.setGameMode(HardcoreState.worldEnded && HardcoreState.hardcoreEnabled ? GameMode.SPECTATOR : GameMode.SURVIVAL);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String baseWorld = "hardcore_" + HardcoreState.resetCount;
        String currentWorld = player.getWorld().getName();
        String lastKnownWorld = HardcoreMultiplayer.playerWorlds.getString(uuid);
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        player.setScoreboard(main);

        // Check if player has moved between attempts
        boolean worldMismatch = lastKnownWorld == null || !lastKnownWorld.startsWith(baseWorld);

        if (worldMismatch) {
            // Player is on a different world than before — force reset
            player.getInventory().clear();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke " + player.getName() + " everything");

            // Teleport to correct world
            World overworld = Bukkit.getWorld(baseWorld);
            if (overworld != null) {
                player.teleport(overworld.getSpawnLocation());
            }

            // Set proper gamemode
            player.setGameMode(HardcoreState.worldEnded ? GameMode.SPECTATOR : GameMode.SURVIVAL);

            // Update tracked world
            HardcoreMultiplayer.playerWorlds.set(uuid, baseWorld);
            HardcoreMultiplayer.savePlayerWorlds();
        }

        if (HardcoreState.hardcoreEnabled && !HardcoreState.worldEnded && !HardcoreState.isTimerRunning) {
            HardcoreState.startTimer();
            WebhookManager.sendWebhook(":arrow_forward: **Server no longer empty - timer resumed**");
        }


        // Set scoreboard and welcome message
        ScoreboardManager.setupPlayer(player);

        player.sendMessage(ChatColor.GOLD + "Welcome " + player.getName() + "! HC is " +
                (HardcoreState.hardcoreEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED") +
                ChatColor.GOLD + ". You're on attempt #" + HardcoreState.resetCount);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(HardcoreMultiplayer.getInstance(), () -> {
            if (Bukkit.getOnlinePlayers().isEmpty() && HardcoreState.hardcoreEnabled && !HardcoreState.worldEnded) {
                HardcoreState.pauseTimer();
                HardcoreState.saveTimer();
                WebhookManager.sendWebhook(":pause_button: **Server empty - timer paused**");
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Location from = event.getFrom();
        World fromWorld = from.getWorld();
        String baseName = "hardcore_" + HardcoreState.resetCount;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                World nether = Bukkit.getWorld(baseName + "_nether");
                if (nether == null) return;
                Location target = new Location(nether, from.getX() / 8, from.getY(), from.getZ() / 8);
                event.setTo(target);
            } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                World overworld = Bukkit.getWorld(baseName);
                if (overworld == null) return;
                Location target = new Location(overworld, from.getX() * 8, from.getY(), from.getZ() * 8);
                event.setTo(target);
            }
        } else if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                World end = Bukkit.getWorld(baseName + "_the_end");
                if (end != null) {
                    event.setTo(end.getSpawnLocation());
                }
            } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
                World overworld = Bukkit.getWorld(baseName);
                if (overworld != null) {
                    event.setTo(overworld.getSpawnLocation());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (HardcoreState.isResetting) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cServer is resetting. Try again in a moment.");
        }
    }

    @EventHandler
    public void onPlayerLowHealth(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0 || finalHealth >= 5.0) return;

        long now = System.currentTimeMillis();
        long last = lowHealthCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - last >= 30_000) {
            Bukkit.broadcastMessage("§c" + player.getName() + " is on critical health! (" + (int) finalHealth + " HP)");
            lowHealthCooldowns.put(player.getUniqueId(), now);
        }


    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!HardcoreState.damageLoggingEnabled) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        double damage = event.getFinalDamage();
        EntityDamageEvent.DamageCause cause = event.getCause();

        // Skip CUSTOM events (internal damage)
        if (cause == EntityDamageEvent.DamageCause.CUSTOM) return;

        long now = System.currentTimeMillis();
        long last = damageLogCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 500) {
            return; // less than 0.5s since last message for this player
        }
        damageLogCooldowns.put(player.getUniqueId(), now);

        String cleanCause;

        switch (cause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (event instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                    cleanCause = damager.getType().toString().toLowerCase().replace("_", " ");
                } else {
                    cleanCause = "entity attack";
                }
                break;
            case PROJECTILE:
                if (event instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                    cleanCause = damager.getType().toString().toLowerCase().replace("_", " ");
                } else {
                    cleanCause = "projectile";
                }
                break;
            case FALL: cleanCause = "fall"; break;
            case FIRE: cleanCause = "fire"; break;
            case FIRE_TICK: cleanCause = "burning"; break;
            case LAVA: cleanCause = "lava"; break;
            case VOID: cleanCause = "void"; break;
            case DROWNING: cleanCause = "drowning"; break;
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION: cleanCause = "explosion"; break;
            case MAGIC: cleanCause = "magic"; break;
            case SUFFOCATION: cleanCause = "suffocation"; break;
            case CONTACT: cleanCause = "block contact"; break;
            default:
                cleanCause = cause.toString().toLowerCase().replace("_", " ");
                break;
        }

        Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " took " + ChatColor.RED +
                (int) damage + " damage" + ChatColor.GRAY + " from " + ChatColor.YELLOW + cleanCause);
    }


}
