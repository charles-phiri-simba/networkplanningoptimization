package com.simba.snip.npo.productioncampaign.repository;

import com.simba.snip.npo.productioncampaign.entity.CampaignCohortEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignCohortRepository extends JpaRepository<CampaignCohortEntity, UUID> {

    List<CampaignCohortEntity> findByRevisionIdOrderByCohortSequenceAsc(UUID revisionId);
}
