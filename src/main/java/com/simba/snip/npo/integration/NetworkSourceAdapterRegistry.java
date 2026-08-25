package com.simba.snip.npo.integration;

import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NetworkSourceAdapterRegistry {

    private final Map<Vendor, NetworkSourceAdapter> adapters = new EnumMap<>(Vendor.class);

    public NetworkSourceAdapterRegistry(List<NetworkSourceAdapter> adapters) {
        for (NetworkSourceAdapter adapter : adapters) {
            this.adapters.put(adapter.vendor(), adapter);
        }
    }

    public NetworkSourceAdapter require(Vendor vendor) {
        NetworkSourceAdapter adapter = adapters.get(vendor);
        if (adapter == null) {
            throw new DomainValidationException("unsupported vendor: " + vendor);
        }
        return adapter;
    }
}
