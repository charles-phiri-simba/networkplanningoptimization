package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<SiteEntity, UUID> {

    Optional<SiteEntity> findBySiteId(String siteId);

    List<SiteEntity> findAllByOrderBySiteIdAsc();
}
