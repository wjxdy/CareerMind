package com.careermind.repository;

import com.careermind.domain.Agent;
import com.careermind.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByIsPresetTrue();
    List<Agent> findByUserId(Long userId);

    @Query("SELECT a FROM Agent a WHERE a.isPreset = true OR a.userId = :userId")
    List<Agent> findByIsPresetTrueOrUserId(Long userId);

    List<Agent> findByType(AgentType type);
}
