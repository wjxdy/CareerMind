package com.careermind.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MessageMetaParserTest {

    @Test
    void parsesConfidence() {
        assertEquals(new BigDecimal("0.82"), MessageMetaParser.parseConfidence("ok\n[confidence: 0.82]").orElseThrow());
        assertEquals(new BigDecimal("0.5"),  MessageMetaParser.parseConfidence("[confidence: 0.5]").orElseThrow());
        assertEquals(new BigDecimal("0.7"),  MessageMetaParser.parseConfidence("text [Confidence:0.7] more").orElseThrow());
    }

    @Test
    void emptyOnMissing() {
        assertTrue(MessageMetaParser.parseConfidence("no tag").isEmpty());
        assertTrue(MessageMetaParser.parseConfidence(null).isEmpty());
        assertTrue(MessageMetaParser.parseConfidence("").isEmpty());
    }

    @Test
    void clampsOutOfRange() {
        assertEquals(0, BigDecimal.ONE.compareTo(MessageMetaParser.parseConfidence("[confidence: 1.5]").orElseThrow()));
        assertEquals(0, BigDecimal.ZERO.compareTo(MessageMetaParser.parseConfidence("[confidence: 0]").orElseThrow()));
    }

    @Test
    void stripsTag() {
        assertEquals("hello", MessageMetaParser.stripConfidence("hello\n[confidence: 0.7]"));
        assertNull(MessageMetaParser.stripConfidence(null));
        assertEquals("a b", MessageMetaParser.stripConfidence("a b [confidence: 0.5]"));
    }

    @Test
    void infersEdgeType() {
        assertNull(MessageMetaParser.inferEdgeType(1, "..."));
        assertEquals("CHALLENGE", MessageMetaParser.inferEdgeType(2, "我反对"));
        assertEquals("SUPPORT",   MessageMetaParser.inferEdgeType(2, "我同意X的观点"));
        assertEquals("SUPPORT",   MessageMetaParser.inferEdgeType(2, "我赞同 @行业分析师"));
        assertEquals("REVISE",    MessageMetaParser.inferEdgeType(3, "..."));
        assertNull(MessageMetaParser.inferEdgeType(4, "..."));
    }
}
