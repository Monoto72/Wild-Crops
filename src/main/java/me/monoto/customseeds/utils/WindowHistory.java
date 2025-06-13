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

    private static final Map<UUID, Deque<Supplier<? extends Window>>> history =
            new ConcurrentHashMap<>();

    public static <W extends Window> void open(Player player, Supplier<W> supplier) {
        Deque<Supplier<? extends Window>> stack =
                history.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());

        if (stack.isEmpty() || stack.peek() != supplier) {
            stack.push(supplier);
        }

        W window = supplier.get();
        window.setFallbackWindow(() -> goBack(player));
        window.addCloseHandler(reason -> cleanupIfEmpty(player, stack));
        window.open();
    }

    public static <W extends Window> void replace(Player player, Supplier<W> supplier) {
        Deque<Supplier<? extends Window>> stack = history.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());

        if (!stack.isEmpty()) {
            stack.pop();
        }
        open(player, supplier);
    }

    public static void clear(Player player) {
        Deque<Supplier<? extends Window>> stack = history.remove(player.getUniqueId());
        if (stack != null) {
            stack.clear();
        }
    }

    private static Window goBack(Player player) {
        UUID id = player.getUniqueId();
        Deque<Supplier<? extends Window>> stack = history.get(id);
        if (stack == null || stack.isEmpty()) {
            history.remove(id);
            return null;
        }

        // pop it too, so we don't re-push a duplicate
        stack.pop();
        Supplier<? extends Window> prevSupplier = stack.peek();
        if (prevSupplier == null) {
            history.remove(id);
            return null;
        }

        open(player, prevSupplier);
        return null;
    }

    private static void cleanupIfEmpty(Player player, Deque<Supplier<? extends Window>> stack) {
        if (!player.isOnline() || stack.isEmpty()) {
            history.remove(player.getUniqueId());
        }
    }
}