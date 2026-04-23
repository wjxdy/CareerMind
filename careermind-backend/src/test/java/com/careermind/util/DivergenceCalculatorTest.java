package com.careermind.util;

import com.careermind.domain.Message;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivergenceCalculatorTest {

    private Message msg(double conf, String type) {
        Message m = new Message();
        m.setConfidence(BigDecimal.valueOf(conf));
        m.setEdgeType(type);
        return m;
    }

    @Test
    void zeroOnEmpty() {
        assertEquals(0, BigDecimal.ZERO.compareTo(DivergenceCalculator.compute(List.of())));
        assertEquals(0, BigDecimal.ZERO.compareTo(DivergenceCalculator.compute(null)));
    }

    @Test
    void higherWithMoreChallengesAndVariance() {
        BigDecimal low = DivergenceCalculator.compute(List.of(
            msg(0.8, null), msg(0.8, null), msg(0.8, null), msg(0.8, null)
        ));
        BigDecimal high = DivergenceCalculator.compute(List.of(
            msg(0.4, "CHALLENGE"), msg(0.5, "CHALLENGE"), msg(0.9, "CHALLENGE"), msg(0.3, "CHALLENGE")
        ));
        assertTrue(high.compareTo(low) > 0,
            () -> "expected high (" + high + ") > low (" + low + ")");
    }

    @Test
    void boundsRespected() {
        BigDecimal v = DivergenceCalculator.compute(List.of(
            msg(0.0, "CHALLENGE"), msg(1.0, "CHALLENGE")
        ));
        assertTrue(v.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(v.compareTo(BigDecimal.ONE)  <= 0);
    }
}
