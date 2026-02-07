/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.config;

import com.mcsets.setstore.SetStorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration manager for the SetStore plugin.
 * Handles reading and writing plugin settings.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class PluginConfig {

    private final SetStorePlugin plugin;
    private FileConfiguration config;

    /**
     * Creates a new configuration manager.
     *
     * @param plugin The plugin instance
     */
    public PluginConfig(SetStorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Checks if the plugin is properly configured.
     *
     * @return true if the API key is set
     */
    public boolean isConfigured() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here");
    }

    // ==================== API Settings ====================

    public String getApiKey() {
        return config.getString("api-key", "");
    }

    public String getBaseUrl() {
        return config.getString("api.base-url", "https://mcsets.com/api/v1/setstore");
    }

    public int getApiTimeout() {
        return config.getInt("api.timeout", 30);
    }

    // ==================== Server Settings ====================

    public String getServerIp() {
        String ip = config.getString("server.ip", "");
        if (ip == null || ip.isEmpty()) {
            ip = plugin.getServer().getIp();
            if (ip == null || ip.isEmpty()) {
                ip = "localhost";
            }
        }
        return ip;
    }

    public int getServerPort() {
        int port = config.getInt("server.port", 0);
        if (port <= 0) {
            port = plugin.getServer().getPort();
        }
        return port;
    }

    // ==================== WebSocket Settings ====================

    public boolean isWebSocketEnabled() {
        return config.getBoolean("websocket.enabled", true);
    }

    public int getWebSocketReconnectDelay() {
        return config.getInt("websocket.reconnect-delay", 5);
    }

    public int getMaxReconnectAttempts() {
        return config.getInt("websocket.max-reconnect-attempts", 5);
    }

    // ==================== Polling Settings ====================

    public boolean isPollingEnabled() {
        return config.getBoolean("polling.enabled", true);
    }

    public int getPollingInterval() {
        return config.getInt("polling.interval", 60);
    }

    // ==================== Heartbeat Settings ====================

    public int getHeartbeatInterval() {
        return config.getInt("heartbeat.interval", 300);
    }

    // ==================== Delivery Settings ====================

    public int getCommandDelay() {
        return config.getInt("delivery.command-delay", 5);
    }

    public boolean isRequireOnline() {
        return config.getBoolean("delivery.require-online", false);
    }

    public boolean isQueueOffline() {
        return config.getBoolean("delivery.queue-offline", true);
    }

    // ==================== Messages ====================

    public String getMessagePrefix() {
        return config.getString("messages.prefix", "&8[&bSetStore&8] &7");
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "");
        return formatMessage(getMessagePrefix() + message);
    }

    public String getMessage(String key, String... replacements) {
        String message = config.getString("messages." + key, "");
        String fullMessage = getMessagePrefix() + message;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                fullMessage = fullMessage.replace(replacements[i], replacements[i + 1]);
            }
        }

        return formatMessage(fullMessage);
    }

    public String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ==================== Logging Settings ====================

    public String getLogLevel() {
        return config.getString("logging.level", "INFO");
    }

    public boolean isLogRequests() {
        return config.getBoolean("logging.log-requests", false);
    }

    public void setLogRequests(boolean value) {
        config.set("logging.log-requests", value);
        plugin.saveConfig();
    }

    public boolean isLogDeliveries() {
        return config.getBoolean("logging.log-deliveries", true);
    }

    public void setLogDeliveries(boolean value) {
        config.set("logging.log-deliveries", value);
        plugin.saveConfig();
    }

    public boolean isLogCommands() {
        return config.getBoolean("logging.log-commands", true);
    }

    public void setLogCommands(boolean value) {
        config.set("logging.log-commands", value);
        plugin.saveConfig();
    }

    // ==================== Debug Settings ====================

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public void setDebug(boolean value) {
        config.set("debug", value);
        plugin.saveConfig();
    }

    /**
     * Toggles a boolean setting.
     *
     * @param setting The setting name
     * @return The new value
     */
    public boolean toggle(String setting) {
        switch (setting.toLowerCase()) {
            case "debug":
                boolean newDebug = !isDebug();
                setDebug(newDebug);
                return newDebug;

            case "requests":
            case "log-requests":
                boolean newRequests = !isLogRequests();
                setLogRequests(newRequests);
                return newRequests;

            case "deliveries":
            case "log-deliveries":
                boolean newDeliveries = !isLogDeliveries();
                setLogDeliveries(newDeliveries);
                return newDeliveries;

            case "commands":
            case "log-commands":
                boolean newCommands = !isLogCommands();
                setLogCommands(newCommands);
                return newCommands;

            default:
                return false;
        }
    }
}
