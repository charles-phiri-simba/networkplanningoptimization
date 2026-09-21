package com.simba.snip.npo.productioncampaign.repository;

import com.simba.snip.npo.productioncampaign.entity.CampaignExecutionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignExecutionItemRepository extends JpaRepository<CampaignExecutionItemEntity, UUID> {

    List<CampaignExecutionItemEntity> findByRevisionIdOrderByItemSequenceAsc(UUID revisionId);

    long countByRevisionId(UUID revisionId);
}
