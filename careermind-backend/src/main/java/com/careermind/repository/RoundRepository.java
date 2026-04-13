package com.careermind.repository;

import com.careermind.domain.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByDiscussionIdOrderByRoundNumberAsc(Long discussionId);
    Optional<Round> findByDiscussionIdAndRoundNumber(Long discussionId, Integer roundNumber);
}
