package com.damir00109;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarTPS {
    private static final Map<UUID, ServerBossEvent> playerBossBars = new HashMap<>();
    private static final Map<UUID, Boolean> playerBossBarStates = new HashMap<>();

    public static void toggleBossBar(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean isEnabled = playerBossBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopBossBarUpdates(player);
            player.sendSystemMessage(Component.literal("BossBar TPS ").append(Component.literal("выключен").withStyle(ChatFormatting.RED)));
        } else {
            startBossBarUpdates(player);
            player.sendSystemMessage(Component.literal("BossBar TPS ").append(Component.literal("включен").withStyle(ChatFormatting.GREEN)));
        }

        playerBossBarStates.put(playerId, !isEnabled);
    }

    private static void startBossBarUpdates(ServerPlayer player) {
        ServerBossEvent bossBar = new ServerBossEvent(
                player.getUUID(),
                Component.literal("TPS: 0.00, MSPT: 0.00ms, Ping: 0ms"),
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.PROGRESS // Исправлено на BossBarOverlay
        );

        bossBar.addPlayer(player);
        playerBossBars.put(player.getUUID(), bossBar);

        new Thread(() -> {
            while (playerBossBarStates.getOrDefault(player.getUUID(), false)) {
                updateBossBar(player);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void stopBossBarUpdates(ServerPlayer player) {
        ServerBossEvent bossBar = playerBossBars.get(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player);
            playerBossBars.remove(player.getUUID());
        }
    }

    private static void updateBossBar(ServerPlayer player) {
        ServerBossEvent bossBar = playerBossBars.get(player.getUUID());
        if (bossBar == null) return;

        double tps = VTPS.getCurrentTPS();
        double mspt = VTPS.getCurrentMSPT();
        int ping = player.connection.latency();

        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);
        bossBar.setName(Component.literal(message));
        bossBar.setColor(getBossBarColor(tps));
    }

    private static BossEvent.BossBarColor getBossBarColor(double tps) {
        if (tps >= 18.0) {
            return BossEvent.BossBarColor.GREEN;
        } else if (tps >= 15.0) {
            return BossEvent.BossBarColor.YELLOW;
        } else {
            return BossEvent.BossBarColor.RED;
        }
    }
}