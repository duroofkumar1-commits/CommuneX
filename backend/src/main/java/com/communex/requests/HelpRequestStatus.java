package com.communex.requests;

/** Statuses used by the assistance workflow. Transitions are enforced by HelpRequestService. */
public enum HelpRequestStatus {
    OPEN,
    ACKNOWLEDGED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    RESOLVED,
    COMPLETED,
    CLOSED,
    CANCELLED
}
