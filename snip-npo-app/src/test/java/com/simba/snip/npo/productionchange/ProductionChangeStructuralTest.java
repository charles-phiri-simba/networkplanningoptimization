package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.AuthorizedParameterMutation;
import com.simba.snip.npo.productionwritegateway.adapter.EricssonEnmWriteAdapter;
import com.simba.snip.npo.productionwritegateway.adapter.VendorNetworkWriteAdapter;
import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeStructuralTest {

    @Test
    void writeSideSpiInterfacesPresent() {
        assertTrue(VendorNetworkWriteAdapter.class.getPackageName()
                .startsWith("com.simba.snip.npo.productionwritegateway"));
        assertTrue(EricssonEnmWriteAdapter.class.getPackageName()
                .startsWith("com.simba.snip.npo.productionwritegateway"));
        assertTrue(EricssonWriteTransport.class.getPackageName()
                .startsWith("com.simba.snip.npo.productionwritegateway"));
    }

    @Test
    void noGenericCommandMethod() {
        for (Class<?> type : new Class<?>[] {VendorNetworkWriteAdapter.class, EricssonWriteTransport.class}) {
            for (Method method : type.getMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                assertTrue(!name.contains("executeraw")
                                && !name.contains("genericcommand")
                                && !name.equals("executecommand")
                                && !name.equals("sendraw"),
                        type.getSimpleName() + " exposes generic command method " + method.getName());
            }
        }
        assertThrows(IllegalArgumentException.class, () ->
                new AuthorizedParameterMutation("CELL", "electricalTilt", "CELL-001", BigDecimal.ONE, BigDecimal.TEN));
        assertTrue(Arrays.stream(VendorNetworkWriteAdapter.class.getMethods())
                .anyMatch(m -> m.getName().equals("applyAuthorizedMutation")));
    }
}
