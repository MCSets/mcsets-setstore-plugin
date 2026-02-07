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
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        if (!plugin.isConnected()) {
            return;
        }

        // Update online players list after a short delay to ensure player is fully joined
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            List<String> onlinePlayers = plugin.getOnlinePlayerNames();
            plugin.notifyOnlinePlayers(onlinePlayers);

            // Also trigger queue processing for this player's pending deliveries
            ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                plugin.getDeliveryExecutor().processQueue();
            });
        }, 1, TimeUnit.SECONDS);

        plugin.logDebug("Player joined: " + event.getPlayer().getName() + " - notifying SetStore");
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        if (!plugin.isConnected()) {
            return;
        }

        // Update online players list after player leaves
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            List<String> onlinePlayers = plugin.getOnlinePlayerNames();
            plugin.notifyOnlinePlayers(onlinePlayers);
        }, 250, TimeUnit.MILLISECONDS);

        plugin.logDebug("Player quit: " + event.getPlayer().getName() + " - notifying SetStore");
    }
}
