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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Command handler for player verification.
 * Generates a verification code for linking Minecraft accounts.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class VerifyCommand extends Command {

    private final SetStorePlugin plugin;

    /**
     * Creates a new verify command handler.
     *
     * @param plugin The plugin instance
     */
    public VerifyCommand(SetStorePlugin plugin) {
        super("verify", "mcsets.verify");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&cThis command can only be used by players!")));
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("mcsets.verify")) {
            player.sendMessage(new TextComponent(
                    plugin.getPluginConfig().getMessage("no-permission")));
            return;
        }

        if (!plugin.isConnected()) {
            player.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&cSetStore is not connected. Please try again later.")));
            return;
        }

        player.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&7Generating verification code...")));

        // Generate verification code async
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                VerifyResponse response = plugin.getApi().verify(
                    player.getName(),
                    player.getUniqueId().toString()
                );

                if (response != null && response.isSuccess()) {
                    String code = response.getCode();
                    int expiresInMinutes = response.getExpiresIn() / 60;
                    String storeUrl = response.getStoreUrl();

                    player.sendMessage(new TextComponent(
                            plugin.getPluginConfig().getMessage("verify-code",
                                "{code}", code)));
                    player.sendMessage(new TextComponent(
                            plugin.getPluginConfig().getMessage("verify-expires",
                                "{minutes}", String.valueOf(expiresInMinutes))));

                    if (storeUrl != null && !storeUrl.isEmpty()) {
                        player.sendMessage(new TextComponent(
                                plugin.getPluginConfig().getMessage("verify-instructions",
                                    "{url}", storeUrl)));
                    }

                    plugin.logInfo("Player " + player.getName() + " generated verification code: " + code);
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Unknown error";
                    player.sendMessage(new TextComponent(
                            plugin.getPluginConfig().formatMessage(
                                "&cFailed to generate verification code: " + errorMsg)));
                }

            } catch (Exception e) {
                player.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage(
                            "&cAn error occurred while generating your verification code.")));
                plugin.logError("Error generating verification code for " + player.getName() + ": " + e.getMessage());
            }
        });
    }
}
