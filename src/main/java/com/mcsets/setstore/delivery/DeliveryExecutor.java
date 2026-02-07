/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.delivery;

import com.mcsets.setstore.SetStorePlugin;
import com.mcsets.setstore.models.Delivery;
import com.mcsets.setstore.models.DeliveryAction;
import com.mcsets.setstore.models.QueueResponse;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes deliveries from the SetStore queue.
 * Handles command execution, duplicate prevention, and result reporting.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class DeliveryExecutor {

    private final SetStorePlugin plugin;
    private final ConcurrentHashMap<Integer, Delivery> processingDeliveries = new ConcurrentHashMap<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);

    /**
     * Creates a new delivery executor.
     *
     * @param plugin The plugin instance
     */
    public DeliveryExecutor(SetStorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Process all pending deliveries from the queue
     */
    public void processQueue() {
        if (!isProcessingQueue.compareAndSet(false, true)) {
            plugin.logDebug("Queue processing already in progress, skipping...");
            return;
        }

        try {
            QueueResponse response = plugin.getApi().getQueue();

            if (response == null || !response.isSuccess()) {
                plugin.logDebug("No deliveries to process or failed to fetch queue");
                return;
            }

            List<Delivery> deliveries = response.getDeliveries();
            if (deliveries == null || deliveries.isEmpty()) {
                plugin.logDebug("Queue is empty");
                return;
            }

            plugin.logInfo("Processing " + deliveries.size() + " pending deliveries");

            for (Delivery delivery : deliveries) {
                if (!processingDeliveries.containsKey(delivery.getId())) {
                    executeDelivery(delivery);
                }
            }

        } catch (Exception e) {
            plugin.logError("Error processing queue: " + e.getMessage());
            if (plugin.getPluginConfig().isDebug()) {
                e.printStackTrace();
            }
        } finally {
            isProcessingQueue.set(false);
        }
    }

    /**
     * Execute a single delivery
     */
    public void executeDelivery(Delivery delivery) {
        if (delivery == null) {
            return;
        }

        // Prevent duplicate processing
        if (processingDeliveries.putIfAbsent(delivery.getId(), delivery) != null) {
            plugin.logDebug("Delivery " + delivery.getId() + " is already being processed");
            return;
        }

        long startTime = System.currentTimeMillis();
        List<String> executedActions = new ArrayList<>();
        String errorMessage = null;
        String status = "success";

        try {
            String playerName = delivery.getPlayerUsername();
            String playerUuidStr = delivery.getPlayerUuid();
            String packageName = delivery.getPackageName();

            if (plugin.getPluginConfig().isLogDeliveries()) {
                plugin.logInfo("Processing delivery #" + delivery.getId() + " for " + playerName + " - " + packageName);
            }

            // Check if player is online (if required)
            ProxiedPlayer player = null;
            if (playerUuidStr != null && !playerUuidStr.isEmpty()) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    player = ProxyServer.getInstance().getPlayer(playerUuid);
                } catch (IllegalArgumentException e) {
                    plugin.logDebug("Invalid UUID format: " + playerUuidStr);
                }
            }

            if (player == null) {
                player = ProxyServer.getInstance().getPlayer(playerName);
            }

            boolean requireOnline = plugin.getPluginConfig().isRequireOnline();
            if (requireOnline && player == null) {
                errorMessage = "Player '" + playerName + "' not found on proxy";
                status = "failed";
                plugin.logWarning("Delivery #" + delivery.getId() + " failed: " + errorMessage);
            } else {
                // Execute all actions
                List<DeliveryAction> actions = delivery.getActions();
                if (actions != null && !actions.isEmpty()) {
                    boolean allSucceeded = true;
                    boolean anySucceeded = false;

                    int commandDelayMs = plugin.getPluginConfig().getCommandDelay();
                    int delayOffset = 0;

                    for (DeliveryAction action : actions) {
                        try {
                            if ("command".equals(action.getType())) {
                                String command = action.getExecutableValue();

                                // Replace placeholders
                                command = command.replace("{player}", playerName);
                                command = command.replace("{username}", playerName);
                                if (playerUuidStr != null) {
                                    command = command.replace("{uuid}", playerUuidStr);
                                }

                                final String finalCommand = command;
                                final int currentDelay = delayOffset;

                                if (currentDelay > 0) {
                                    ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                                        executeCommand(finalCommand, delivery.getId());
                                    }, currentDelay, TimeUnit.MILLISECONDS);
                                } else {
                                    executeCommand(finalCommand, delivery.getId());
                                }

                                executedActions.add(command);
                                anySucceeded = true;
                                delayOffset += commandDelayMs;
                            }
                        } catch (Exception e) {
                            allSucceeded = false;
                            plugin.logError("Failed to execute action for delivery #" + delivery.getId() + ": " + e.getMessage());
                            errorMessage = "Failed to execute command: " + e.getMessage();
                        }
                    }

                    if (!allSucceeded && anySucceeded) {
                        status = "partial";
                    } else if (!anySucceeded) {
                        status = "failed";
                        if (errorMessage == null) {
                            errorMessage = "No actions could be executed";
                        }
                    }
                }

                // Notify player if online
                if (player != null && player.isConnected()) {
                    player.sendMessage(new TextComponent(
                            plugin.getPluginConfig().getMessage("delivery-received",
                                    "{package}", packageName)));
                    player.sendMessage(new TextComponent(
                            plugin.getPluginConfig().getMessage("delivery-executed",
                                    "{package}", packageName)));
                }
            }

        } catch (Exception e) {
            status = "failed";
            errorMessage = "Unexpected error: " + e.getMessage();
            plugin.logError("Error executing delivery #" + delivery.getId() + ": " + e.getMessage());
            if (plugin.getPluginConfig().isDebug()) {
                e.printStackTrace();
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Report delivery result
            final String finalStatus = status;
            final String finalErrorMessage = errorMessage;
            final List<String> finalExecutedActions = executedActions;

            ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                try {
                    plugin.getApi().reportDelivery(
                        delivery.getId(),
                        finalStatus,
                        finalExecutedActions,
                        finalErrorMessage,
                        duration
                    );

                    if (plugin.getPluginConfig().isLogDeliveries()) {
                        plugin.logInfo("Delivery #" + delivery.getId() + " completed with status: " + finalStatus);
                    }
                } catch (Exception e) {
                    plugin.logError("Failed to report delivery result: " + e.getMessage());
                } finally {
                    processingDeliveries.remove(delivery.getId());
                }
            });
        }
    }

    private void executeCommand(String command, int deliveryId) {
        try {
            if (plugin.getPluginConfig().isLogCommands()) {
                plugin.logInfo("[Delivery #" + deliveryId + "] Executing: " + command);
            }

            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                    ProxyServer.getInstance().getConsole(), command);

        } catch (Exception e) {
            plugin.logError("[Delivery #" + deliveryId + "] Command failed: " + command + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Check if a specific delivery is currently being processed
     */
    public boolean isProcessing(int deliveryId) {
        return processingDeliveries.containsKey(deliveryId);
    }

    /**
     * Get count of currently processing deliveries
     */
    public int getProcessingCount() {
        return processingDeliveries.size();
    }
}
