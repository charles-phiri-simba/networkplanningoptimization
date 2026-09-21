package com.simba.snip.npo.productioncampaign.repository;

import com.simba.snip.npo.productioncampaign.entity.CampaignRevisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CampaignRevisionRepository extends JpaRepository<CampaignRevisionEntity, UUID> {

    Optional<CampaignRevisionEntity> findByCampaignIdAndRevisionNumber(UUID campaignId, int revisionNumber);
}
