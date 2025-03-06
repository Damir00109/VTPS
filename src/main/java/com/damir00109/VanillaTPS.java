package com.damir00109;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class VanillaTPS implements ModInitializer {
	public static final String MOD_ID = "vanilla-tps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private long lastTickTime = 0; // Время последнего тика
	private double tps = 20.0; // Текущий TPS (по умолчанию 20)
	private double mspt = 50.0; // Текущий MSPT (по умолчанию 50 мс)
	private boolean actionBarEnabled = false; // Флаг для включения/выключения Action Bar
	private boolean bossBarEnabled = false; // Флаг для включения/выключения Boss Bar
	private int tickCounter = 0; // Счетчик тиков для задержки
	private static final int TICK_DELAY = 20; // Задержка в тиках (20 тиков = 1 секунда)

	// Очереди для сбора статистики TPS и MSPT
	private final Queue<Double> tps5s = new ArrayDeque<>(100); // 5 секунд (100 тиков)
	private final Queue<Double> tps1m = new ArrayDeque<>(1200); // 1 минута (1200 тиков)
	private final Queue<Double> tps5m = new ArrayDeque<>(6000); // 5 минут (6000 тиков)
	private final Queue<Double> tps15m = new ArrayDeque<>(18000); // 15 минут (18000 тиков)

	private final Queue<Double> mspt5s = new ArrayDeque<>(100); // 5 секунд (100 тиков)
	private final Queue<Double> mspt1m = new ArrayDeque<>(1200); // 1 минута (1200 тиков)
	private final Queue<Double> mspt5m = new ArrayDeque<>(6000); // 5 минут (6000 тиков)
	private final Queue<Double> mspt15m = new ArrayDeque<>(18000); // 15 минут (18000 тиков)

	private ServerBossBar bossBar; // Босс-бар для отображения TPS
	private ScheduledExecutorService bossBarUpdater; // Таймер для обновления босс-бара
	private ScheduledExecutorService actionBarUpdater; // Таймер для обновления Action Bar

	@Override
	public void onInitialize() {
		LOGGER.info("Vanilla TPS Mod initialized!");

		// Регистрируем обработчик событий для серверных тиков
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		// Регистрируем команды
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerTpsCommand(dispatcher);
			registerTpsActionBarCommand(dispatcher);
			registerTpsBarCommand(dispatcher); // Добавляем регистрацию новой команды
		});
	}

	// Метод для обработки серверных тиков
	private void onServerTick(MinecraftServer server) {
		long currentTime = System.nanoTime();
		if (lastTickTime != 0) {
			double deltaTime = (currentTime - lastTickTime) / 1_000_000_000.0; // Разница во времени в секундах
			tps = 1.0 / deltaTime; // Рассчитываем TPS
			mspt = deltaTime * 1000; // Рассчитываем MSPT

			// Собираем статистику TPS и MSPT
			collectStatistics(tps, mspt);

			// Обновляем Action Bar для всех игроков, если он включен
			if (actionBarEnabled) {
				tickCounter++;
				if (tickCounter >= TICK_DELAY) {
					for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
						int ping = player.networkHandler.getLatency();
						player.sendMessage(Text.of("TPS: " + String.format("%.2f", tps) + ", MSPT: " + String.format("%.2f", mspt) + "ms, Ping: " + ping + "ms"), true);
					}
					tickCounter = 0; // Сбрасываем счетчик
				}
			}
		}
		lastTickTime = currentTime;
	}

	// Метод для сбора статистики TPS и MSPT
	private void collectStatistics(double tps, double mspt) {
		tps5s.add(tps);
		tps1m.add(tps);
		tps5m.add(tps);
		tps15m.add(tps);

		mspt5s.add(mspt);
		mspt1m.add(mspt);
		mspt5m.add(mspt);
		mspt15m.add(mspt);

		// Удаляем старые данные, если очередь переполнена
		if (tps5s.size() > 100) tps5s.poll();
		if (tps1m.size() > 1200) tps1m.poll();
		if (tps5m.size() > 6000) tps5m.poll();
		if (tps15m.size() > 18000) tps5s.poll();

		if (mspt5s.size() > 100) mspt5s.poll();
		if (mspt1m.size() > 1200) mspt5s.poll();
		if (mspt5m.size() > 6000) mspt5s.poll();
		if (mspt15m.size() > 18000) mspt5s.poll();
	}

	// Регистрация команды /tps
	private void registerTpsCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("tps")
				.requires(source -> source.hasPermissionLevel(2)) // Требуется уровень доступа 2 (администратор)
				.executes(this::executeTpsCommand));
	}

	// Выполнение команды /tps
	private int executeTpsCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		// Получаем игрока, вызвавшего команду
		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			// Отправляем сообщение с TPS, MSPT и пингом в чат
			source.sendMessage(Text.of(getTpsInfo()));
		} else {
			// Если команда вызвана не игроком (например, консолью), отправляем сообщение в чат
			source.sendMessage(Text.of("This command can only be executed by a player."));
		}

		return 1; // Успешное выполнение команды
	}

	// Метод для получения информации о TPS и MSPT
	private String getTpsInfo() {
		StringBuilder builder = new StringBuilder();

		// TPS
		builder.append("TPS: ");
		builder.append(String.format("%.2f (5s), ", getAverage(tps5s)));
		builder.append(String.format("%.2f (1m), ", getAverage(tps1m)));
		builder.append(String.format("%.2f (5m), ", getAverage(tps5m)));
		builder.append(String.format("%.2f (15m)\n", getAverage(tps15m)));

		// MSPT
		builder.append("MSPT - Средний, Минимум, Максимум\n");
		builder.append("├─ 5s - ").append(getMsptStats(mspt5s)).append("\n");
		builder.append("├─ 10s - ").append(getMsptStats(mspt1m)).append("\n");
		builder.append("└─ 60s - ").append(getMsptStats(mspt5m)).append("\n");

		// CPU и RAM
		builder.append("CPU: ").append(getCpuUsage()).append("\n");
		builder.append("RAM: ").append(getRamUsage()).append("\n");

		return builder.toString();
	}

	// Метод для расчета среднего значения
	private double getAverage(Queue<Double> queue) {
		if (queue.isEmpty()) return 0;
		double sum = 0;
		for (double value : queue) {
			sum += value;
		}
		return sum / queue.size();
	}

	// Метод для расчета статистики MSPT (среднее, минимум, максимум)
	private String getMsptStats(Queue<Double> queue) {
		if (queue.isEmpty()) return "0, 0, 0";
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double sum = 0;
		for (double value : queue) {
			sum += value;
			if (value < min) min = value;
			if (value > max) max = value;
		}
		double average = sum / queue.size();
		return String.format("%.2f, %.2f, %.2f", average, min, max);
	}

	// Метод для получения информации о CPU
	private String getCpuUsage() {
		com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		double processLoad = osBean.getProcessCpuLoad() * 100;

		return String.format("%.2f%% (proc.)", processLoad);
	}

	// Метод для получения информации о RAM
	private String getRamUsage() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory(); // Общая выделенная память JVM
		long freeMemory = runtime.freeMemory(); // Свободная память JVM
		long maxMemory = runtime.maxMemory(); // Максимальная память, которую может выделить JVM

		// Использованная память
		long usedMemory = totalMemory - freeMemory;

		// Процент использованной памяти
		double usedPercentage = (double) usedMemory / maxMemory * 100;

		// Физический объем оперативной памяти (если нужно)
		com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		long physicalMemory = osBean.getTotalMemorySize(); // Физическая память компьютера

		return String.format("JVM: %.2f%% (%dM/%dM, max. %dM), Physical: %dM",
				usedPercentage,
				usedMemory / 1024 / 1024,
				totalMemory / 1024 / 1024,
				maxMemory / 1024 / 1024,
				physicalMemory / 1024 / 1024);
	}

	// Регистрация команды /tps-actionbar
	private void registerTpsActionBarCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("tps-actionbar")
				.requires(source -> source.hasPermissionLevel(0)) // Доступно всем (уровень доступа 0)
				.executes(this::executeTpsActionBarCommand) // Без аргумента
				.then(argument("playerName", StringArgumentType.word()) // С аргументом
						.suggests(PLAYER_NAME_SUGGESTIONS) // Добавляем подсказки
						.executes(this::executeTpsActionBarCommandWithPlayer)));
	}

	// Выполнение команды /tps-actionbar без аргумента
	private int executeTpsActionBarCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		// Переключаем состояние Action Bar
		actionBarEnabled = !actionBarEnabled;

		// Отправляем сообщение в чат
		source.sendMessage(Text.of("TPS-Actionbar " + (actionBarEnabled ? "включён" : "выключен")));

		// Получаем игрока, вызвавшего команду
		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			handleActionBarForPlayer(player, actionBarEnabled);
		} else {
			source.sendMessage(Text.of("This command can only be executed by a player."));
		}

		return 1; // Успешное выполнение команды
	}

	// Выполнение команды /tps-actionbar с аргументом (ник игрока)
	private int executeTpsActionBarCommandWithPlayer(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		String playerName = StringArgumentType.getString(context, "playerName");

		// Получаем игрока по имени
		ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
		if (targetPlayer == null) {
			source.sendMessage(Text.of("Игрок с именем " + playerName + " не найден."));
			return 0; // Команда не выполнена
		}

		// Переключаем состояние Action Bar для указанного игрока
		boolean newActionBarState = !actionBarEnabled;
		actionBarEnabled = newActionBarState;

		// Отправляем сообщение в чат
		source.sendMessage(Text.of("TPS-Actionbar для игрока " + playerName + " " + (newActionBarState ? "включён" : "выключен")));

		// Обрабатываем Action Bar для указанного игрока
		handleActionBarForPlayer(targetPlayer, newActionBarState);

		return 1; // Успешное выполнение команды
	}

	// Метод для обработки Action Bar для указанного игрока
	private void handleActionBarForPlayer(ServerPlayerEntity player, boolean enable) {
		if (enable) {
			// Запускаем таймер для обновления Action Bar каждые 500 мс
			actionBarUpdater = Executors.newSingleThreadScheduledExecutor();
			actionBarUpdater.scheduleAtFixedRate(() -> {
				int ping = player.networkHandler.getLatency();
				player.sendMessage(Text.of("TPS: " + String.format("%.2f", getAverage(tps5s)) + " MSPT: " + String.format("%.2f", getAverage(mspt5s)) + "ms Ping: " + ping + "ms"), true);
			}, 0, 500, TimeUnit.MILLISECONDS);
		} else {
			// Останавливаем таймер
			if (actionBarUpdater != null) {
				actionBarUpdater.shutdown();
			}
		}
	}

	// Регистрация команды /tpsbar
	private void registerTpsBarCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("tpsbar")
				.requires(source -> source.hasPermissionLevel(0)) // Доступно всем (уровень доступа 0)
				.executes(this::executeTpsBarCommand) // Без аргумента
				.then(argument("playerName", StringArgumentType.word()) // С аргументом
						.suggests(PLAYER_NAME_SUGGESTIONS) // Добавляем подсказки
						.executes(this::executeTpsBarCommandWithPlayer)));
	}

	// Выполнение команды /tpsbar без аргумента
	private int executeTpsBarCommand(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		// Переключаем состояние Boss Bar
		bossBarEnabled = !bossBarEnabled;

		// Отправляем сообщение в чат
		source.sendMessage(Text.of("TPS-Bossbar " + (bossBarEnabled ? "включён" : "выключен")));

		// Получаем игрока, вызвавшего команду
		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			handleBossBarForPlayer(player, bossBarEnabled);
		} else {
			source.sendMessage(Text.of("This command can only be executed by a player."));
		}

		return 1; // Успешное выполнение команды
	}

	// Выполнение команды /tpsbar с аргументом (ник игрока)
	private int executeTpsBarCommandWithPlayer(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		String playerName = StringArgumentType.getString(context, "playerName");

		// Получаем игрока по имени
		ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);
		if (targetPlayer == null) {
			source.sendMessage(Text.of("Игрок с именем " + playerName + " не найден."));
			return 0; // Команда не выполнена
		}

		// Переключаем состояние Boss Bar для указанного игрока
		boolean newBossBarState = !bossBarEnabled;
		bossBarEnabled = newBossBarState;

		// Отправляем сообщение в чат
		source.sendMessage(Text.of("TPS-Bossbar для игрока " + playerName + " " + (newBossBarState ? "включён" : "выключен")));
		// Обрабатываем босс-бар для указанного игрока
		handleBossBarForPlayer(targetPlayer, newBossBarState);

		return 1; // Успешное выполнение команды
	}

	// Метод для обработки босс-бара для указанного игрока
	private void handleBossBarForPlayer(ServerPlayerEntity player, boolean enable) {
		if (enable) {
			// Создаем босс-бар
			bossBar = new ServerBossBar(Text.of("TPS: " + String.format("%.2f", getAverage(tps5s)) + " MSPT: " + String.format("%.2f", getAverage(mspt5s)) + "ms Ping: " + player.networkHandler.getLatency() + "ms"), getBossBarColor(getAverage(tps5s)), BossBar.Style.PROGRESS);
			bossBar.setPercent(Math.min((float) (getAverage(tps5s) / 20.0), 1.0f)); // Ограничиваем прогресс до 100%

			// Добавляем босс-бар игроку
			bossBar.addPlayer(player);

			// Запускаем таймер для обновления босс-бара каждые 500 мс
			bossBarUpdater = Executors.newSingleThreadScheduledExecutor();
			bossBarUpdater.scheduleAtFixedRate(() -> {
				bossBar.setName(Text.of("TPS: " + String.format("%.2f", getAverage(tps5s)) + " MSPT: " + String.format("%.2f", getAverage(mspt5s)) + "ms Ping: " + player.networkHandler.getLatency() + "ms"));
				bossBar.setPercent(Math.min((float) (getAverage(tps5s) / 20.0), 1.0f)); // Ограничиваем прогресс до 100%
				bossBar.setColor(getBossBarColor(getAverage(tps5s)));
			}, 0, 500, TimeUnit.MILLISECONDS);
		} else {
			// Останавливаем таймер и удаляем босс-бар
			if (bossBarUpdater != null) {
				bossBarUpdater.shutdown();
			}
			if (bossBar != null) {
				bossBar.setVisible(false);
				bossBar.clearPlayers();
			}
		}
	}

	// Метод для получения цвета босс-бара в зависимости от TPS
	private BossBar.Color getBossBarColor(double tps) {
		if (tps >= 18) {
			return BossBar.Color.GREEN;
		} else if (tps >= 15) {
			return BossBar.Color.YELLOW;
		} else {
			return BossBar.Color.RED;
		}
	}

	// Провайдер подсказок для имен игроков
	private static final SuggestionProvider<ServerCommandSource> PLAYER_NAME_SUGGESTIONS = (context, builder) -> {
		MinecraftServer server = context.getSource().getServer();
		List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
		for (ServerPlayerEntity player : players) {
			builder.suggest(player.getName().getString());
		}
		return builder.buildFuture();
	};
}