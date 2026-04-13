package com.careermind.repository;

import com.careermind.domain.Task;
import com.careermind.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TaskStatus status);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.agents WHERE t.id = :id")
    Task findByIdWithAgents(@Param("id") Long id);
}
