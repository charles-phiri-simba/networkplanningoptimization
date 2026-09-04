package com.simba.snip.npo.vendorcertification;

import org.springframework.jdbc.core.JdbcTemplate;

final class Phase17GraphCleanup {

    private Phase17GraphCleanup() {
    }

    static void deleteAll(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM phase17_invalidation_outbox");
        jdbc.update("DELETE FROM phase17_invalidation_event");
        jdbc.update("DELETE FROM phase17_certification_audit_event");
        jdbc.update("DELETE FROM vendor_transport_health_event");
        jdbc.update("DELETE FROM vendor_transport_health");
        jdbc.update("DELETE FROM vendor_version_compatibility");
        jdbc.update("UPDATE transport_certification_evidence SET superseded_by = NULL");
        jdbc.update("DELETE FROM transport_certification_evidence");
        jdbc.update("DELETE FROM production_target_certification");
        jdbc.update("UPDATE production_target_onboarding SET onboarding_version_id = NULL");
        jdbc.update("DELETE FROM production_target_onboarding_version");
        jdbc.update("DELETE FROM production_target_onboarding");
        jdbc.update("UPDATE transport_certification SET current_version_id = NULL");
        jdbc.update("UPDATE transport_certification_version SET bundle_version_id = NULL");
        jdbc.update("DELETE FROM transport_certification_version");
        jdbc.update("DELETE FROM transport_certification_bundle");
        jdbc.update("DELETE FROM transport_certification");
        jdbc.update("UPDATE vendor_write_transport_profile "
                + "SET status = 'DRAFT', certification_state = 'DRAFT', capability_cert_version_id = NULL");
        jdbc.update("UPDATE vendor_interface_definition SET capability_cert_version_id = NULL");
        jdbc.update("DELETE FROM vendor_capability_certification");
        jdbc.update("DELETE FROM vendor_write_transport_profile");
        jdbc.update("DELETE FROM vendor_interface_approval");
        jdbc.update("DELETE FROM vendor_interface_definition");
        jdbc.update("DELETE FROM vendor_security_certification");
        jdbc.update("DELETE FROM production_endpoint_profile");
        jdbc.update("DELETE FROM production_credential_profile");
        jdbc.update("DELETE FROM production_network_policy_profile");
        jdbc.update("DELETE FROM production_tls_profile");
        jdbc.update("DELETE FROM vendor_transport_artifact");
        jdbc.update("DELETE FROM vendor_abstract_protocol_placeholder");
    }
}
