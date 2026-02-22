package com.damir00109;

import java.util.Queue;

public class TPS {

    public static String getTpsInfo() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%-15s", "TPS:"))
                .append(String.format("%-15s", "MSPT:"))
                .append(String.format("%-20s", "Ресурсы:"))
                .append("\n");

        double tps5s = getAverage(VTPS.tps5s);
        double tps1m = getAverage(VTPS.tps1m);
        double tps5m = getAverage(VTPS.tps5m);
        double tps15m = getAverage(VTPS.tps15m);

        String s5 = String.format("%.2f", tps5s);
        String s1 = String.format("%.2f", tps1m);
        String s5m = String.format("%.2f", tps5m);
        String s15 = String.format("%.2f", tps15m);

        int max = Math.max(Math.max(s5.length(), s1.length()), Math.max(s5m.length(), s15.length()));

        builder.append("├─ 5s: ").append(String.format("%-" + max + "s", s5))
                .append("   ├─ 5s: ").append(String.format("%-10.2f", getAverage(VTPS.mspt5s)))
                .append("   CPU: ").append(String.format("%.2f%%", VTPS.getCpuUsage())).append("\n");

        builder.append("├─ 1m: ").append(String.format("%-" + max + "s", s1))
                .append("   ├─ 1m: ").append(String.format("%-10.2f", getAverage(VTPS.mspt1m)))
                .append("   RAM: ").append(String.format("%.2f%%", VTPS.getRamUsagePercentage())).append("\n");

        builder.append("├─ 5m: ").append(String.format("%-" + max + "s", s5m))
                .append("   ├─ 5m: ").append(String.format("%-10.2f", getAverage(VTPS.mspt5m))).append("\n");

        builder.append("└─ 15m: ").append(String.format("%-" + max + "s", s15))
                .append("  └─ 15m: ").append(String.format("%-10.2f", getAverage(VTPS.mspt15m))).append("\n");

        builder.append("\nИспользовано: ").append(VTPS.getRamUsageFormatted()).append("\n");

        return builder.toString();
    }

    private static double getAverage(Queue<Double> queue) {
        if (queue.isEmpty()) return 0;
        double sum = 0;
        for (double v : queue) sum += v;
        return sum / queue.size();
    }
}