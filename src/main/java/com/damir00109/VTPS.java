package com.damir00109;

import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ModInitializer;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Queue;

import com.mojang.brigadier.CommandDispatcher;

import static net.minecraft.commands.Commands.literal;

public class VTPS implements ModInitializer {
	public static final String MOD_ID = "vanilla-tps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("VTPS mod initialized");
	}

	// Общие поля (остаются без изменений)
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

	private static void collectStatistics(double tps, double mspt) {
		// Код без изменений
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

	public static double getCurrentTPS() {
		return tps;
	}

	public static double getCurrentMSPT() {
		return mspt;
	}

	public static double getCpuUsage() {
		com.sun.management.OperatingSystemMXBean osBean =
				(com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		double load = osBean.getProcessCpuLoad() * 100;
		return Double.isNaN(load) ? 0 : load;
	}

	public static double getRamUsagePercentage() {
		Runtime runtime = Runtime.getRuntime();
		long total = runtime.totalMemory();
		long free = runtime.freeMemory();
		long used = total - free;
		long max = runtime.maxMemory();
		return (double) used / max * 100;
	}

	public static String getRamUsageFormatted() {
		Runtime runtime = Runtime.getRuntime();
		long total = runtime.totalMemory();
		long free = runtime.freeMemory();
		long used = total - free;
		long max = runtime.maxMemory();

		return String.format("%dM / %dM (max: %dM)",
				used / 1024 / 1024,
				total / 1024 / 1024,
				max / 1024 / 1024
		);
	}

	private static boolean isOp(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) return false;

		// Используем прямой вызов как в оригинальном коде Minecraft
		return source.getServer()
				.getPlayerList()
				.isOp(player.nameAndId());
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		// /vtps info
		dispatcher.register(
				literal("vtps")
						.requires(VTPS::isOp)
						.then(literal("info")
								.executes(ctx -> {
									ServerPlayer player = ctx.getSource().getPlayer();
									if (player != null) {
										player.sendSystemMessage(Component.literal("Автор мода: ").append(Component.literal("damir00109").withStyle(ChatFormatting.GOLD)));
										player.sendSystemMessage(Component.literal("Спасибо за использование мода!").withStyle(ChatFormatting.GREEN));
									}
									return 1;
								})
						)
		);

		// /vtps actionbar
		dispatcher.register(
				literal("vtps")
						.requires(VTPS::isOp)
						.then(literal("actionbar")
								.executes(ctx -> {
									ServerPlayer p = ctx.getSource().getPlayer();
									if (p != null) ActionBar.toggleActionBar(p);
									return 1;
								})
						)
		);

		// /vtps bossbar
		dispatcher.register(
				literal("vtps")
						.requires(VTPS::isOp)
						.then(literal("bossbar")
								.executes(ctx -> {
									ServerPlayer p = ctx.getSource().getPlayer();
									if (p != null) BossBarTPS.toggleBossBar(p);
									return 1;
								})
						)
		);

		// /tps - с локализацией
		dispatcher.register(
				literal("tps")
						.requires(VTPS::isOp)
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayer();
							if (player != null) {
								ctx.getSource().sendSystemMessage(Component.literal(TPS.getTpsInfo()));
							} else {
								ctx.getSource().sendSystemMessage(Component.literal(TPS.getTpsInfo()));
							}
							return 1;
						})
		);

		// /tps-actionbar - устаревшая команда
		dispatcher.register(
				literal("tps-actionbar")
						.requires(VTPS::isOp)
						.executes(ctx -> {
							ctx.getSource().sendSystemMessage(Component.literal("Команда /tps-actionbar ").append(Component.literal("устарела").withStyle(ChatFormatting.YELLOW)).append(Component.literal(". Используйте /vtps actionbar")));
							ServerPlayer p = ctx.getSource().getPlayer();
							if (p != null) ActionBar.toggleActionBar(p);
							return 1;
						})
		);

		// /tabtps - устаревшая команда
		dispatcher.register(
				literal("tabtps")
						.requires(VTPS::isOp)
						.executes(ctx -> {
							ctx.getSource().sendSystemMessage(Component.literal("Команда /tabtps ").append(Component.literal("устарела").withStyle(ChatFormatting.YELLOW)).append(Component.literal(". Используйте /vtps bossbar")));
							ServerPlayer p = ctx.getSource().getPlayer();
							if (p != null) BossBarTPS.toggleBossBar(p);
							return 1;
						})
		);
	}
}