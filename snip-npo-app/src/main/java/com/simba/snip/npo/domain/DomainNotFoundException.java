package com.simba.snip.npo.domain;

public class DomainNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public DomainNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " not found: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
