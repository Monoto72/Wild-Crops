package me.monoto.customseeds.utils;

import org.bukkit.entity.Player;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class WindowHistory {

    private static final Map<UUID, Deque<Supplier<? extends Window>>> history = new ConcurrentHashMap<>();

    public static <W extends Window> void open(Player player, Supplier<W> supplier) {
        Deque<Supplier<? extends Window>> stack =
                history.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayDeque<>());

        stack.push(supplier);
        W window = supplier.get();

        window.setFallbackWindow(() -> {
            stack.pop();

            Supplier<? extends Window> prevSupplier = stack.peek();
            if (prevSupplier == null) {
                history.remove(player.getUniqueId());
                return null;
            }

            // pop it too, so we don't re-push a duplicate
            stack.pop();

            WindowHistory.open(player, prevSupplier);
            return null;
        });

        window.addCloseHandler(reason -> {
            if (!window.isOpen() && stack.isEmpty()) {
                history.remove(player.getUniqueId());
            }
        });

        window.open();
    }

    public static <W extends Window> void replace(Player player, Supplier<W> supplier) {
        Deque<Supplier<? extends Window>> stack =
                history.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());

        if (!stack.isEmpty()) stack.pop();
        open(player, supplier);
    }

    public static void clear(Player player) {
        Deque<Supplier<? extends Window>> stack = history.remove(player.getUniqueId());
        if (stack != null) stack.clear();
    }
}
