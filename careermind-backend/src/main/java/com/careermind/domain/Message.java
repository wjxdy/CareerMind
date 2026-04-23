package com.careermind.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id", nullable = true)
    private Agent agent;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;  // 回复哪条消息

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    @Builder.Default
    private com.careermind.enums.MessageType messageType = com.careermind.enums.MessageType.AGENT;

    @Column(name = "is_final")
    @Builder.Default
    private Boolean isFinal = false;  // 是否是最终观点

    @Column(name = "edge_type", length = 16)
    private String edgeType;  // SUPPORT/CHALLENGE/REVISE; null = independent

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;  // 0.00-1.00

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
