package Kassis.hardcoreMultiplayer;

import org.bukkit.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Scoreboard;

public class WorldResetManager {

    public static void resetWorld() throws IOException {
        HardcoreState.isResetting = true;
        HardcoreState.resetCount++;
        HardcoreMultiplayer plugin = HardcoreMultiplayer.getInstance();

        plugin.getConfig().set("resetCount", HardcoreState.resetCount);
        plugin.saveConfig();

        String base = "hardcore_" + HardcoreState.resetCount;

        World overworld = new WorldCreator(base).environment(World.Environment.NORMAL).createWorld();
        overworld.setHardcore(true);

        World nether = new WorldCreator(base + "_nether").environment(World.Environment.NETHER).createWorld();
        nether.setHardcore(true);

        World end = new WorldCreator(base + "_the_end").environment(World.Environment.THE_END).createWorld();
        end.setHardcore(true);


        clearPlayerData(overworld);
        clearPlayerData(nether);
        clearPlayerData(end);

        Bukkit.getScheduler().runTaskLater(HardcoreMultiplayer.getInstance(), () -> {
            World newOverworld = Bukkit.getWorld("hardcore_" + HardcoreState.resetCount);
            World newNether = Bukkit.getWorld("hardcore_" + HardcoreState.resetCount + "_nether");
            World newEnd = Bukkit.getWorld("hardcore_" + HardcoreState.resetCount + "_the_end");

            for (World world : List.of(newOverworld, newNether, newEnd)) {
                if (world == null) continue;
                world.setGameRule(GameRule.NATURAL_REGENERATION, true);
                world.setGameRule(GameRule.KEEP_INVENTORY, false);
                world.setGameRule(GameRule.DO_INSOMNIA, false);
                world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 1);
                world.setDifficulty(Difficulty.HARD);
            }

            // Only create Health objective if it doesn't exist
            Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
            if (mainBoard.getObjective("Health") == null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "scoreboard objectives add Health health {\"text\":\"❤\",\"color\":\"red\"}");
            }

            // Always re-set the display slots just in case
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "scoreboard objectives setdisplay below_name Health");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "scoreboard objectives setdisplay list Health");

        }, 60L);



        Bukkit.getWorlds().remove(overworld);
        Bukkit.getWorlds().add(0, overworld);

        clearPlayerData(overworld);
        clearPlayerData(nether);
        clearPlayerData(end);

        if (HardcoreState.resetCount > 1) {
            String old = "hardcore_" + (HardcoreState.resetCount - 1);
            archiveAndDelete(old);
            archiveAndDelete(old + "_nether");
            archiveAndDelete(old + "_the_end");
        }

        updateBukkitYML(base);

        HardcoreState.resetTimer();       // NEW
        HardcoreState.saveTimer();        // NEW

        String discordMessage = String.format(":white_check_mark: **New world created!**\n" +
                        "Attempt #%d has begun\n" +
                        "World Name: `%s`",
                HardcoreState.resetCount,
                base);

        WebhookManager.sendWebhook(discordMessage);

        HardcoreState.isResetting = false;
        HardcoreState.worldEnded = false;
        HardcoreState.hardcoreEnabled = false;
    }

    private static void archiveAndDelete(String worldName) throws IOException {
        File src = new File(Bukkit.getWorldContainer(), worldName);
        if (!src.exists()) return;

        File archiveDir = new File(Bukkit.getWorldContainer(), "archive");
        archiveDir.mkdirs();
        File dest = new File(archiveDir, worldName + "_" + System.currentTimeMillis());
        copyFolder(src, dest);

        Bukkit.unloadWorld(worldName, false);
        deleteFolder(src);

        cleanOldArchives(2); // KEEP ONLY LAST 2
    }


    private static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            dest.mkdirs();
            for (File f : src.listFiles()) {
                copyFolder(f, new File(dest, f.getName()));
            }
        } else {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteFolder(File folder) {
        if (!folder.exists()) return;
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) deleteFolder(f);
            else f.delete();
        }
        folder.delete();
    }

    private static void updateBukkitYML(String newWorldName) {
        File bukkitFile = new File(Bukkit.getWorldContainer().getParentFile(), "bukkit.yml");
        if (!bukkitFile.exists()) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(bukkitFile);
            config.set("settings.world-name", newWorldName);
            config.save(bukkitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void clearPlayerData(World world) {
        File playerDataFolder = new File(world.getWorldFolder(), "playerdata");
        if (!playerDataFolder.exists()) return;

        for (File file : playerDataFolder.listFiles()) {
            if (file.getName().endsWith(".dat")) {
                file.delete();
            }
        }
    }

    private static void cleanOldArchives(int keepCount) {
        File archiveDir = new File(Bukkit.getWorldContainer(), "archive");
        File[] archives = archiveDir.listFiles();
        if (archives == null || archives.length <= keepCount) return;

        Arrays.sort(archives, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < archives.length - keepCount; i++) {
            deleteFolder(archives[i]);
        }
    }

}
