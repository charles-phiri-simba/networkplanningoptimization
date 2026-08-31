package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanReviewRepository extends JpaRepository<NetworkChangePlanReviewEntity, UUID> {

    List<NetworkChangePlanReviewEntity> findByPlanIdOrderByReviewedAtAsc(UUID planId);
}
