package me.monoto.customseeds.utils;

import org.bukkit.permissions.PermissionDefault;

public class PermDefinition {
    private final String node;
    private final String description;
    private final PermissionDefault defaultValue;

    public PermDefinition(String node, String description, PermissionDefault defaultValue) {
        this.node = node;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getNode() {
        return node;
    }

    public String getDescription() {
        return description;
    }

    public PermissionDefault getDefaultValue() {
        return defaultValue;
    }
}
