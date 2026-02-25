package com.damir00109;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Queue;
import com.mojang.brigadier.CommandDispatcher;
import com.damir00109.ActionBar;
import com.damir00109.BossBarTPS;

import static net.minecraft.server.command.CommandManager.literal;

public class VTPS {
	public static final String MOD_ID = "vanilla-tps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Общие поля (теперь public)
	public static long lastTickTime = 0;
	public static double tps = 20.0;
	public static double mspt = 50.0;
	public static final Queue<Double> tps5s = new ArrayDeque<>(100);
	public static final Queue<Double> tps1m = new ArrayDeque<>(1200);
	public static final Queue<Double> tps5m = new ArrayDeque<>(6000);
	public static final Queue<Double> tps15m = new ArrayDeque<>(18000);
	public static final Queue<Double> mspt5s = new ArrayDeque<>(100);
	public static final Queue<Double> mspt1m = new ArrayDeque<>(1200);
	public static final Queue<Double> mspt5m = new ArrayDeque<>(6000);
	public static final Queue<Double> mspt15m = new ArrayDeque<>(18000);

	/**
	 * Метод для обработки серверных тиков.
	 */
	public static void onServerTick(MinecraftServer server) {
		long currentTime = System.nanoTime();
		if (lastTickTime != 0) {
			double deltaTime = (currentTime - lastTickTime) / 1_000_000_000.0;
			tps = 1.0 / deltaTime;
			mspt = deltaTime * 1000;

			collectStatistics(tps, mspt);
		}
		lastTickTime = currentTime;
	}

	/**
	 * Метод для сбора статистики TPS и MSPT.
	 */
	private static void collectStatistics(double tps, double mspt) {
		tps5s.add(tps);
		tps1m.add(tps);
		tps5m.add(tps);
		tps15m.add(tps);

		mspt5s.add(mspt);
		mspt1m.add(mspt);
		mspt5m.add(mspt);
		mspt15m.add(mspt);

		if (tps5s.size() > 100) tps5s.poll();
		if (tps1m.size() > 1200) tps1m.poll();
		if (tps5m.size() > 6000) tps5m.poll();
		if (tps15m.size() > 18000) tps15m.poll();

		if (mspt5s.size() > 100) mspt5s.poll();
		if (mspt1m.size() > 1200) mspt1m.poll();
		if (mspt5m.size() > 6000) mspt5m.poll();
		if (mspt15m.size() > 18000) mspt15m.poll();
	}

	/**
	 * Возвращает текущее значение TPS.
	 */
	public static double getCurrentTPS() {
		return tps;
	}

	/**
	 * Возвращает текущее значение MSPT.
	 */
	public static double getCurrentMSPT() {
		return mspt;
	}

	/**
	 * Метод для получения информации о CPU.
	 */
	public static double getCpuUsage() {
		com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		double processLoad = osBean.getProcessCpuLoad() * 100;
		return Double.isNaN(processLoad) ? 0 : processLoad;
	}

	/**
	 * Метод для получения информации о RAM в процентах.
	 */
	public static double getRamUsagePercentage() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = totalMemory - freeMemory;
		long maxMemory = runtime.maxMemory();
		return (double) usedMemory / maxMemory * 100;
	}

	/**
	 * Метод для получения информации о RAM в формате "Использовано: XM / YM (max: ZM)".
	 */
	public static String getRamUsageFormatted() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = totalMemory - freeMemory;
		long maxMemory = runtime.maxMemory();
		return String.format("%dM / %dM (max: %dM)",
				usedMemory / 1024 / 1024,
				totalMemory / 1024 / 1024,
				maxMemory / 1024 / 1024);
	}

	/**
	 * Метод для регистрации команд.
	 */
	public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		// /vtps info
		dispatcher.register(
				literal("vtps")
						.requires(source -> source.hasPermissionLevel(2))
						.then(literal("info")
								.executes(ctx -> {
									ServerPlayerEntity player = ctx.getSource().getPlayer();
									if (player != null) {
										player.sendMessage(Text.literal("Автор мода: ").append(Text.literal("damir00109").formatted(Formatting.GOLD)));
										player.sendMessage(Text.literal("Спасибо за использование мода!").formatted(Formatting.GREEN));
									}
									return 1;
								})
						)
		);

		// /vtps actionbar
		dispatcher.register(
				literal("vtps")
						.requires(source -> source.hasPermissionLevel(2))
						.then(literal("actionbar")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									if (player != null) {
										ActionBar.toggleActionBar(player);
									}
									return 1;
								})
						)
		);
		// /vtps bossbar
		dispatcher.register(
				literal("vtps")
						.requires(source -> source.hasPermissionLevel(2))
						.then(literal("bossbar")
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									if (player != null) {
										BossBarTPS.toggleBossBar(player);
									}
									return 1;
								})
						)
		);



		// Команда /tps
		dispatcher.register(
				literal("tps")
						.requires(source -> source.hasPermissionLevel(2)) // Требует уровня доступа 2 (оператор)
						.executes(context -> {
							context.getSource().sendMessage(Text.of(TPS.getTpsInfo()));
							return 1;
						})
		);

		// Команда /tps-actionbar
		dispatcher.register(
				literal("tps-actionbar")
						.requires(source -> source.hasPermissionLevel(2)) // Требует уровня доступа 2 (оператор)
						.executes(context -> {
							context.getSource().sendFeedback(() -> Text.literal("Команда /tps-actionbar ").append(Text.literal("устарела").formatted(Formatting.YELLOW)).append(Text.literal(". Используйте /vtps actionbar")), true);
							ServerPlayerEntity player = context.getSource().getPlayer();
							if (player != null) {
								ActionBar.toggleActionBar(player);
							}
							return 1;
						})
		);

		// Команда /tabtps
		dispatcher.register(
				literal("tabtps")
						.requires(source -> source.hasPermissionLevel(2)) // Требует уровня доступа 2 (оператор)
						.executes(context -> {
							context.getSource().sendFeedback(() -> Text.literal("Команда /tabtps ").append(Text.literal("устарела").formatted(Formatting.YELLOW)).append(Text.literal(". Используйте /vtps bossbar")), true);
							ServerPlayerEntity player = context.getSource().getPlayer();
							if (player != null) {
								BossBarTPS.toggleBossBar(player);
							}
							return 1;
						})
		);
	}
}
