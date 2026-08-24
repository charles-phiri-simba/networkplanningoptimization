package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GnbRepository extends JpaRepository<GnbEntity, UUID> {

    @EntityGraph(attributePaths = "site")
    Optional<GnbEntity> findByGnbId(String gnbId);

    @EntityGraph(attributePaths = "site")
    List<GnbEntity> findAllByOrderByGnbIdAsc();
}
