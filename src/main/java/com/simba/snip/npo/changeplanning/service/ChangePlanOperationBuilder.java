package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.OperationType;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanOperationBuilder {

    private final ChangePlanningProperties properties;

    public ChangePlanOperationBuilder(ChangePlanningProperties properties) {
        this.properties = properties;
    }

    public NetworkChangePlanOperationEntity buildForwardOperation(
            UUID planId,
            ParameterChangeIntent intent,
            Instant createdAt
    ) {
        if (!SimulatableParameterRegistry.TX_POWER.equals(intent.parameter())) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_PARAMETER_UNSUPPORTED, intent.parameter());
        }
        return NetworkChangePlanOperationEntity.create(
                UUID.randomUUID(),
                planId,
                1,
                OperationType.SET_PARAMETER.name(),
                intent.targetType(),
                intent.targetId(),
                intent.parameter(),
                intent.expectedCurrentValue(),
                intent.desiredValue(),
                createdAt
        );
    }

    public void enforceOperationCount(List<NetworkChangePlanOperationEntity> operations) {
        if (operations.size() > properties.getMaximumOperationCount()) {
            throw new ChangePlanException(
                    ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID,
                    "maximum operation count exceeded"
            );
        }
    }
}
