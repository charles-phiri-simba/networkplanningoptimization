package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RadioConfigurationRepository extends JpaRepository<RadioConfigurationEntity, UUID> {

    List<RadioConfigurationEntity> findByCell_IdOrderByParameterNameAsc(UUID cellId);

    java.util.Optional<RadioConfigurationEntity> findByCell_IdAndParameterName(UUID cellId, String parameterName);
}
