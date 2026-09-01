package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CellRepository extends JpaRepository<CellEntity, UUID> {

    @EntityGraph(attributePaths = {"gnb", "gnb.site"})
    Optional<CellEntity> findByCellId(String cellId);

    @EntityGraph(attributePaths = {"gnb", "gnb.site"})
    List<CellEntity> findAllByOrderByCellIdAsc();
}
