package com.careermind.repository;

import com.careermind.domain.MergeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MergeResultRepository extends JpaRepository<MergeResult, Long> {
    Optional<MergeResult> findByTaskId(Long taskId);
}
