package com.careermind.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageMetaParser {

    private static final Pattern CONFIDENCE = Pattern.compile(
        "\\[\\s*confidence\\s*:\\s*(\\d(?:\\.\\d{1,2})?)\\s*\\]",
        Pattern.CASE_INSENSITIVE
    );

    private MessageMetaParser() {}

    public static Optional<BigDecimal> parseConfidence(String content) {
        if (content == null) return Optional.empty();
        Matcher m = CONFIDENCE.matcher(content);
        if (!m.find()) return Optional.empty();
        try {
            BigDecimal v = new BigDecimal(m.group(1));
            if (v.compareTo(BigDecimal.ZERO) < 0) v = BigDecimal.ZERO;
            if (v.compareTo(BigDecimal.ONE)  > 0) v = BigDecimal.ONE;
            return Optional.of(v);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static String stripConfidence(String content) {
        if (content == null) return null;
        return CONFIDENCE.matcher(content).replaceAll("").trim();
    }

    /**
     * Round 1: NONE (independent diagnosis)
     * Round 2: default CHALLENGE; supportive keywords flip to SUPPORT
     * Round 3: REVISE
     * Round 4: NONE (final statement)
     */
    public static String inferEdgeType(int roundNumber, String content) {
        if (roundNumber == 1 || roundNumber == 4) return null;
        String c = content == null ? "" : content;
        if (roundNumber == 2) {
            if (c.contains("同意") || c.contains("赞同") || c.contains("补充") || c.contains("我支持")) {
                return "SUPPORT";
            }
            return "CHALLENGE";
        }
        return "REVISE";
    }
}
