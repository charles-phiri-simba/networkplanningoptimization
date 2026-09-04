package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import com.simba.snip.npo.targetonboarding.api.TargetOnboardingController;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Phase17OnboardingDurabilityIT extends ProductionChangeITSupport {

    static final String TARGET = "ERICSSON-ENM-PHASE17-ONB";

    @AfterEach
    void cleanupOnboardingGraph() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void createReviewApprovePersistsAndSurvivesReread() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET, "onb-api");
        Map<String, Object> profile = jdbc.queryForMap(
                "SELECT interface_definition_version_id, capability_cert_version_id, security_cert_version_id, "
                        + "network_policy_profile_version_id FROM vendor_write_transport_profile "
                        + "WHERE transport_profile_version_id = ?",
                graph.transportProfileVersionId());

        ResponseEntity<Map> created = http.exchange(
                "/api/v1/target-onboardings",
                HttpMethod.POST,
                entity(createBody(graph, profile), "creator-1", Phase17CertificationPermission.TARGET_ONBOARD_CREATE),
                Map.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertEquals("DRAFT", created.getBody().get("status"));
        UUID id = UUID.fromString((String) created.getBody().get("id"));
        assertEquals("DRAFT", jdbc.queryForObject(
                "SELECT status FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));
        assertEquals("creator-1", jdbc.queryForObject(
                "SELECT created_by FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));
        assertNotNull(jdbc.queryForObject(
                "SELECT onboarding_version_id FROM production_target_onboarding WHERE onboarding_id = ?",
                UUID.class, id));

        ResponseEntity<Map> reviewed = http.exchange(
                "/api/v1/target-onboardings/" + id + "/review",
                HttpMethod.POST,
                entity(Map.of(), "reviewer-1", Phase17CertificationPermission.TARGET_ONBOARD_REVIEW),
                Map.class);
        assertEquals(HttpStatus.OK, reviewed.getStatusCode());
        assertEquals("IN_REVIEW", jdbc.queryForObject(
                "SELECT status FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));
        assertEquals("reviewer-1", jdbc.queryForObject(
                "SELECT reviewed_by FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));

        ResponseEntity<Map> sod = http.exchange(
                "/api/v1/target-onboardings/" + id + "/approve",
                HttpMethod.POST,
                entity(Map.of(), "creator-1", Phase17CertificationPermission.TARGET_ONBOARD_APPROVE),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, sod.getStatusCode());
        assertEquals("P17_SOD_VIOLATION", sod.getBody().get("code"));
        assertEquals("IN_REVIEW", jdbc.queryForObject(
                "SELECT status FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));

        ResponseEntity<Map> approved = http.exchange(
                "/api/v1/target-onboardings/" + id + "/approve",
                HttpMethod.POST,
                entity(Map.of(), "approver-1", Phase17CertificationPermission.TARGET_ONBOARD_APPROVE),
                Map.class);
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertEquals("APPROVED", jdbc.queryForObject(
                "SELECT status FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));
        assertEquals("approver-1", jdbc.queryForObject(
                "SELECT approved_by FROM production_target_onboarding WHERE onboarding_id = ?", String.class, id));
        assertNotEquals("L4", jdbc.queryForObject(
                "SELECT certification_level FROM production_target_onboarding WHERE onboarding_id = ?",
                String.class, id));
    }

    @Test
    void callersCannotInjectEndpointOrCredential() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET, "onb-inject");
        Map<String, Object> profile = jdbc.queryForMap(
                "SELECT interface_definition_version_id, capability_cert_version_id, security_cert_version_id, "
                        + "network_policy_profile_version_id FROM vendor_write_transport_profile "
                        + "WHERE transport_profile_version_id = ?",
                graph.transportProfileVersionId());
        Map<String, Object> endpointBody = createBody(graph, profile);
        endpointBody.put("endpointOverride", "https://evil.example");
        ResponseEntity<Map> endpointDenied = http.exchange(
                "/api/v1/target-onboardings",
                HttpMethod.POST,
                entity(endpointBody, "creator-1", Phase17CertificationPermission.TARGET_ONBOARD_CREATE),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, endpointDenied.getStatusCode());
        Map<String, Object> credentialBody = createBody(graph, profile);
        credentialBody.put("credentialValue", "not-a-real-secret");
        ResponseEntity<Map> credentialDenied = http.exchange(
                "/api/v1/target-onboardings",
                HttpMethod.POST,
                entity(credentialBody, "creator-1", Phase17CertificationPermission.TARGET_ONBOARD_CREATE),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, credentialDenied.getStatusCode());
    }

    @Test
    void callersCannotAssignApprovedOrL4() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET, "onb-deny");
        Map<String, Object> profile = jdbc.queryForMap(
                "SELECT interface_definition_version_id, capability_cert_version_id, security_cert_version_id, "
                        + "network_policy_profile_version_id FROM vendor_write_transport_profile "
                        + "WHERE transport_profile_version_id = ?",
                graph.transportProfileVersionId());
        Map<String, Object> body = createBody(graph, profile);
        body.put("certificationLevel", "L4");
        body.put("status", "APPROVED");
        body.put("health", "HEALTHY");
        ResponseEntity<Map> denied = http.exchange(
                "/api/v1/target-onboardings",
                HttpMethod.POST,
                entity(body, "creator-1", Phase17CertificationPermission.TARGET_ONBOARD_CREATE),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        Integer approved = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_target_onboarding WHERE production_target_id = ? AND status = 'APPROVED' "
                        + "AND created_by = 'creator-1'",
                Integer.class, TARGET);
        assertEquals(1, approved);
    }

    private Map<String, Object> createBody(Phase17CertificationGraphSeeder.Graph graph, Map<String, Object> profile) {
        return new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("productionTargetId", TARGET),
                Map.entry("certificationLevel", "L0"),
                Map.entry("interfaceDefinitionVersionId", profile.get("interface_definition_version_id")),
                Map.entry("transportProfileVersionId", graph.transportProfileVersionId()),
                Map.entry("artifactDigest", Phase17CertificationGraphSeeder.PACKAGED_DIGEST),
                Map.entry("capabilityCertVersionId", profile.get("capability_cert_version_id")),
                Map.entry("securityCertVersionId", profile.get("security_cert_version_id")),
                Map.entry("credentialProfileVersionId", graph.credentialProfileVersionId()),
                Map.entry("tlsProfileVersionId", graph.tlsProfileVersionId()),
                Map.entry("networkPolicyProfileVersionId", profile.get("network_policy_profile_version_id")),
                Map.entry("endpointProfileVersionId", graph.endpointProfileVersionId()),
                Map.entry("bundleVersionId", graph.bundleVersionId()),
                Map.entry("vendorSoftwareVersion", "ENM-22"),
                Map.entry("environment", "LAB")
        ));
    }

    private static HttpEntity<Map<String, Object>> entity(Map<String, Object> body, String actor, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TargetOnboardingController.ACTOR_HEADER, actor);
        headers.add(TargetOnboardingController.PERMISSION_HEADER, permission);
        return new HttpEntity<>(body, headers);
    }
}
