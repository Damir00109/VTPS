package com.damir00109;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;

/**
 * Упрощённая утилита для получения пинга игрока, ориентированная на 1.20.1.
 * Берёт приватное поле "connection" у ServerPlayer и читаeт int поле "latency" в соединении.
 */
public class PingUtil {
	private static Field connectionField;
	private static Field latencyField;
	private static boolean initialized = false;

	public static int getPing(ServerPlayer player) {
		try {
			if (!initialized) initReflection(player);
			if (connectionField == null || latencyField == null) return 0;

			Object connection = connectionField.get(player);
			if (connection == null) return 0;

			return latencyField.getInt(connection);
		} catch (Exception e) {
			// В простом варианте не печатаем большой стектрейс, возвращаем 0
			return 0;
		}
	}

	private static synchronized void initReflection(ServerPlayer player) {
		if (initialized) return;
		initialized = true;
		try {
			// Попробуем найти поле connection в ServerPlayer
			try {
				connectionField = player.getClass().getDeclaredField("connection");
				connectionField.setAccessible(true);
			} catch (NoSuchFieldException e) {
				// ничего
			}

			if (connectionField == null) return;

			// Попробуем найти поле latency в объекте connection
			Class<?> connClass = connectionField.getType();
			try {
				latencyField = connClass.getDeclaredField("latency");
				latencyField.setAccessible(true);
			} catch (NoSuchFieldException e) {
				// ничего
			}
		} catch (Exception ignored) {
		}
	}
}
