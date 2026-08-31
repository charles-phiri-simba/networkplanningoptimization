package com.simba.snip.npo.changeexecution.api;

import java.util.UUID;

public record CreateExecutionRequest(UUID planId, String executionTargetId) {
}
