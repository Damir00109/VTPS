package com.damir00109;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ActionBar {
    private static final Map<UUID, Boolean> playerActionBarStates = new HashMap<>();

    /**
     * Проверяет, включён ли Action Bar для игрока.
     */
    public static boolean isActionBarEnabled(ServerPlayer player) {
        return playerActionBarStates.getOrDefault(player.getUUID(), false);
    }

    /**
     * Включает или выключает Action Bar для конкретного игрока.
     */
    public static void toggleActionBar(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean isEnabled = playerActionBarStates.getOrDefault(playerId, false);

        if (isEnabled) {
            stopActionBarUpdates(player); // Выключаем Action Bar
            player.displayClientMessage(Component.nullToEmpty("TPS ActionBar выключен"), false);
        } else {
            startActionBarUpdates(player); // Включаем Action Bar
            player.displayClientMessage(Component.nullToEmpty("TPS ActionBar включён"), false);
        }

        playerActionBarStates.put(playerId, !isEnabled); // Обновляем состояние
    }

    /**
     * Запускает обновление Action Bar для игрока.
     */
    private static void startActionBarUpdates(ServerPlayer player) {
        // Запускаем поток для обновления Action Bar
        new Thread(() -> {
            while (playerActionBarStates.getOrDefault(player.getUUID(), false)) {
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
    private static void stopActionBarUpdates(ServerPlayer player) {
        // Ничего не нужно делать, поток сам завершится
    }

    /**
     * Обновляет текст Action Bar для игрока.
     */
    public static void updateActionBar(ServerPlayer player) {
        double tps = VTPS.getCurrentTPS();
        double mspt = VTPS.getCurrentMSPT();
        int ping = player.connection.latency(); // Пинг игрока

        // Формируем текст для Action Bar
        String message = String.format("TPS: %.2f, MSPT: %.2fms, Ping: %dms", tps, mspt, ping);

        // Отправляем сообщение в Action Bar
        player.displayClientMessage(Component.nullToEmpty(message), true);
    }
}