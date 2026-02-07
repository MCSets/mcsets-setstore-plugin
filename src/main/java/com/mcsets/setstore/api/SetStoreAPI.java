/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcsets.setstore.SetStorePlugin;
import com.mcsets.setstore.models.ApiResponse;
import com.mcsets.setstore.models.ConnectResponse;
import com.mcsets.setstore.models.HeartbeatResponse;
import com.mcsets.setstore.models.QueueResponse;
import com.mcsets.setstore.models.VerifyResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for the SetStore API.
 * Handles all communication with the MCSets SetStore backend.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class SetStoreAPI {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";

    private final SetStorePlugin plugin;
    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl;
    private final String apiKey;

    /**
     * Creates a new API client.
     *
     * @param plugin The plugin instance
     */
    public SetStoreAPI(SetStorePlugin plugin) {
        this.plugin = plugin;
        this.baseUrl = plugin.getPluginConfig().getBaseUrl();
        this.apiKey = plugin.getPluginConfig().getApiKey();
        this.gson = new GsonBuilder().create();

        int timeout = plugin.getPluginConfig().getApiTimeout();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Shuts down the API client and releases resources.
     */
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    /**
     * Registers the server with the SetStore API.
     *
     * @param serverIp      The server IP address
     * @param serverPort    The server port
     * @param serverVersion The Minecraft server version
     * @param onlinePlayers List of online player names
     * @return The connection response
     */
    public ConnectResponse connect(String serverIp, int serverPort, String serverVersion, List<String> onlinePlayers) {
        ConnectRequest payload = new ConnectRequest(apiKey, serverIp, serverPort, serverVersion, onlinePlayers);
        String json = gson.toJson(payload);

        plugin.logDebug("Sending connect request to " + baseUrl + "/connect");
        logRequest("Connect", json);

        Request request = new Request.Builder()
                .url(baseUrl + "/connect")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);
            logResponse("Connect", responseBody);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, ConnectResponse.class);
            } else {
                plugin.logError("Connect failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Connect request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetches pending deliveries from the queue.
     *
     * @return The queue response containing pending deliveries
     */
    public QueueResponse getQueue() {
        plugin.logDebug("Fetching delivery queue from " + baseUrl + "/queue");

        Request request = new Request.Builder()
                .url(baseUrl + "/queue")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);
            logResponse("Queue", responseBody);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, QueueResponse.class);
            } else {
                plugin.logError("Queue fetch failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Queue request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reports the result of a delivery execution.
     *
     * @param deliveryId      The delivery ID
     * @param status          The delivery status (success, failed, partial)
     * @param actionsExecuted List of executed commands
     * @param errorMessage    Error message if failed
     * @param durationMs      Execution duration in milliseconds
     * @return The API response
     */
    public ApiResponse reportDelivery(int deliveryId, String status, List<String> actionsExecuted,
                                      String errorMessage, long durationMs) {
        DeliverRequest payload = new DeliverRequest(deliveryId, status, actionsExecuted, errorMessage, durationMs);
        String json = gson.toJson(payload);

        plugin.logDebug("Reporting delivery " + deliveryId + " as " + status);
        logRequest("Deliver", json);

        Request request = new Request.Builder()
                .url(baseUrl + "/deliver")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);
            logResponse("Deliver", responseBody);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, ApiResponse.class);
            } else {
                plugin.logError("Deliver report failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Deliver request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reports the list of online players.
     *
     * @param players List of player names
     * @return The API response
     */
    public ApiResponse reportOnlinePlayers(List<String> players) {
        String json = gson.toJson(new OnlineRequest(players));

        plugin.logDebug("Reporting " + players.size() + " online players");

        Request request = new Request.Builder()
                .url(baseUrl + "/online")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, ApiResponse.class);
            } else {
                plugin.logError("Online report failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Online request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends a heartbeat to keep the server marked as online.
     *
     * @return The heartbeat response
     */
    public HeartbeatResponse heartbeat() {
        Request request = new Request.Builder()
                .url(baseUrl + "/heartbeat")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .post(RequestBody.create("{}", JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, HeartbeatResponse.class);
            } else {
                plugin.logError("Heartbeat failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Heartbeat request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a verification code for a player.
     *
     * @param username The player's username
     * @param uuid     The player's UUID
     * @return The verification response
     */
    public VerifyResponse verify(String username, String uuid) {
        String json = gson.toJson(new VerifyRequest(username, uuid));

        plugin.logDebug("Generating verification code for " + username);

        Request request = new Request.Builder()
                .url(baseUrl + "/verify")
                .header(HEADER_API_KEY, apiKey)
                .header(HEADER_ACCEPT, APPLICATION_JSON)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = getResponseBody(response);
            logResponse("Verify", responseBody);

            if (response.isSuccessful()) {
                return gson.fromJson(responseBody, VerifyResponse.class);
            } else {
                plugin.logError("Verify failed with status " + response.code() + ": " + responseBody);
                return null;
            }
        } catch (IOException e) {
            plugin.logError("Verify request failed: " + e.getMessage());
            return null;
        }
    }

    // ==================== Helper Methods ====================

    private String getResponseBody(Response response) throws IOException {
        return response.body() != null ? response.body().string() : "";
    }

    private void logRequest(String endpoint, String json) {
        if (plugin.getPluginConfig().isLogRequests()) {
            plugin.logInfo(endpoint + " request: " + json);
        }
    }

    private void logResponse(String endpoint, String json) {
        if (plugin.getPluginConfig().isLogRequests()) {
            plugin.logInfo(endpoint + " response: " + json);
        }
    }

    // ==================== Request DTOs ====================

    /**
     * Request payload for server connection.
     */
    @SuppressWarnings("unused")
    private static class ConnectRequest {
        final String api_key;
        final String server_ip;
        final int server_port;
        final String server_version;
        final List<String> online_players;

        ConnectRequest(String apiKey, String serverIp, int serverPort,
                       String serverVersion, List<String> onlinePlayers) {
            this.api_key = apiKey;
            this.server_ip = serverIp;
            this.server_port = serverPort;
            this.server_version = serverVersion;
            this.online_players = onlinePlayers;
        }
    }

    /**
     * Request payload for delivery result reporting.
     */
    @SuppressWarnings("unused")
    private static class DeliverRequest {
        final int delivery_id;
        final String status;
        final List<String> actions_executed;
        final String error_message;
        final long duration_ms;

        DeliverRequest(int deliveryId, String status, List<String> actionsExecuted,
                       String errorMessage, long durationMs) {
            this.delivery_id = deliveryId;
            this.status = status;
            this.actions_executed = actionsExecuted;
            this.error_message = errorMessage;
            this.duration_ms = durationMs;
        }
    }

    /**
     * Request payload for online players reporting.
     */
    @SuppressWarnings("unused")
    private static class OnlineRequest {
        final List<String> players;

        OnlineRequest(List<String> players) {
            this.players = players;
        }
    }

    /**
     * Request payload for player verification.
     */
    @SuppressWarnings("unused")
    private static class VerifyRequest {
        final String username;
        final String uuid;

        VerifyRequest(String username, String uuid) {
            this.username = username;
            this.uuid = uuid;
        }
    }
}
