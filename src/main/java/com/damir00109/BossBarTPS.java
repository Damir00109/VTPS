package com.damir00109;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class BossBarTPS {
    private static final Map<UUID, ServerBossEvent> playerBossBars = new HashMap<>();
    private static final Map<UUID, Boolean> playerBossBarStates = new HashMap<>();

    /**
     * Включает или выключает Boss Bar для конкретного игрока.
     */
    public static void toggleBossBar(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean isEnabled = playerBossBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopBossBarUpdates(player); // Выключаем Boss Bar
            player.displayClientMessage(Component.nullToEmpty("TPS BossBar выключен"), false);
        } else {
            startBossBarUpdates(player); // Включаем Boss Bar
            player.displayClientMessage(Component.nullToEmpty("TPS BossBar включён"), false);
        }

        playerBossBarStates.put(playerId, !isEnabled); // Обновляем состояние
    }

    /**
     * Запускает обновление Boss Bar для игрока.
     */
    private static void startBossBarUpdates(ServerPlayer player) {
        ServerBossEvent bossBar = new ServerBossEvent(
                Component.literal("TPS: 0.00, MSPT: 0.00ms, Ping: 0ms"),
                BossEvent.BossBarColor.GREEN, // Начальный цвет
                BossEvent.BossBarOverlay.PROGRESS
        );

        bossBar.addPlayer(player); // Показываем Boss Bar игроку
        playerBossBars.put(player.getUUID(), bossBar); // Сохраняем Boss Bar

        // Запускаем поток для обновления Boss Bar
        new Thread(() -> {
            while (playerBossBarStates.getOrDefault(player.getUUID(), false)) {
                updateBossBar(player); // Обновляем Boss Bar
                try {
                    Thread.sleep(500); // Задержка 500 мс
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Останавливает обновление Boss Bar для игрока.
     */
    private static void stopBossBarUpdates(ServerPlayer player) {
        ServerBossEvent bossBar = playerBossBars.get(player.getUUID());
        if (bossBar != null) {
            bossBar.removePlayer(player); // Скрываем Boss Bar
            playerBossBars.remove(player.getUUID()); // Удаляем Boss Bar
        }
    }

    /**
     * Обновляет текст и цвет Boss Bar для игрока.
     */
    private static void updateBossBar(ServerPlayer player) {
        ServerBossEvent bossBar = playerBossBars.get(player.getUUID());
        if (bossBar == null) return;

        double tps = VTPS.getCurrentTPS();
        double mspt = VTPS.getCurrentMSPT();
        int ping = player.connection.latency(); // Пинг игрока

        // Формируем текст для Boss Bar
        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);

        // Обновляем текст и цвет Boss Bar
        bossBar.setName(Component.literal(message)); // Устанавливаем текст
        bossBar.setColor(getBossBarColor(tps)); // Устанавливаем цвет
    }

    /**
     * Возвращает цвет Boss Bar в зависимости от значения TPS.
     */
    private static BossEvent.BossBarColor getBossBarColor(double tps) {
        if (tps >= 18.0) {
            return BossEvent.BossBarColor.GREEN; // Всё хорошо
        } else if (tps >= 15.0) {
            return BossEvent.BossBarColor.YELLOW; // Средняя нагрузка
        } else {
            return BossEvent.BossBarColor.RED; // Высокая нагрузка
        }
    }
}