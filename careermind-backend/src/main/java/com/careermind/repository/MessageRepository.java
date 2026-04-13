package com.careermind.repository;

import com.careermind.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoundIdOrderByCreatedAtAsc(Long roundId);
    List<Message> findByRoundIdAndAgentId(Long roundId, Long agentId);
}
