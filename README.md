# Hardcore Multiplayer

A custom Minecraft plugin that transforms standard survival into a brutal roguelike: when one player dies, everyone dies. Built for co-op chaos, this plugin automatically resets the world, tracks attempts, and supports Discord webhooks for external death notifications. Ideal for streamers, friends, or masochists.

## 🎮 Core Features

- **Shared Lives:** One death = everyone dies = world reset.
- **Automatic World Reset:** Deletes and regenerates overworld, Nether, and End with a new seed.
- **Attempt Tracking:** Keeps a persistent counter across resets using scoreboard + MOTD.
- **Time Tracking:** Keeps track of how long each run lasts.
- **MOTD Sync:** Server list MOTD displays current run number.
- **Damage Logging:** Toggleable debug mode to trace shared damage events.
- **Join Lockout:** Prevents players from joining mid-run.
- **Admin Utilities:** Includes teleport fixes, spawn recovery, and more.
- **Discord Webhook Integration:** Optional – send run summaries or death alerts to your server.

## 🛠 Commands

All commands are run via `/hc` and require operator permissions.

| Command | Description |
|--------|-------------|
| `/hc enable` | Starts a new hardcore session. Sets all players to survival. |
| `/hc disable` | Ends the hardcore session without resetting. |
| `/hc reset` | Manually resets the world if a death has occurred. |
| `/hc attempts` | Shows the current attempt number. |
| `/hc attempts clear` | Resets the attempt counter to 1. |
| `/hc time` | Shows the current run duration. |
| `/hc time reset` | Resets the run timer to zero. |
| `/hc time set <hours> <minutes>` | Manually set the timer. |
| `/hc world` | Displays your current world and attempt number. *(player only)* |
| `/hc fix [player]` | Teleports a player to spawn and sets survival mode. |
| `/hc damagelogging enable` | Enables debug-mode logging for shared damage. |
| `/hc damagelogging disable` | Disables damage logging. |
| `/hc locate` | Groups players based on proximity and prints their locations. *(player only)* |
| `/hc locate <player>` | Displays a specific player's coordinates. *(player only)* |

> ❗ `fix`, `world`, and `locate` commands require a player — not usable from console.

```
HardcoreMultiplayer/
├── src/
│   └── main/
│       ├── java/Kassis/hardcoreMultiplayer/
│       │   ├── HardcoreMultiplayer.java
│       │   ├── HardcoreCommandHandler.java
│       │   ├── HardcoreEventListener.java
│       │   ├── HardcoreState.java
│       │   ├── MOTDListener.java
│       │   ├── ScoreboardManager.java
│       │   ├── WebhookManager.java
│       │   └── WorldResetManager.java
│       └── resources/
│           ├── plugin.yml
│           └── config.yml
├── pom.xml
├── .gitignore
├── README.md
```

## 🌐 Webhook Support

If `webhook-url` is defined in `config.yml`, the plugin will send Discord messages on:
- Hardcore session enabled/disabled
- Death-triggered resets

Format: markdown-style messages using basic emoji + text, easily customisable.

## ✅ Tested On

- **Minecraft:** 1.20.4 (compatible with most 1.20+ versions)
- **Servers:** Paper & Spigot
- **Java:** 17+

