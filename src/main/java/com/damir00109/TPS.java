package com.damir00109;

import java.util.Queue;

public class TPS {
    /**
     * Метод для получения информации о TPS.
     */
    public static String getTpsInfo() {
        StringBuilder builder = new StringBuilder();

        // Заголовки
        builder.append(String.format("%-15s", "TPS:"))
                .append(String.format("%-15s", "MSPT:"))
                .append(String.format("%-20s", "Ресурсы:"))
                .append("\n");

        // Собираем все значения TPS для определения максимальной длины
        double tps5sValue = getAverage(VTPS.tps5s);
        double tps1mValue = getAverage(VTPS.tps1m);
        double tps5mValue = getAverage(VTPS.tps5m);
        double tps15mValue = getAverage(VTPS.tps15m);

        // Преобразуем значения TPS в строки
        String tps5sStr = String.format("%.2f", tps5sValue);
        String tps1mStr = String.format("%.2f", tps1mValue);
        String tps5mStr = String.format("%.2f", tps5mValue);
        String tps15mStr = String.format("%.2f", tps15mValue);

        // Находим максимальную длину строки TPS
        int maxTpsLength = Math.max(Math.max(tps5sStr.length(), tps1mStr.length()),
                Math.max(tps5mStr.length(), tps15mStr.length()));

        // 5 секунд
        builder.append("├─ 5s: ")
                .append(String.format("%-" + maxTpsLength + "s", tps5sStr))
                .append("   ├─ 5s: ")
                .append(String.format("%-10.2f", getAverage(VTPS.mspt5s)))
                .append("   CPU: ").append(String.format("%.2f%%", VTPS.getCpuUsage()))
                .append("\n");

        // 1 минута
        builder.append("├─ 1m: ")
                .append(String.format("%-" + maxTpsLength + "s", tps1mStr))
                .append("   ├─ 1m: ")
                .append(String.format("%-10.2f", getAverage(VTPS.mspt1m)))
                .append("   RAM: ").append(String.format("%.2f%%", VTPS.getRamUsagePercentage()))
                .append("\n");

        // 5 минут
        builder.append("├─ 5m: ")
                .append(String.format("%-" + maxTpsLength + "s", tps5mStr))
                .append("   ├─ 5m: ")
                .append(String.format("%-10.2f", getAverage(VTPS.mspt5m)))
                .append("\n");

        // 15 минут
        builder.append("└─ 15m: ")
                .append(String.format("%-" + maxTpsLength + "s", tps15mStr))
                .append("  └─ 15m: ")
                .append(String.format("%-10.2f", getAverage(VTPS.mspt15m)))
                .append("\n");

        // Информация о памяти (отдельная строка)
        builder.append("\nИспользовано: ").append(VTPS.getRamUsageFormatted()).append("\n");

        return builder.toString();
    }

    /**
     * Метод для расчета среднего значения.
     */
    private static double getAverage(Queue<Double> queue) {
        if (queue.isEmpty()) return 0;
        double sum = 0;
        for (double value : queue) {
            sum += value;
        }
        return sum / queue.size();
    }
}