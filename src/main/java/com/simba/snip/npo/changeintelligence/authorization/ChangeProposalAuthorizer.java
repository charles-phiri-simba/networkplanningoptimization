package com.simba.snip.npo.changeintelligence.authorization;

import com.simba.snip.npo.changeintelligence.ChangeProposalException;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ChangeProposalAuthorizer {

    public static final String PERMISSION_VIEW = "VIEW_NETWORK_CHANGE_PROPOSALS";
    public static final String PERMISSION_GENERATE = "GENERATE_NETWORK_CHANGE_PROPOSAL";
    public static final String PERMISSION_REVIEW = "REVIEW_NETWORK_CHANGE_PROPOSAL";
    public static final String PERMISSION_APPROVE = "APPROVE_NETWORK_CHANGE_PROPOSAL";
    public static final String PERMISSION_REJECT = "REJECT_NETWORK_CHANGE_PROPOSAL";
    public static final String HEADER = "X-SNIP-CHANGE-PROPOSAL-PERMISSION";
    private static final String ATTR = "snip.changeProposalPermission";

    private final ThreadLocal<String> override = new ThreadLocal<>();

    public void bindRequestPermission(String permission) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTR, permission, RequestAttributes.SCOPE_REQUEST);
        } else {
            override.set(permission);
        }
    }

    public String current() {
        String local = override.get();
        if (local != null) {
            return local;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    public void requireView() {
        require(PERMISSION_VIEW, ChangeProposalFailureCode.PROPOSAL_REVIEW_FORBIDDEN);
    }

    public void requireGenerate() {
        require(PERMISSION_GENERATE, ChangeProposalFailureCode.PROPOSAL_GENERATION_FORBIDDEN);
    }

    public void requireReview() {
        require(PERMISSION_REVIEW, ChangeProposalFailureCode.PROPOSAL_REVIEW_FORBIDDEN);
    }

    public void requireApprove() {
        require(PERMISSION_APPROVE, ChangeProposalFailureCode.PROPOSAL_APPROVAL_FORBIDDEN);
    }

    public void requireReject() {
        require(PERMISSION_REJECT, ChangeProposalFailureCode.PROPOSAL_REJECTION_FORBIDDEN);
    }

    public void runWith(String permission, Runnable action) {
        String previous = override.get();
        override.set(permission);
        try {
            action.run();
        } finally {
            if (previous == null) {
                override.remove();
            } else {
                override.set(previous);
            }
        }
    }

    private void require(String permission, ChangeProposalFailureCode failureCode) {
        if (!permission.equals(current())) {
            throw new ChangeProposalException(failureCode, permission + " is required");
        }
    }
}
