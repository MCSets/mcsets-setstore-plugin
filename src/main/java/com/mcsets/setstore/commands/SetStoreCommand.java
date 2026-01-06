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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin command handler for the SetStore plugin.
 * Provides subcommands for status, configuration, and management.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class SetStoreCommand implements CommandExecutor, TabCompleter {

    private final SetStorePlugin plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "status", "queue", "reconnect", "debug", "logging", "help"
    );
    private static final List<String> LOGGING_OPTIONS = Arrays.asList(
        "requests", "deliveries", "commands", "all", "none"
    );

    /**
     * Creates a new command handler.
     *
     * @param plugin The plugin instance
     */
    public SetStoreCommand(SetStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mcsets.admin")) {
            sender.sendMessage(plugin.getPluginConfig().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "status":
                handleStatus(sender);
                break;

            case "queue":
                handleQueue(sender);
                break;

            case "reconnect":
                handleReconnect(sender);
                break;

            case "debug":
                handleDebug(sender);
                break;

            case "logging":
            case "log":
                handleLogging(sender, args);
                break;

            case "help":
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.getPluginConfig().reload();
        sender.sendMessage(plugin.getPluginConfig().getMessage("reload-success"));
        plugin.logInfo("Configuration reloaded by " + sender.getName());
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&8&m                    &r &bSetStore Status &8&m                    "));

        // Connection status
        if (plugin.isConnected()) {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7API Status: &aConnected"));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Server: &f" + plugin.getServerName() + " &7(ID: &f" + plugin.getServerId() + "&7)"));
        } else {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7API Status: &cDisconnected"));
        }

        // WebSocket status
        if (plugin.getPluginConfig().isWebSocketEnabled()) {
            if (plugin.isWebSocketConnected()) {
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&7WebSocket: &aConnected"));
            } else {
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&7WebSocket: &cDisconnected"));
            }
        } else {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7WebSocket: &7Disabled"));
        }

        // Polling status
        if (plugin.getPluginConfig().isPollingEnabled()) {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Polling: &aEnabled &7(every " + plugin.getPluginConfig().getPollingInterval() + "s)"));
        } else {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Polling: &7Disabled"));
        }

        // Processing status
        int processing = plugin.getDeliveryExecutor().getProcessingCount();
        sender.sendMessage(plugin.getPluginConfig().formatMessage(
            "&7Processing: &f" + processing + " &7deliveries"));

        sender.sendMessage(plugin.getPluginConfig().formatMessage("&8&m                                                          "));
    }

    private void handleQueue(CommandSender sender) {
        if (!plugin.isConnected()) {
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&cSetStore is not connected. Cannot fetch queue."));
            return;
        }

        sender.sendMessage(plugin.getPluginConfig().formatMessage(
            "&7Fetching pending deliveries..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDeliveryExecutor().processQueue();

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&aQueue processed! Check console for details."));
            });
        });
    }

    private void handleReconnect(CommandSender sender) {
        sender.sendMessage(plugin.getPluginConfig().formatMessage(
            "&7Reconnecting to SetStore..."));

        plugin.reconnect();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.isConnected()) {
                sender.sendMessage(plugin.getPluginConfig().getMessage("status-connected"));
            } else {
                sender.sendMessage(plugin.getPluginConfig().getMessage("status-disconnected"));
            }
        }, 60L); // Check after 3 seconds
    }

    private void handleDebug(CommandSender sender) {
        boolean newValue = plugin.getPluginConfig().toggle("debug");
        String status = newValue ? "&aenabled" : "&cdisabled";
        sender.sendMessage(plugin.getPluginConfig().formatMessage(
            "&7Debug mode " + status));
        plugin.logInfo("Debug mode " + (newValue ? "enabled" : "disabled") + " by " + sender.getName());
    }

    private void handleLogging(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Show current logging status
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&8&m                    &r &bLogging Settings &8&m                    "));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Debug: " + (plugin.getPluginConfig().isDebug() ? "&aON" : "&cOFF")));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Log Requests: " + (plugin.getPluginConfig().isLogRequests() ? "&aON" : "&cOFF")));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Log Deliveries: " + (plugin.getPluginConfig().isLogDeliveries() ? "&aON" : "&cOFF")));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Log Commands: " + (plugin.getPluginConfig().isLogCommands() ? "&aON" : "&cOFF")));
            sender.sendMessage(plugin.getPluginConfig().formatMessage("&8&m                                                          "));
            sender.sendMessage(plugin.getPluginConfig().formatMessage(
                "&7Usage: &b/setstore logging <requests|deliveries|commands|all|none>"));
            return;
        }

        String option = args[1].toLowerCase();

        switch (option) {
            case "all":
                plugin.getPluginConfig().setLogRequests(true);
                plugin.getPluginConfig().setLogDeliveries(true);
                plugin.getPluginConfig().setLogCommands(true);
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&aAll logging enabled"));
                break;

            case "none":
            case "off":
                plugin.getPluginConfig().setLogRequests(false);
                plugin.getPluginConfig().setLogDeliveries(false);
                plugin.getPluginConfig().setLogCommands(false);
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&cAll logging disabled"));
                break;

            case "requests":
                boolean reqValue = plugin.getPluginConfig().toggle("requests");
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&7Request logging " + (reqValue ? "&aenabled" : "&cdisabled")));
                break;

            case "deliveries":
                boolean delValue = plugin.getPluginConfig().toggle("deliveries");
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&7Delivery logging " + (delValue ? "&aenabled" : "&cdisabled")));
                break;

            case "commands":
                boolean cmdValue = plugin.getPluginConfig().toggle("commands");
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&7Command logging " + (cmdValue ? "&aenabled" : "&cdisabled")));
                break;

            default:
                sender.sendMessage(plugin.getPluginConfig().formatMessage(
                    "&cUnknown option. Use: requests, deliveries, commands, all, or none"));
                break;
        }

        plugin.logInfo("Logging settings changed by " + sender.getName());
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&8&m                    &r &bSetStore Commands &8&m                    "));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore status &8- &7Show connection status"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore queue &8- &7Process pending deliveries"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore reconnect &8- &7Reconnect to SetStore"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore debug &8- &7Toggle debug mode"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore logging [option] &8- &7Configure logging"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore reload &8- &7Reload configuration"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&b/setstore help &8- &7Show this help"));
        sender.sendMessage(plugin.getPluginConfig().formatMessage("&8&m                                                          "));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("mcsets.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("logging") || subCommand.equals("log")) {
                return LOGGING_OPTIONS.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
