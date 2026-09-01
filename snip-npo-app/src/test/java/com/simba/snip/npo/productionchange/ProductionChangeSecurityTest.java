package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeSecurityTest {

    @Test
    void productionPermissionSetComplete() {
        Set<String> values = Arrays.stream(ProductionChangePermission.class.getDeclaredFields())
                .filter(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(ProductionChangeSecurityTest::stringValue)
                .collect(Collectors.toSet());
        assertTrue(values.contains(ProductionChangePermission.VIEW_PRODUCTION_CHANGE));
        assertTrue(values.contains(ProductionChangePermission.REQUEST_PRODUCTION_CHANGE));
        assertTrue(values.contains(ProductionChangePermission.REVIEW_PRODUCTION_CHANGE));
        assertTrue(values.contains(ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE));
        assertTrue(values.contains(ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE));
        assertTrue(values.contains(ProductionChangePermission.REQUEST_PRODUCTION_ROLLBACK));
        assertTrue(values.contains(ProductionChangePermission.REVIEW_PRODUCTION_ROLLBACK));
        assertTrue(values.contains(ProductionChangePermission.AUTHORIZE_PRODUCTION_ROLLBACK));
        assertTrue(values.contains(ProductionChangePermission.EXECUTE_PRODUCTION_ROLLBACK));
        assertTrue(values.contains(ProductionChangePermission.ADMINISTER_PRODUCTION_TARGET));
    }

    private static String stringValue(Field field) {
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            return value == null ? "" : value.toString();
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
