package com.simba.snip.npo.integration.ericsson.enm;

import java.util.List;

public record EnmInventoryPage(
        String pageIdentity,
        String continuationToken,
        boolean lastPage,
        List<EnmManagedElement> managedElements,
        List<EnmRadioFunction> radioFunctions,
        List<EnmCell> cells
) {
    public EnmInventoryPage {
        managedElements = managedElements == null ? List.of() : List.copyOf(managedElements);
        radioFunctions = radioFunctions == null ? List.of() : List.copyOf(radioFunctions);
        cells = cells == null ? List.of() : List.copyOf(cells);
    }

    public int entityCount() {
        return managedElements.size() + radioFunctions.size() + cells.size();
    }
}
