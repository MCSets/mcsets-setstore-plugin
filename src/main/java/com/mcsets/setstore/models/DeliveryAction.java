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

/**
 * Model representing an action to execute for a delivery.
 *
 * @author MCSets
 * @version 1.0.0
 */
public class DeliveryAction {
    @SerializedName("type")
    private String type;

    @SerializedName("value")
    private String value;

    @SerializedName("parsed_value")
    private String parsedValue;

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getParsedValue() {
        return parsedValue;
    }

    public String getExecutableValue() {
        return parsedValue != null && !parsedValue.isEmpty() ? parsedValue : value;
    }

    @Override
    public String toString() {
        return "DeliveryAction{type='" + type + "', value='" + value + "'}";
    }
}
