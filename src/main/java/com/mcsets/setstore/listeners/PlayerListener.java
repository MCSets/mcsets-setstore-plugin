/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.listeners;

import com.mcsets.setstore.SetStorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * Event listener for player join/quit events.
 * Reports online player changes to the SetStore API.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class PlayerListener implements Listener {

    private final SetStorePlugin plugin;

    /**
     * Creates a new player listener.
     *
     * @param plugin The plugin instance
     */
    public PlayerListener(SetStorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isConnected()) {
            return;
        }

        // Update online players list after a short delay to ensure player is fully joined
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<String> onlinePlayers = plugin.getOnlinePlayerNames();
            plugin.notifyOnlinePlayers(onlinePlayers);

            // Also trigger queue processing for this player's pending deliveries
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDeliveryExecutor().processQueue();
            });
        }, 20L); // 1 second delay

        plugin.logDebug("Player joined: " + event.getPlayer().getName() + " - notifying SetStore");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.isConnected()) {
            return;
        }

        // Update online players list after player leaves
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<String> onlinePlayers = plugin.getOnlinePlayerNames();
            plugin.notifyOnlinePlayers(onlinePlayers);
        }, 5L); // Short delay to ensure player is removed

        plugin.logDebug("Player quit: " + event.getPlayer().getName() + " - notifying SetStore");
    }
}
