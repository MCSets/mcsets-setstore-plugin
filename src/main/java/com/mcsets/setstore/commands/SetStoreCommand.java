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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Admin command handler for the SetStore plugin.
 * Provides subcommands for status, configuration, and management.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class SetStoreCommand extends Command implements TabExecutor {

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
        super("setstore", "mcsets.admin", "ss", "mcsets");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mcsets.admin")) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().getMessage("no-permission")));
            return;
        }

        if (args.length == 0) {
            showHelp(sender);
            return;
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
    }

    private void handleReload(CommandSender sender) {
        plugin.getPluginConfig().reload();
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().getMessage("reload-success")));
        plugin.logInfo("Configuration reloaded by " + sender.getName());
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&8&m                    &r &bSetStore Status &8&m                    ")));

        // Connection status
        if (plugin.isConnected()) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7API Status: &aConnected")));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Server: &f" + plugin.getServerName() + " &7(ID: &f" + plugin.getServerId() + "&7)")));
        } else {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7API Status: &cDisconnected")));
        }

        // WebSocket status
        if (plugin.getPluginConfig().isWebSocketEnabled()) {
            if (plugin.isWebSocketConnected()) {
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&7WebSocket: &aConnected")));
            } else {
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&7WebSocket: &cDisconnected")));
            }
        } else {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7WebSocket: &7Disabled")));
        }

        // Polling status
        if (plugin.getPluginConfig().isPollingEnabled()) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Polling: &aEnabled &7(every " + plugin.getPluginConfig().getPollingInterval() + "s)")));
        } else {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Polling: &7Disabled")));
        }

        // Processing status
        int processing = plugin.getDeliveryExecutor().getProcessingCount();
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&7Processing: &f" + processing + " &7deliveries")));

        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&8&m                                                          ")));
    }

    private void handleQueue(CommandSender sender) {
        if (!plugin.isConnected()) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&cSetStore is not connected. Cannot fetch queue.")));
            return;
        }

        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&7Fetching pending deliveries...")));

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            plugin.getDeliveryExecutor().processQueue();

            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&aQueue processed! Check console for details.")));
        });
    }

    private void handleReconnect(CommandSender sender) {
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&7Reconnecting to SetStore...")));

        plugin.reconnect();

        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            if (plugin.isConnected()) {
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().getMessage("status-connected")));
            } else {
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().getMessage("status-disconnected")));
            }
        }, 3, TimeUnit.SECONDS);
    }

    private void handleDebug(CommandSender sender) {
        boolean newValue = plugin.getPluginConfig().toggle("debug");
        String status = newValue ? "&aenabled" : "&cdisabled";
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&7Debug mode " + status)));
        plugin.logInfo("Debug mode " + (newValue ? "enabled" : "disabled") + " by " + sender.getName());
    }

    private void handleLogging(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&8&m                    &r &bLogging Settings &8&m                    ")));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Debug: " + (plugin.getPluginConfig().isDebug() ? "&aON" : "&cOFF"))));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Log Requests: " + (plugin.getPluginConfig().isLogRequests() ? "&aON" : "&cOFF"))));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Log Deliveries: " + (plugin.getPluginConfig().isLogDeliveries() ? "&aON" : "&cOFF"))));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Log Commands: " + (plugin.getPluginConfig().isLogCommands() ? "&aON" : "&cOFF"))));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&8&m                                                          ")));
            sender.sendMessage(new TextComponent(
                    plugin.getPluginConfig().formatMessage("&7Usage: &b/setstore logging <requests|deliveries|commands|all|none>")));
            return;
        }

        String option = args[1].toLowerCase();

        switch (option) {
            case "all":
                plugin.getPluginConfig().setLogRequests(true);
                plugin.getPluginConfig().setLogDeliveries(true);
                plugin.getPluginConfig().setLogCommands(true);
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&aAll logging enabled")));
                break;

            case "none":
            case "off":
                plugin.getPluginConfig().setLogRequests(false);
                plugin.getPluginConfig().setLogDeliveries(false);
                plugin.getPluginConfig().setLogCommands(false);
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&cAll logging disabled")));
                break;

            case "requests":
                boolean reqValue = plugin.getPluginConfig().toggle("requests");
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&7Request logging " + (reqValue ? "&aenabled" : "&cdisabled"))));
                break;

            case "deliveries":
                boolean delValue = plugin.getPluginConfig().toggle("deliveries");
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&7Delivery logging " + (delValue ? "&aenabled" : "&cdisabled"))));
                break;

            case "commands":
                boolean cmdValue = plugin.getPluginConfig().toggle("commands");
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&7Command logging " + (cmdValue ? "&aenabled" : "&cdisabled"))));
                break;

            default:
                sender.sendMessage(new TextComponent(
                        plugin.getPluginConfig().formatMessage("&cUnknown option. Use: requests, deliveries, commands, all, or none")));
                break;
        }

        plugin.logInfo("Logging settings changed by " + sender.getName());
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&8&m                    &r &bSetStore Commands &8&m                    ")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore status &8- &7Show connection status")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore queue &8- &7Process pending deliveries")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore reconnect &8- &7Reconnect to SetStore")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore debug &8- &7Toggle debug mode")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore logging [option] &8- &7Configure logging")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore reload &8- &7Reload configuration")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&b/setstore help &8- &7Show this help")));
        sender.sendMessage(new TextComponent(
                plugin.getPluginConfig().formatMessage("&8&m                                                          ")));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
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
