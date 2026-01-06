/*
 * MCSets SetStore Plugin
 * Copyright (c) 2025-2026 MCSets
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * https://mcsets.com
 */
package com.mcsets.setstore.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model representing a delivery from the SetStore queue.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class Delivery {
    @SerializedName("id")
    private int id;

    @SerializedName("player_username")
    private String playerUsername;

    @SerializedName("player_uuid")
    private String playerUuid;

    @SerializedName("package_name")
    private String packageName;

    @SerializedName("actions")
    private List<DeliveryAction> actions;

    public int getId() {
        return id;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<DeliveryAction> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return "Delivery{id=" + id + ", player='" + playerUsername + "', package='" + packageName + "'}";
    }
}
