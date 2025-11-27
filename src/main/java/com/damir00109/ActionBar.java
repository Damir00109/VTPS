package com.damir00109;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBar {
    private static final Map<UUID, Boolean> playerActionBarStates = new HashMap<>();

    public static boolean isActionBarEnabled(ServerPlayer player) { // ServerPlayerEntity -> ServerPlayer
        return playerActionBarStates.getOrDefault(player.getUUID(), false); // getUuid() -> getUUID()
    }

    public static void toggleActionBar(ServerPlayer player) { // ServerPlayerEntity -> ServerPlayer
        UUID playerId = player.getUUID(); // getUuid() -> getUUID()
        boolean isEnabled = playerActionBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopActionBarUpdates(player);
            player.sendSystemMessage(Component.literal("TPS ActionBar выключен")); // sendMessage -> sendSystemMessage, Text -> Component
        } else {
            startActionBarUpdates(player);
            player.sendSystemMessage(Component.literal("TPS ActionBar включён"));
        }

        playerActionBarStates.put(playerId, !isEnabled);
    }

    private static void startActionBarUpdates(ServerPlayer player) { // ServerPlayerEntity -> ServerPlayer
        new Thread(() -> {
            while (playerActionBarStates.getOrDefault(player.getUUID(), false)) { // getUuid() -> getUUID()
                updateActionBar(player);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void stopActionBarUpdates(ServerPlayer player) { // ServerPlayerEntity -> ServerPlayer
        // Ничего не нужно делать
    }

    public static void updateActionBar(ServerPlayer player) { // ServerPlayerEntity -> ServerPlayer
        double tps = VanillaTPS.getCurrentTPS();
        double mspt = VanillaTPS.getCurrentMSPT();
        int ping = player.connection.latency(); // networkHandler.getLatency() -> connection.latency()

        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);
        player.sendSystemMessage(Component.literal(message), true); // sendMessage -> sendSystemMessage, Text -> Component
    }
}