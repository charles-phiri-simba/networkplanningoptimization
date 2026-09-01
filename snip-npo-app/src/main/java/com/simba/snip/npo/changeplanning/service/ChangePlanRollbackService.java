package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.OperationType;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChangePlanRollbackService {

    private final ChangePlanningProperties properties;

    public ChangePlanRollbackService(ChangePlanningProperties properties) {
        this.properties = properties;
    }

    public NetworkChangePlanRollbackOperationEntity buildRollback(
            UUID planId,
            ParameterChangeIntent forwardIntent,
            Instant createdAt
    ) {
        if (!properties.isRequireRollback()) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_ROLLBACK_UNAVAILABLE, "rollback not required");
        }
        return NetworkChangePlanRollbackOperationEntity.create(
                UUID.randomUUID(),
                planId,
                1,
                OperationType.SET_PARAMETER.name(),
                forwardIntent.targetType(),
                forwardIntent.targetId(),
                forwardIntent.parameter(),
                forwardIntent.desiredValue(),
                forwardIntent.expectedCurrentValue(),
                createdAt
        );
    }

    public boolean validateRollback(
            NetworkChangePlanOperationEntity forward,
            NetworkChangePlanRollbackOperationEntity rollback
    ) {
        if (rollback == null) {
            return !properties.isRequireRollback();
        }
        return forward.getTargetEntityId().equals(rollback.getTargetEntityId())
                && forward.getParameterName().equals(rollback.getParameterName())
                && forward.getDesiredValue().equals(rollback.getExpectedCurrentValue())
                && forward.getExpectedCurrentValue().equals(rollback.getDesiredValue());
    }
}
