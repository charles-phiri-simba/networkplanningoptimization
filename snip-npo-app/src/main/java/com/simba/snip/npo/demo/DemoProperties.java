package com.simba.snip.npo.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.demo")
public class DemoProperties {

    private boolean enabled = false;
    private boolean bootstrapAssurance = true;
    private boolean synchronizeFeaturedTwins = true;
    private boolean triggerSimulatorKnowledgeBaseline = true;
    private List<String> featuredAssuranceCells = new ArrayList<>(List.of("CELL-001", "CELL-007", "CELL-010", "CELL-014"));
    private List<String> featuredTwinCells = new ArrayList<>(List.of("CELL-001", "CELL-002", "CELL-003", "CELL-007"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBootstrapAssurance() {
        return bootstrapAssurance;
    }

    public void setBootstrapAssurance(boolean bootstrapAssurance) {
        this.bootstrapAssurance = bootstrapAssurance;
    }

    public boolean isSynchronizeFeaturedTwins() {
        return synchronizeFeaturedTwins;
    }

    public void setSynchronizeFeaturedTwins(boolean synchronizeFeaturedTwins) {
        this.synchronizeFeaturedTwins = synchronizeFeaturedTwins;
    }

    public boolean isTriggerSimulatorKnowledgeBaseline() {
        return triggerSimulatorKnowledgeBaseline;
    }

    public void setTriggerSimulatorKnowledgeBaseline(boolean triggerSimulatorKnowledgeBaseline) {
        this.triggerSimulatorKnowledgeBaseline = triggerSimulatorKnowledgeBaseline;
    }

    public List<String> getFeaturedAssuranceCells() {
        return featuredAssuranceCells;
    }

    public void setFeaturedAssuranceCells(List<String> featuredAssuranceCells) {
        this.featuredAssuranceCells = featuredAssuranceCells;
    }

    public List<String> getFeaturedTwinCells() {
        return featuredTwinCells;
    }

    public void setFeaturedTwinCells(List<String> featuredTwinCells) {
        this.featuredTwinCells = featuredTwinCells;
    }
}
