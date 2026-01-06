/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore;

import com.mcsets.setstore.api.SetStoreAPI;
import com.mcsets.setstore.commands.SetStoreCommand;
import com.mcsets.setstore.commands.VerifyCommand;
import com.mcsets.setstore.config.PluginConfig;
import com.mcsets.setstore.delivery.DeliveryExecutor;
import com.mcsets.setstore.listeners.PlayerListener;
import com.mcsets.setstore.models.ConnectResponse;
import com.mcsets.setstore.models.HeartbeatResponse;
import com.mcsets.setstore.models.WebSocketConfig;
import com.mcsets.setstore.websocket.SetStoreWebSocket;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Main plugin class for MCSets SetStore integration.
 * Handles automatic delivery of store purchases to Minecraft servers.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class SetStorePlugin extends JavaPlugin {

    private static SetStorePlugin instance;

    private PluginConfig pluginConfig;
    private SetStoreAPI api;
    private SetStoreWebSocket webSocket;
    private DeliveryExecutor deliveryExecutor;

    private BukkitTask heartbeatTask;
    private BukkitTask pollingTask;

    private boolean connected;
    private int serverId = -1;
    private String serverName = "";

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        pluginConfig = new PluginConfig(this);

        if (!pluginConfig.isConfigured()) {
            logError("API key not configured! Please set your API key in config.yml");
            logError("Get your API key from: https://mcsets.com/dashboard/servers");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api = new SetStoreAPI(this);
        deliveryExecutor = new DeliveryExecutor(this);

        registerCommands();
        registerListeners();

        Bukkit.getScheduler().runTaskAsynchronously(this, this::connect);

        logInfo("MCSets SetStore plugin enabled!");
    }

    @Override
    public void onDisable() {
        cancelTasks();

        if (webSocket != null) {
            webSocket.disconnect();
        }

        if (api != null) {
            api.shutdown();
        }

        logInfo("MCSets SetStore plugin disabled!");
        instance = null;
    }

    /**
     * Registers all plugin commands.
     */
    private void registerCommands() {
        PluginCommand verifyCmd = getCommand("verify");
        PluginCommand setstoreCmd = getCommand("setstore");

        if (verifyCmd != null) {
            verifyCmd.setExecutor(new VerifyCommand(this));
        }

        if (setstoreCmd != null) {
            SetStoreCommand cmd = new SetStoreCommand(this);
            setstoreCmd.setExecutor(cmd);
            setstoreCmd.setTabCompleter(cmd);
        }
    }

    /**
     * Registers all event listeners.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /**
     * Cancels all scheduled tasks.
     */
    private void cancelTasks() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
    }

    /**
     * Establishes connection to the SetStore API.
     */
    public void connect() {
        logInfo("Connecting to SetStore...");

        String serverIp = pluginConfig.getServerIp();
        int serverPort = pluginConfig.getServerPort();
        String serverVersion = Bukkit.getMinecraftVersion();
        List<String> onlinePlayers = getOnlinePlayerNames();

        try {
            ConnectResponse response = api.connect(serverIp, serverPort, serverVersion, onlinePlayers);

            if (response != null && response.isSuccess()) {
                handleSuccessfulConnection(response);
            } else {
                handleFailedConnection(response);
            }
        } catch (Exception e) {
            logError("Error connecting to SetStore: " + e.getMessage());
            if (pluginConfig.isDebug()) {
                e.printStackTrace();
            }
            scheduleReconnect();
        }
    }

    /**
     * Handles a successful API connection.
     *
     * @param response The connection response
     */
    private void handleSuccessfulConnection(ConnectResponse response) {
        connected = true;
        serverId = response.getServer().getId();
        serverName = response.getServer().getName();

        logInfo("Connected to SetStore!");
        logInfo("Server: " + serverName + " (ID: " + serverId + ")");

        if (response.getPendingDeliveries() > 0) {
            logInfo("Pending deliveries: " + response.getPendingDeliveries());
            Bukkit.getScheduler().runTaskAsynchronously(this, deliveryExecutor::processQueue);
        }

        WebSocketConfig wsConfig = response.getWebsocket();
        if (pluginConfig.isWebSocketEnabled() && wsConfig != null) {
            startWebSocket(wsConfig);
        }

        startHeartbeat();

        if (pluginConfig.isPollingEnabled()) {
            startPolling();
        }
    }

    /**
     * Handles a failed API connection.
     *
     * @param response The connection response (may be null)
     */
    private void handleFailedConnection(ConnectResponse response) {
        String errorMsg = response != null ? response.getMessage() : "Unknown error";
        logError("Failed to connect to SetStore: " + errorMsg);
        connected = false;
        scheduleReconnect();
    }

    /**
     * Schedules a reconnection attempt.
     */
    private void scheduleReconnect() {
        connected = false;
        long delayTicks = pluginConfig.getWebSocketReconnectDelay() * 20L;
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, this::connect, delayTicks);
    }

    /**
     * Starts the WebSocket connection for real-time delivery notifications.
     *
     * @param config The WebSocket configuration
     */
    private void startWebSocket(WebSocketConfig config) {
        logInfo("Connecting to WebSocket...");
        webSocket = new SetStoreWebSocket(this, config);
        webSocket.connect();
    }

    /**
     * Starts the heartbeat task to keep the server marked as online.
     */
    private void startHeartbeat() {
        int intervalTicks = pluginConfig.getHeartbeatInterval() * 20;

        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!connected) {
                return;
            }

            try {
                HeartbeatResponse response = api.heartbeat();
                if (response != null && response.isSuccess()) {
                    logDebug("Heartbeat sent successfully");
                    if (response.getPendingDeliveries() > 0) {
                        logDebug("Pending deliveries: " + response.getPendingDeliveries());
                        deliveryExecutor.processQueue();
                    }
                } else {
                    logWarning("Heartbeat failed, attempting to reconnect...");
                    connected = false;
                    connect();
                }
            } catch (Exception e) {
                logError("Heartbeat error: " + e.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    /**
     * Starts the polling task as a fallback for WebSocket.
     */
    private void startPolling() {
        int intervalTicks = pluginConfig.getPollingInterval() * 20;

        pollingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (connected && (webSocket == null || !webSocket.isConnected())) {
                logDebug("Polling for deliveries...");
                deliveryExecutor.processQueue();
            }
        }, intervalTicks, intervalTicks);
    }

    /**
     * Reconnects to the SetStore API.
     */
    public void reconnect() {
        connected = false;

        if (webSocket != null) {
            webSocket.disconnect();
            webSocket = null;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, this::connect);
    }

    /**
     * Gets a list of online player names.
     *
     * @return List of player names
     */
    public List<String> getOnlinePlayerNames() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        return players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    /**
     * Reports the current online players to the API.
     *
     * @param players List of player names
     */
    public void notifyOnlinePlayers(List<String> players) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                api.reportOnlinePlayers(players);
                logDebug("Reported " + players.size() + " online players");
            } catch (Exception e) {
                logError("Failed to report online players: " + e.getMessage());
            }
        });
    }

    // ==================== Getters ====================

    /**
     * Gets the plugin instance.
     *
     * @return The plugin instance
     */
    public static SetStorePlugin getInstance() {
        return instance;
    }

    /**
     * Gets the plugin configuration.
     *
     * @return The plugin configuration
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    /**
     * Gets the API client.
     *
     * @return The API client
     */
    public SetStoreAPI getApi() {
        return api;
    }

    /**
     * Gets the delivery executor.
     *
     * @return The delivery executor
     */
    public DeliveryExecutor getDeliveryExecutor() {
        return deliveryExecutor;
    }

    /**
     * Checks if connected to the API.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Gets the server ID.
     *
     * @return The server ID
     */
    public int getServerId() {
        return serverId;
    }

    /**
     * Gets the server name.
     *
     * @return The server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Checks if WebSocket is connected.
     *
     * @return true if WebSocket is connected
     */
    public boolean isWebSocketConnected() {
        return webSocket != null && webSocket.isConnected();
    }

    // ==================== Logging ====================

    /**
     * Logs an info message.
     *
     * @param message The message to log
     */
    public void logInfo(String message) {
        getLogger().info(message);
    }

    /**
     * Logs a warning message.
     *
     * @param message The message to log
     */
    public void logWarning(String message) {
        getLogger().warning(message);
    }

    /**
     * Logs an error message.
     *
     * @param message The message to log
     */
    public void logError(String message) {
        getLogger().severe(message);
    }

    /**
     * Logs a debug message (only when debug mode is enabled).
     *
     * @param message The message to log
     */
    public void logDebug(String message) {
        if (pluginConfig != null && pluginConfig.isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level   The log level
     * @param message The message to log
     */
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
}
