package Kassis.hardcoreMultiplayer;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookManager {
    private static String webhookUrl;

    public static void init(FileConfiguration config) {
        webhookUrl = config.getString("discord-webhook-url", "");

    }

    public static void sendWebhook(String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("[WebhookManager] Webhook URL is empty, aborting.");
            return;
        }

        if (!HardcoreMultiplayer.getInstance().isEnabled()) {
            System.out.println("[WebhookManager] Plugin is disabled, skipping webhook.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(HardcoreMultiplayer.getInstance(), () -> {
            try {
                System.out.println("[WebhookManager] Sending webhook: " + content);

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "HardcoreMultiplayer");
                connection.setDoOutput(true);

                String jsonPayload = "{ \"embeds\": [ { " +
                        "\"title\": \"HARDCORE\", " +
                        "\"description\": \"" + content
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n") +
                        "\", " +
                        "\"color\": 16711680" +
                        "} ] }";

                System.out.println("[WebhookManager] JSON Payload: " + jsonPayload);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                System.out.println("[WebhookManager] Response Code: " + responseCode);
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}