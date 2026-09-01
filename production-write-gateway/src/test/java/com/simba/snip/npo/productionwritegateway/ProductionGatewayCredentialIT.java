package com.simba.snip.npo.productionwritegateway;

import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionwritegateway.security.TestOnlyProductionCredentialResolver;
import com.simba.snip.npo.productionwritegateway.security.GatewayCallerAuthenticator;
import com.simba.snip.npo.productionwritegateway.service.GatewayExecutionOrchestrator;
import com.simba.snip.npo.productionwritegateway.service.ProductionCredentialResolutionService;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductionGatewayCredentialIT extends AbstractGatewayPostgresIT {

    @Autowired
    private GatewayExecutionOrchestrator orchestrator;

    @Autowired
    private ProductionCredentialResolutionService credentialService;

    @Autowired
    private TestOnlyProductionCredentialResolver fakeResolver;

    @Autowired
    private ControlledTestEricssonWriteTransport transport;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AtomicInteger mutationInvocationCounter;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() {
        credentialService.resetCredentialResolutionCount();
        fakeResolver.reset();
        transport.reset();
        mutationInvocationCounter.set(0);
    }

    @Test
    void credentialAfterConsumeAndPreflight() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        transport.seedCell(seeded.cellId(), seeded.expectedValue());
        int before = credentialService.getCredentialResolutionCount();
        orchestrator.execute(
                new GatewayExecuteRequest(seeded.grantId(), seeded.productionChangeId(), "corr-1"),
                "snip-npo-app",
                null,
                GrantType.FORWARD
        );
        assertTrue(credentialService.getCredentialResolutionCount() > before);
        assertEquals("CONSUMED", jdbcTemplate.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?",
                String.class,
                seeded.grantId()
        ));
    }

    @Test
    void noCredentialCallWhenConsumeDenied() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        int before = credentialService.getCredentialResolutionCount();
        assertThrows(RuntimeException.class, () -> orchestrator.execute(
                new GatewayExecuteRequest(seeded.grantId(), UUID_MISMATCH(), "corr-2"),
                "snip-npo-app",
                null,
                GrantType.FORWARD
        ));
        assertEquals(before, credentialService.getCredentialResolutionCount());
        assertEquals(0, mutationInvocationCounter.get());
    }

    @Test
    void credentialFailureZeroMutation() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        transport.seedCell(seeded.cellId(), seeded.expectedValue());
        fakeResolver.failNext();
        assertThrows(RuntimeException.class, () -> orchestrator.execute(
                new GatewayExecuteRequest(seeded.grantId(), seeded.productionChangeId(), "corr-3"),
                "snip-npo-app",
                null,
                GrantType.FORWARD
        ));
        assertEquals(0, mutationInvocationCounter.get());
    }

    @Test
    void refuseOldVersionFallback() {
        assertThrows(RuntimeException.class, () -> fakeResolver.resolveVersion("ericsson-enm-lab-write", "v1-old"));
        assertEquals(0, mutationInvocationCounter.get());
    }

    @Test
    void refuseReadProfileSubstitution() {
        assertThrows(RuntimeException.class, () -> fakeResolver.resolveLatest("ericsson-enm-int-inventory-reader"));
        assertEquals(0, mutationInvocationCounter.get());
    }

    @Test
    void executeRejectsMutationPayloadOverrides() throws Exception {
        mockMvc.perform(post("/internal/v1/gateway/execute")
                        .header(GatewayCallerAuthenticator.CALLER_HEADER, "snip-npo-app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grantId":"00000000-0000-0000-0000-000000000001",
                                 "productionChangeId":"00000000-0000-0000-0000-000000000002",
                                 "correlationId":"c",
                                 "cellId":"attacker-cell",
                                 "parameter":"txPower",
                                 "desiredValue":99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reasonCode").value("PRODUCTION_INVALID_REQUEST"));
        assertEquals(0, mutationInvocationCounter.get());
    }

    @Test
    void jwtAloneIsRejected() throws Exception {
        mockMvc.perform(post("/internal/v1/gateway/execute")
                        .header("Authorization", "Bearer not-an-authority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grantId":"00000000-0000-0000-0000-000000000001",
                                 "productionChangeId":"00000000-0000-0000-0000-000000000002",
                                 "correlationId":"c"}
                                """))
                .andExpect(status().isForbidden());
        assertEquals(0, mutationInvocationCounter.get());
    }

    private static java.util.UUID UUID_MISMATCH() {
        return java.util.UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    }
}
