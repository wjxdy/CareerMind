package com.careermind.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "merge_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnore
    private Task task;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;  // 整体总结

    @OneToMany(mappedBy = "mergeResult", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Plan> plans;

    @Column(name = "blind_spots", columnDefinition = "TEXT")
    private String blindSpots;  // JSON 格式存储认知盲区

    @Column(name = "convergence_rate")
    private Double convergenceRate;  // 观点收敛度

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
