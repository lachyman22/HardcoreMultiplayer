package Kassis.hardcoreMultiplayer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;


import java.io.File;
import java.io.IOException;

public class HardcoreMultiplayer extends JavaPlugin {

    private static HardcoreMultiplayer instance;

    public static HardcoreMultiplayer getInstance() {
        return instance;
    }

    public static FileConfiguration playerWorlds;
    private File playerWorldsFile;


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        WebhookManager.init(getConfig());
        HardcoreState.loadConfig(getConfig());

        playerWorldsFile = new File(getDataFolder(), "playerWorlds.yml");
        if (!playerWorldsFile.exists()) {
            try {
                playerWorldsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerWorlds = YamlConfiguration.loadConfiguration(playerWorldsFile);


        // Send server online notification
        String version = Bukkit.getBukkitVersion().split("-")[0];
        String ip = "kassis.connectminecraft.com";
        String message = String.format(
                ":green_circle: **Server Online**\n" +
                        "IP: `%s`\n" +
                        "Version: %s\n" +
                        "Current Attempt: #%d",
                ip,
                version,
                HardcoreState.resetCount
        );

        HardcoreCommandHandler handler = new HardcoreCommandHandler();
        getCommand("hc").setExecutor(handler);
        getCommand("hc").setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new HardcoreEventListener(), this);
        getServer().getPluginManager().registerEvents(new MOTDListener(), this);

        WebhookManager.sendWebhook(message);


        // Load all three dimensions
        String currentWorldName = "hardcore_" + HardcoreState.resetCount;
        World overworld = Bukkit.getWorld(currentWorldName);
        World nether = Bukkit.getWorld(currentWorldName + "_nether");
        World end = Bukkit.getWorld(currentWorldName + "_the_end");

        // Create worlds if they don't exist
        if (overworld == null) {
            overworld = new WorldCreator(currentWorldName).createWorld();
        }
        if (nether == null) {
            nether = new WorldCreator(currentWorldName + "_nether").environment(World.Environment.NETHER).createWorld();
        }
        if (end == null) {
            end = new WorldCreator(currentWorldName + "_the_end").environment(World.Environment.THE_END).createWorld();
        }

        // Teleport all players to correct dimension's spawn
        for (Player player : Bukkit.getOnlinePlayers()) {
            World playerWorld = player.getWorld();
            Location spawnLoc;

            if (playerWorld.getName().endsWith("_nether")) {
                spawnLoc = nether.getSpawnLocation();
            } else if (playerWorld.getName().endsWith("_the_end")) {
                spawnLoc = end.getSpawnLocation();
            } else {
                spawnLoc = overworld.getSpawnLocation();
            }

            // Only teleport if they're in the wrong attempt
            if (!playerWorld.getName().startsWith("hardcore_" + HardcoreState.resetCount)) {
                player.teleport(spawnLoc);
            }

            player.setGameMode(HardcoreState.worldEnded ? GameMode.SPECTATOR : GameMode.SURVIVAL);
        }


        Bukkit.getScheduler().runTaskTimer(this, ScoreboardManager::updateAll, 0L, 10L);
    }

    @Override
    public void onDisable() {
        HardcoreState.pauseTimer();
        HardcoreState.saveTimer();
        WebhookManager.sendWebhook(":red_circle: **Server Offline**");
    }

    public static void savePlayerWorlds() {
        try {
            playerWorlds.save(new File(getInstance().getDataFolder(), "playerWorlds.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
