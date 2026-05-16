package com.postmortemai.application.port;

import com.postmortemai.domain.model.Incident;

import java.util.Optional;

/**
 * Output Port for Incident persistence.
 */
public interface IncidentRepositoryPort {
    Incident save(Incident incident);
    Optional<Incident> findByRawLogHash(String rawLogHash);
}
