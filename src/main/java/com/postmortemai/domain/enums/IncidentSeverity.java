package com.postmortemai.domain.enums;

/**
 * Severity level of an incident, aligned with the Google SRE model.
 * The value is inferred exclusively by the LLM — never set directly by the user.
 * Persisted as a STRING to avoid ordinal-based compatibility issues.
 */
public enum IncidentSeverity {

    /** P1 — Critical: total service outage or data loss. */
    P1,

    /** P2 — High: major feature degradation affecting most users. */
    P2,

    /** P3 — Medium: partial degradation with a workaround available. */
    P3,

    /** P4 — Low: minor issue with negligible user impact. */
    P4
}
