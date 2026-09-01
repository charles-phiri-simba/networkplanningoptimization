package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.integration.enm.EnmTransport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EnmTransportReadOnlyContractTest {

    private static final Set<String> FORBIDDEN = Set.of(
            "setparameter", "applychange", "writecell", "executecommand",
            "mutate", "write", "apply", "settxpower", "sendmutation"
    );

    @Test
    void noWriteMethods() {
        Method[] methods = EnmTransport.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(FORBIDDEN.stream().anyMatch(name::contains)
                            && !name.startsWith("fetch")
                            && !name.equals("open")
                            && !name.equals("close")
                            && !name.equals("lastretryafter")
                            && !name.equals("supportsincremental"),
                    "write-like method on EnmTransport: " + method.getName());
            assertFalse(name.equals("setparameter"));
            assertFalse(name.equals("applychange"));
            assertFalse(name.equals("writecell"));
            assertFalse(name.equals("executecommand"));
        }
        assertEquals("com.simba.snip.npo.integration.enm.EnmTransport", EnmTransport.class.getName());
    }
}
