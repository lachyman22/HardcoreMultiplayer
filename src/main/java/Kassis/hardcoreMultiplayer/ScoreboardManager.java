package Kassis.hardcoreMultiplayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    private static final String SIDEBAR_TITLE = ChatColor.GOLD + "Hardcore Status";

    public static void setupPlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // Sidebar only
        Objective sidebar = scoreboard.registerNewObjective("hcSidebar", "dummy", SIDEBAR_TITLE);
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
    }

    public static void updateAll() {
        long durationMillis = HardcoreState.getElapsedMillis();
        long totalMinutes = durationMillis / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();

            Objective sidebar = scoreboard.getObjective("hcSidebar");
            if (sidebar == null) {
                sidebar = scoreboard.registerNewObjective("hcSidebar", "dummy", SIDEBAR_TITLE);
                sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            // Clear sidebar
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }

            // Sidebar lines
            String statusLine = ChatColor.YELLOW + "Status: " + (HardcoreState.hardcoreEnabled
                    ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED");
            String attemptLine = ChatColor.YELLOW + "Attempt: " + ChatColor.WHITE + "#" + HardcoreState.resetCount;
            String timeLine = ChatColor.YELLOW + "Time: " + ChatColor.WHITE + String.format("%dh %dm", hours, minutes);

            sidebar.getScore(statusLine + "   ").setScore(3);
            sidebar.getScore(timeLine + " ").setScore(2);
            sidebar.getScore(attemptLine + "  ").setScore(1);


            int row = 0;
            for (Player target : Bukkit.getOnlinePlayers()) {
                String formattedHearts = String.valueOf((int) target.getHealth());
                String nameLine = ChatColor.WHITE + target.getName() + ": " + ChatColor.RED + formattedHearts + "❤";
                sidebar.getScore(nameLine + "   ").setScore(-1 - row++);
            }
        }
    }
}
