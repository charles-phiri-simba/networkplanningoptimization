package com.simba.snip.npo.productioncampaign.config;

import com.simba.snip.npo.productioncampaign.domain.CampaignConstants;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.production-campaign")
public class ProductionCampaignProperties {

    private boolean trustedIngressGuarantee;
    private List<String> trustedAuthenticationSources = new ArrayList<>();
    private int configuredPolicyMaximum = CampaignConstants.HARD_MAX_CAMPAIGN_ITEMS;
    private boolean automaticProgressionEnabled;
    private boolean automaticResumeEnabled;
    private boolean automaticRollbackEnabled;
    private boolean automaticCohortReleaseEnabled;

    public boolean isTrustedIngressGuarantee() {
        return trustedIngressGuarantee;
    }

    public void setTrustedIngressGuarantee(boolean trustedIngressGuarantee) {
        this.trustedIngressGuarantee = trustedIngressGuarantee;
    }

    public List<String> getTrustedAuthenticationSources() {
        return trustedAuthenticationSources;
    }

    public void setTrustedAuthenticationSources(List<String> trustedAuthenticationSources) {
        this.trustedAuthenticationSources = trustedAuthenticationSources == null
                ? new ArrayList<>()
                : new ArrayList<>(trustedAuthenticationSources);
    }

    public int getConfiguredPolicyMaximum() {
        return configuredPolicyMaximum;
    }

    public void setConfiguredPolicyMaximum(int configuredPolicyMaximum) {
        this.configuredPolicyMaximum = configuredPolicyMaximum;
    }

    public boolean isAutomaticProgressionEnabled() {
        return automaticProgressionEnabled;
    }

    public void setAutomaticProgressionEnabled(boolean automaticProgressionEnabled) {
        this.automaticProgressionEnabled = automaticProgressionEnabled;
    }

    public boolean isAutomaticResumeEnabled() {
        return automaticResumeEnabled;
    }

    public void setAutomaticResumeEnabled(boolean automaticResumeEnabled) {
        this.automaticResumeEnabled = automaticResumeEnabled;
    }

    public boolean isAutomaticRollbackEnabled() {
        return automaticRollbackEnabled;
    }

    public void setAutomaticRollbackEnabled(boolean automaticRollbackEnabled) {
        this.automaticRollbackEnabled = automaticRollbackEnabled;
    }

    public boolean isAutomaticCohortReleaseEnabled() {
        return automaticCohortReleaseEnabled;
    }

    public void setAutomaticCohortReleaseEnabled(boolean automaticCohortReleaseEnabled) {
        this.automaticCohortReleaseEnabled = automaticCohortReleaseEnabled;
    }

    public int effectiveMaximumItems() {
        if (configuredPolicyMaximum <= 0) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SIZE_LIMIT,
                    "configured campaign item maximum is invalid and fails closed"
            );
        }
        return Math.min(CampaignConstants.HARD_MAX_CAMPAIGN_ITEMS, configuredPolicyMaximum);
    }

    public void validate() {
        if (automaticProgressionEnabled || automaticResumeEnabled
                || automaticRollbackEnabled || automaticCohortReleaseEnabled) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "automatic campaign progression/release/resume/rollback is not authorized"
            );
        }
        if (configuredPolicyMaximum > CampaignConstants.HARD_MAX_CAMPAIGN_ITEMS) {
            throw new CampaignException(
                    ProductionReasonCode.CAMPAIGN_SIZE_LIMIT,
                    "configuration cannot raise HARD_MAX_CAMPAIGN_ITEMS"
            );
        }
        effectiveMaximumItems();
    }
}
