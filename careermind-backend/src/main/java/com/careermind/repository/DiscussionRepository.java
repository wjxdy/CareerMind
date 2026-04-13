package com.careermind.repository;

import com.careermind.domain.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    Optional<Discussion> findByTaskId(Long taskId);
}
