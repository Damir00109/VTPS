package com.damir00109;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarTPS {
    private static final Map<UUID, ServerBossBar> playerBossBars = new HashMap<>();
    private static final Map<UUID, Boolean> playerBossBarStates = new HashMap<>();

    /**
     * Включает или выключает Boss Bar для конкретного игрока.
     */
    public static void toggleBossBar(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        boolean isEnabled = playerBossBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopBossBarUpdates(player); // Выключаем Boss Bar
            player.sendMessage(Text.of("TPS BossBar выключен"), false);
        } else {
            startBossBarUpdates(player); // Включаем Boss Bar
            player.sendMessage(Text.of("TPS BossBar включён"), false);
        }

        playerBossBarStates.put(playerId, !isEnabled); // Обновляем состояние
    }

    /**
     * Запускает обновление Boss Bar для игрока.
     */
    private static void startBossBarUpdates(ServerPlayerEntity player) {
        ServerBossBar bossBar = new ServerBossBar(
                Text.literal("TPS: 0.00, MSPT: 0.00ms, Ping: 0ms"),
                ServerBossBar.Color.GREEN, // Начальный цвет
                ServerBossBar.Style.PROGRESS
        );

        bossBar.addPlayer(player); // Показываем Boss Bar игроку
        playerBossBars.put(player.getUuid(), bossBar); // Сохраняем Boss Bar

        // Запускаем поток для обновления Boss Bar
        new Thread(() -> {
            while (playerBossBarStates.getOrDefault(player.getUuid(), false)) {
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
    private static void stopBossBarUpdates(ServerPlayerEntity player) {
        ServerBossBar bossBar = playerBossBars.get(player.getUuid());
        if (bossBar != null) {
            bossBar.removePlayer(player); // Скрываем Boss Bar
            playerBossBars.remove(player.getUuid()); // Удаляем Boss Bar
        }
    }

    /**
     * Обновляет текст и цвет Boss Bar для игрока.
     */
    private static void updateBossBar(ServerPlayerEntity player) {
        ServerBossBar bossBar = playerBossBars.get(player.getUuid());
        if (bossBar == null) return;

        double tps = VTPS.getCurrentTPS();
        double mspt = VTPS.getCurrentMSPT();
        int ping = player.networkHandler.getLatency(); // Пинг игрока

        // Формируем текст для Boss Bar
        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);

        // Обновляем текст и цвет Boss Bar
        bossBar.setName(Text.literal(message)); // Устанавливаем текст
        bossBar.setColor(getBossBarColor(tps)); // Устанавливаем цвет
    }

    /**
     * Возвращает цвет Boss Bar в зависимости от значения TPS.
     */
    private static ServerBossBar.Color getBossBarColor(double tps) {
        if (tps >= 18.0) {
            return ServerBossBar.Color.GREEN; // Всё хорошо
        } else if (tps >= 15.0) {
            return ServerBossBar.Color.YELLOW; // Средняя нагрузка
        } else {
            return ServerBossBar.Color.RED; // Высокая нагрузка
        }
    }
}