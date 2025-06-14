package Kassis.hardcoreMultiplayer;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MOTDListener implements Listener {

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        long durationMillis = HardcoreState.getElapsedMillis();
        long totalMinutes = durationMillis / (1000 * 60);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String motd = ChatColor.GOLD + "Hardcore Attempt #" + HardcoreState.resetCount +
                ChatColor.GRAY + " - " + hours + "h " + minutes + "m";
        event.setMotd(motd);
    }
}
