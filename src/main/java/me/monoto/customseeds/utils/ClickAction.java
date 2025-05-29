package me.monoto.customseeds.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

public class ClickAction {
    public final ClickType clickType;
    public final Component description;
    public final @Nullable Component currentValue;

    public ClickAction(ClickType clickType, Component description, @Nullable Component currentValue) {
        this.clickType = clickType;
        this.description = description;
        this.currentValue = currentValue;
    }

    public ClickAction(ClickType clickType, Component description) {
        this(clickType, description, null);
    }

    public ClickAction(ClickType clickType, String description, Component currentValue) {
        this(clickType, Component.text(description), currentValue);
    }

    public ClickAction(ClickType clickType, String description, String currentValue) {
        this(clickType,
                Component.text(description),
                currentValue != null ? Component.text(currentValue) : null);
    }

    public ClickAction(ClickType clickType, String description) {
        this(clickType, Component.text(description), null);
    }

    /**
     * Turn a ClickType enum into a human label, e.g. SHIFT_LEFT → "Shift Left"
     */
    public static String humanize(ClickType type) {
        String raw = type.name().toLowerCase().replace('_', ' ');
        StringBuilder out = new StringBuilder(raw.length());
        for (String w : raw.split(" ")) {
            out.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1))
                    .append(' ');
        }
        out.setLength(out.length() - 1);
        return out.toString();
    }
}
