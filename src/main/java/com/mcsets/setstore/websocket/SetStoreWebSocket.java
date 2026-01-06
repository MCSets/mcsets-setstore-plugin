/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsets.setstore.SetStorePlugin;
import com.mcsets.setstore.models.Delivery;
import com.mcsets.setstore.models.WebSocketConfig;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket client for real-time delivery notifications.
 * Connects to the SetStore WebSocket server using the Pusher protocol.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class SetStoreWebSocket {

    private static final int MAX_HTTP_ERRORS_BEFORE_DISABLE = 3;

    private final SetStorePlugin plugin;
    private final WebSocketConfig config;
    private final Gson gson;

    private WebSocketClient client;
    private String socketId;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean permanentlyDisabled = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicInteger consecutiveHttpErrors = new AtomicInteger(0);

    /**
     * Creates a new WebSocket client.
     *
     * @param plugin The plugin instance
     * @param config The WebSocket configuration
     */
    public SetStoreWebSocket(SetStorePlugin plugin, WebSocketConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new Gson();
    }

    /**
     * Establishes the WebSocket connection.
     */
    public void connect() {
        if (permanentlyDisabled.get()) {
            plugin.logDebug("WebSocket is disabled, skipping connection");
            return;
        }

        try {
            URI uri = new URI(config.getWebSocketUrl());
            plugin.logDebug("Connecting to WebSocket: " + uri);

            client = createWebSocketClient(uri);
            client.connect();
        } catch (Exception e) {
            plugin.logError("Failed to create WebSocket connection: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Creates the WebSocket client with event handlers.
     */
    private WebSocketClient createWebSocketClient(URI uri) {
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                plugin.logInfo("WebSocket connected!");
                connected.set(true);
                reconnectAttempts.set(0);
            }

            @Override
            public void onMessage(String message) {
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                handleClose(code, reason);
            }

            @Override
            public void onError(Exception ex) {
                plugin.logError("WebSocket error: " + ex.getMessage());
                if (plugin.getPluginConfig().isDebug()) {
                    ex.printStackTrace();
                }
            }
        };
    }

    /**
     * Handles WebSocket close events.
     */
    private void handleClose(int code, String reason) {
        connected.set(false);
        socketId = null;

        // Check if this is an HTTP error (server doesn't support WebSocket)
        if (isHttpError(reason)) {
            int httpErrors = consecutiveHttpErrors.incrementAndGet();
            if (httpErrors >= MAX_HTTP_ERRORS_BEFORE_DISABLE) {
                plugin.logWarning("WebSocket server not available. Disabling WebSocket and using polling instead.");
                permanentlyDisabled.set(true);
                return;
            }
        } else {
            consecutiveHttpErrors.set(0);
        }

        plugin.logDebug("WebSocket disconnected: " + reason + " (code: " + code + ")");
        scheduleReconnect();
    }

    /**
     * Checks if the close reason indicates an HTTP error.
     */
    private boolean isHttpError(String reason) {
        if (reason == null) {
            return false;
        }
        return reason.contains("400") || reason.contains("Bad Request") ||
               reason.contains("404") || reason.contains("Not Found");
    }

    /**
     * Handles incoming WebSocket messages.
     */
    private void handleMessage(String message) {
        plugin.logDebug("WebSocket message: " + message);

        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String event = json.has("event") ? json.get("event").getAsString() : "";

            switch (event) {
                case "pusher:connection_established":
                    handleConnectionEstablished(json);
                    break;

                case "pusher:subscription_succeeded":
                    plugin.logInfo("Subscribed to channel: " + config.getChannel());
                    break;

                case "pusher:subscription_error":
                    plugin.logError("Failed to subscribe to channel!");
                    break;

                case "pusher:ping":
                    sendPong();
                    break;

                case "delivery.new":
                case "App\\Events\\DeliveryEvent":
                    handleDeliveryEvent(json);
                    break;

                case "delivery.pending":
                    handlePendingDeliveries(json);
                    break;

                default:
                    if (!event.startsWith("pusher:")) {
                        plugin.logDebug("Unknown event: " + event);
                    }
                    break;
            }
        } catch (Exception e) {
            plugin.logError("Error parsing WebSocket message: " + e.getMessage());
            if (plugin.getPluginConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnectionEstablished(JsonObject json) {
        try {
            JsonObject data = JsonParser.parseString(json.get("data").getAsString()).getAsJsonObject();
            socketId = data.get("socket_id").getAsString();
            plugin.logDebug("WebSocket socket ID: " + socketId);
            subscribeToChannel();
        } catch (Exception e) {
            plugin.logError("Error handling connection established: " + e.getMessage());
        }
    }

    private void subscribeToChannel() {
        if (client == null || !client.isOpen()) {
            return;
        }

        String channel = config.getChannel();
        JsonObject subscribeMessage = new JsonObject();
        subscribeMessage.addProperty("event", "pusher:subscribe");

        JsonObject data = new JsonObject();
        data.addProperty("channel", channel);

        if (channel.startsWith("private-")) {
            data.addProperty("auth", generateAuth(channel));
        }

        subscribeMessage.add("data", data);
        client.send(subscribeMessage.toString());

        plugin.logDebug("Subscribing to channel: " + channel);
    }

    private String generateAuth(String channel) {
        return plugin.getPluginConfig().getApiKey() + ":" + socketId + ":" + channel;
    }

    private void sendPong() {
        if (client != null && client.isOpen()) {
            JsonObject pong = new JsonObject();
            pong.addProperty("event", "pusher:pong");
            pong.add("data", new JsonObject());
            client.send(pong.toString());
        }
    }

    private void handleDeliveryEvent(JsonObject json) {
        try {
            String dataString = json.get("data").getAsString();
            JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();

            if (data.has("delivery")) {
                Delivery delivery = gson.fromJson(data.get("delivery"), Delivery.class);
                plugin.logInfo("New delivery received via WebSocket: " + delivery);

                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getDeliveryExecutor().executeDelivery(delivery));
            }
        } catch (Exception e) {
            plugin.logError("Error handling delivery event: " + e.getMessage());
            if (plugin.getPluginConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    private void handlePendingDeliveries(JsonObject json) {
        try {
            String dataString = json.get("data").getAsString();
            JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();

            int count = data.has("count") ? data.get("count").getAsInt() : 0;
            if (count > 0) {
                plugin.logInfo("Received notification of " + count + " pending deliveries");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        plugin.getDeliveryExecutor().processQueue());
            }
        } catch (Exception e) {
            plugin.logError("Error handling pending deliveries: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (permanentlyDisabled.get()) {
            return;
        }

        int maxAttempts = plugin.getPluginConfig().getMaxReconnectAttempts();
        int attempts = reconnectAttempts.incrementAndGet();

        if (maxAttempts > 0 && attempts > maxAttempts) {
            plugin.logWarning("Max WebSocket reconnection attempts reached. Using polling instead.");
            permanentlyDisabled.set(true);
            return;
        }

        int delay = plugin.getPluginConfig().getWebSocketReconnectDelay();
        plugin.logDebug("Reconnecting to WebSocket in " + delay + " seconds (attempt " + attempts + ")");

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!connected.get() && !permanentlyDisabled.get()) {
                connect();
            }
        }, delay * 20L);
    }

    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        connected.set(false);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                plugin.logDebug("Error closing WebSocket: " + e.getMessage());
            }
            client = null;
        }
    }

    /**
     * Checks if connected to the WebSocket server.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected.get() && client != null && client.isOpen();
    }

    /**
     * Checks if WebSocket has been permanently disabled.
     *
     * @return true if disabled
     */
    public boolean isPermanentlyDisabled() {
        return permanentlyDisabled.get();
    }

    /**
     * Gets the current socket ID.
     *
     * @return The socket ID
     */
    public String getSocketId() {
        return socketId;
    }
}
