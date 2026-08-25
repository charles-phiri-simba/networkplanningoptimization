package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {

    List<AgentRunEntity> findAllByOrderByStartedAtDesc();
}
