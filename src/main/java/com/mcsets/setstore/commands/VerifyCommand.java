/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.commands;

import com.mcsets.setstore.SetStorePlugin;
import com.mcsets.setstore.models.VerifyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for player verification.
 * Generates a verification code for linking Minecraft accounts.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class VerifyCommand implements CommandExecutor {

    private final SetStorePlugin plugin;

    /**
     * Creates a new verify command handler.
     *
     * @param plugin The plugin instance
     */
    public VerifyCommand(SetStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPluginConfig().formatMessage("&cThis command can only be used by players!"));
            return true;
        }

        if (!player.hasPermission("mcsets.verify")) {
            player.sendMessage(plugin.getPluginConfig().getMessage("no-permission"));
            return true;
        }

        if (!plugin.isConnected()) {
            player.sendMessage(plugin.getPluginConfig().formatMessage("&cSetStore is not connected. Please try again later."));
            return true;
        }

        player.sendMessage(plugin.getPluginConfig().formatMessage("&7Generating verification code..."));

        // Generate verification code async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                VerifyResponse response = plugin.getApi().verify(
                    player.getName(),
                    player.getUniqueId().toString()
                );

                // Send response on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (response != null && response.isSuccess()) {
                        String code = response.getCode();
                        int expiresInMinutes = response.getExpiresIn() / 60;
                        String storeUrl = response.getStoreUrl();

                        player.sendMessage(plugin.getPluginConfig().getMessage("verify-code",
                            "{code}", code));
                        player.sendMessage(plugin.getPluginConfig().getMessage("verify-expires",
                            "{minutes}", String.valueOf(expiresInMinutes)));

                        if (storeUrl != null && !storeUrl.isEmpty()) {
                            player.sendMessage(plugin.getPluginConfig().getMessage("verify-instructions",
                                "{url}", storeUrl));
                        }

                        plugin.logInfo("Player " + player.getName() + " generated verification code: " + code);
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Unknown error";
                        player.sendMessage(plugin.getPluginConfig().formatMessage(
                            "&cFailed to generate verification code: " + errorMsg));
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getPluginConfig().formatMessage(
                        "&cAn error occurred while generating your verification code."));
                });
                plugin.logError("Error generating verification code for " + player.getName() + ": " + e.getMessage());
            }
        });

        return true;
    }
}
