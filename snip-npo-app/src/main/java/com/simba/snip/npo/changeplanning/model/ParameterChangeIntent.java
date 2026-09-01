package com.simba.snip.npo.changeplanning.model;

public record ParameterChangeIntent(
        String targetType,
        String targetId,
        String parameter,
        String expectedCurrentValue,
        String desiredValue
) {
}
