package Kassis.hardcoreMultiplayer;

import org.bukkit.configuration.file.FileConfiguration;

public class HardcoreState {
    public static boolean hardcoreEnabled = false;
    public static boolean worldEnded = false;
    public static int resetCount = 1;
    public static boolean isResetting = false;

    // Timer system
    public static long totalElapsed = 0;
    public static long activeStartTime = 0;
    public static boolean isTimerRunning = false;
    public static boolean damageLoggingEnabled = false;



    public static void enable() {
        hardcoreEnabled = true;
        worldEnded = false;
        startTimer();
        HardcoreMultiplayer plugin = HardcoreMultiplayer.getInstance();
        plugin.getConfig().set("resetCount", resetCount);
        plugin.saveConfig();
    }

    public static void disable() {
        hardcoreEnabled = false;
        pauseTimer();
        saveTimer();
    }

    public static void resetTimer() {
        totalElapsed = 0;
        activeStartTime = 0;
        isTimerRunning = false;
    }

    public static void startTimer() {
        if (!isTimerRunning) {
            activeStartTime = System.currentTimeMillis();
            isTimerRunning = true;
        }
    }

    public static void pauseTimer() {
        if (isTimerRunning) {
            totalElapsed += System.currentTimeMillis() - activeStartTime;
            isTimerRunning = false;
        }
    }

    public static long getElapsedMillis() {
        if (isTimerRunning) {
            return totalElapsed + (System.currentTimeMillis() - activeStartTime);
        } else {
            return totalElapsed;
        }
    }

    public static void loadConfig(FileConfiguration config) {
        resetCount = config.getInt("resetCount", 1);
        totalElapsed = config.getLong("totalElapsed", 0);
        isTimerRunning = false;
        activeStartTime = 0;
    }

    public static void saveTimer() {
        HardcoreMultiplayer plugin = HardcoreMultiplayer.getInstance();
        plugin.getConfig().set("totalElapsed", totalElapsed);
        plugin.saveConfig();
    }
}
