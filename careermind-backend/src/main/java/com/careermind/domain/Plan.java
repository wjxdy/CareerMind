package com.careermind.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merge_result_id", nullable = false)
    @JsonIgnore
    private MergeResult mergeResult;

    @Column(nullable = false)
    private String title;  // 方案标题，如"激进冲刺型"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "confidence")
    private Integer confidence;  // 置信度 0-100

    @Column(name = "supporters", columnDefinition = "TEXT")
    private String supporters;  // JSON 数组，支持者 Agent IDs

    @Column(name = "opponents", columnDefinition = "TEXT")
    private String opponents;  // JSON 数组，反对者 Agent IDs

    @Column(name = "milestones", columnDefinition = "TEXT")
    private String milestones;  // JSON 格式，关键里程碑

    @Column(name = "risks", columnDefinition = "TEXT")
    private String risks;  // JSON 数组，风险提示

    @Column(name = "applicable_conditions", columnDefinition = "TEXT")
    private String applicableConditions;  // 适用条件

    @Column(name = "is_selected")
    @Builder.Default
    private Boolean isSelected = false;  // 用户是否选择了这个方案

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
