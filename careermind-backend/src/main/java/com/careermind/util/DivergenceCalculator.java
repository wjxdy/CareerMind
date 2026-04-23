package com.careermind.util;

import com.careermind.domain.Message;

import java.math.BigDecimal;
import java.util.List;

public final class DivergenceCalculator {

    private DivergenceCalculator() {}

    /**
     * v = stddev(confidences)/0.5 * 0.5 + challengeDensity * 0.5
     * Result clamped to [0, 1].
     */
    public static BigDecimal compute(List<Message> roundMessages) {
        if (roundMessages == null || roundMessages.isEmpty()) return BigDecimal.ZERO;

        double[] confs = roundMessages.stream()
            .filter(m -> m.getConfidence() != null)
            .mapToDouble(m -> m.getConfidence().doubleValue())
            .toArray();

        double sd = stddev(confs);
        double sdNorm = Math.min(0.5, sd) / 0.5; // 归一化到 [0,1]

        long challenge = roundMessages.stream()
            .filter(m -> "CHALLENGE".equalsIgnoreCase(m.getEdgeType()))
            .count();
        double density = (double) challenge / roundMessages.size();

        double v = sdNorm * 0.5 + density * 0.5;
        v = Math.max(0.0, Math.min(1.0, v));
        return BigDecimal.valueOf(Math.round(v * 100.0) / 100.0);
    }

    private static double stddev(double[] xs) {
        if (xs.length == 0) return 0;
        double mean = 0;
        for (double x : xs) mean += x;
        mean /= xs.length;
        double sq = 0;
        for (double x : xs) sq += (x - mean) * (x - mean);
        return Math.sqrt(sq / xs.length);
    }
}
