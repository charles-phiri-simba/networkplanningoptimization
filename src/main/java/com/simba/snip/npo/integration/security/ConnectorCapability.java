package com.simba.snip.npo.integration.security;

public enum ConnectorCapability {
    READ_SITE,
    READ_GNB,
    READ_CELL,
    READ_CONFIGURATION,
    READ_NEIGHBOURS,
    INVENTORY_READ,
    CONFIGURATION_READ,
    PAGINATION,
    FULL_SYNCHRONIZATION,
    INCREMENTAL_SYNCHRONIZATION,
    DURABLE_CHECKPOINT,
    RESUMABLE_CHECKPOINT,
    EXPLICIT_REMOVE_EVENT,
    SOURCE_VERSION,
    WRITE_CONFIGURATION,
    ACTIVATE,
    DEACTIVATE,
    LOCK,
    UNLOCK,
    RESET,
    EXECUTE_COMMAND,
    DELETE,
    NETWORK_MUTATION,
    PARAMETER_CHANGE;

    public boolean mutatesNetwork() {
        return switch (this) {
            case WRITE_CONFIGURATION, ACTIVATE, DEACTIVATE, LOCK, UNLOCK, RESET, EXECUTE_COMMAND, DELETE,
                    NETWORK_MUTATION, PARAMETER_CHANGE -> true;
            default -> false;
        };
    }
}
