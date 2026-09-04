package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.service.TransportCertificationEvidenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Phase17EvidenceSupersessionIT extends ProductionChangeITSupport {

    @Autowired
    private TransportCertificationEvidenceService evidenceService;

    @AfterEach
    void cleanup() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void t17Impl015016017HashOnlyAndSupersession() {
        Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "evid");
        UUID subject = UUID.randomUUID();
        UUID subjectVersion = UUID.randomUUID();
        assertEquals(Phase17DenialCode.P17_BUNDLE_INVALID,
                assertThrows(Phase17Exception.class, () -> evidenceService.add(
                        subjectVersion, subject, "issuer-1", "TRANSPORT_CERTIFY",
                        "LAB_PROOF", "PASS", null, "ref")).denialCode());
        UUID first = evidenceService.add(
                subjectVersion, subject, "issuer-1", "TRANSPORT_CERTIFY",
                "RECERT", "PASS", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "ref-1");
        UUID second = evidenceService.add(
                subjectVersion, subject, "issuer-2", "TRANSPORT_CERTIFY",
                "RECERT2", "FAIL", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "ref-2");
        evidenceService.supersede(first, second, "issuer-2");
        assertEquals("SUPERSEDED", jdbc.queryForObject(
                "SELECT status FROM transport_certification_evidence WHERE evidence_id = ?",
                String.class, first));
        assertEquals("FAIL", jdbc.queryForObject(
                "SELECT result FROM transport_certification_evidence WHERE evidence_id = ?",
                String.class, second));
    }
}
