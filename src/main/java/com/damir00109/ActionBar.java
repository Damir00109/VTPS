package com.damir00109;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBar {
    private static final Map<UUID, Boolean> playerActionBarStates = new HashMap<>();

    /**
     * Проверяет, включён ли Action Bar для игрока.
     */
    public static boolean isActionBarEnabled(ServerPlayerEntity player) {
        return playerActionBarStates.getOrDefault(player.getUuid(), false);
    }

    /**
     * Включает или выключает Action Bar для конкретного игрока.
     */
    public static void toggleActionBar(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        boolean isEnabled = playerActionBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopActionBarUpdates(player); // Выключаем Action Bar
            player.sendMessage(Text.of("TPS ActionBar выключен"), false);
        } else {
            startActionBarUpdates(player); // Включаем Action Bar
            player.sendMessage(Text.of("TPS ActionBar включён"), false);
        }

        playerActionBarStates.put(playerId, !isEnabled); // Обновляем состояние
    }

    /**
     * Запускает обновление Action Bar для игрока.
     */
    private static void startActionBarUpdates(ServerPlayerEntity player) {
        // Запускаем поток для обновления Action Bar
        new Thread(() -> {
            while (playerActionBarStates.getOrDefault(player.getUuid(), false)) {
                updateActionBar(player); // Обновляем Action Bar
                try {
                    Thread.sleep(500); // Задержка 500 мс
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Останавливает обновление Action Bar для игрока.
     */
    private static void stopActionBarUpdates(ServerPlayerEntity player) {
        // Ничего не нужно делать, поток сам завершится
    }

    /**
     * Обновляет текст Action Bar для игрока.
     */
    public static void updateActionBar(ServerPlayerEntity player) {
        double tps = VanillaTPS.getCurrentTPS();
        double mspt = VanillaTPS.getCurrentMSPT();
        int ping = player.networkHandler.getLatency(); // Пинг игрока

        // Формируем текст для Action Bar
        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);

        // Отправляем сообщение в Action Bar
        player.sendMessage(Text.of(message), true);
    }
}