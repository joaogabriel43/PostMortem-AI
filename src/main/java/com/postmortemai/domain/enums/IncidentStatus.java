package com.postmortemai.domain.enums;

/**
 * Lifecycle status of an incident.
 * Persisted as a STRING to avoid ordinal-based compatibility issues.
 */
public enum IncidentStatus {

    /** The incident is actively being investigated. */
    INVESTIGATING,

    /** A fix has been applied and the system is under observation. */
    MONITORING,

    /** The incident has been fully resolved. */
    RESOLVED
}
