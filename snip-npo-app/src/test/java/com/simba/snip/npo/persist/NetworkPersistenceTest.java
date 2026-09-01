package com.simba.snip.npo.persist;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class NetworkPersistenceTest extends AbstractPostgresIT {

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private GnbRepository gnbRepository;

    @Autowired
    private NeighbourRelationshipRepository neighbourRelationshipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySeedLoadsCanonicalTopology() {
        assertTrue(siteRepository.findBySiteId("SITE-001").isPresent());
        assertTrue(gnbRepository.findByGnbId("GNB-001").isPresent());
        CellEntity cell = cellRepository.findByCellId("CELL-001").orElseThrow();
        assertTrue(cell.getGnb().getGnbId().equals("GNB-001"));
        assertTrue(cell.getGnb().getSite().getSiteId().equals("SITE-001"));
        assertTrue(neighbourRelationshipRepository.findBySourceCell_IdOrderByTargetCell_CellIdAsc(cell.getId()).size() >= 1);
    }

    @Test
    void neighbourCannotPointAtSelf() {
        UUID cellPk = cellRepository.findByCellId("CELL-001").orElseThrow().getId();
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO neighbour_relationship (id, source_cell_id, target_cell_id, relation_type, status) "
                                + "VALUES (?, ?, ?, 'INTRA_FREQUENCY', 'ACTIVE')",
                        UUID.randomUUID(), cellPk, cellPk
                ));
    }

    @Test
    void domainIdentifiersAreUnique() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO cell (id, cell_id, name, gnb_id, technology, band, status) "
                                + "VALUES (?, 'CELL-001', 'dup', ?, 'NR', 'n78', 'ACTIVE')",
                        UUID.randomUUID(),
                        cellRepository.findByCellId("CELL-001").orElseThrow().getGnb().getId()
                ));
    }

    @Test
    void cellMustReferenceExistingGnb() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO cell (id, cell_id, name, gnb_id, technology, band, status) "
                                + "VALUES (?, 'CELL-MISSING-GNB', 'orphan', ?, 'NR', 'n78', 'ACTIVE')",
                        UUID.randomUUID(),
                        UUID.fromString("00000000-0000-4000-a000-999999999999")
                ));
    }
}
