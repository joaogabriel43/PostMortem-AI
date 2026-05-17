package com.postmortemai.application.port;

import com.postmortemai.domain.model.Incident;

import java.util.Optional;
import java.util.UUID;

import com.postmortemai.application.dto.PageQuery;
import com.postmortemai.application.dto.PageResult;
import com.postmortemai.application.dto.ProjectHistoryItem;

/**
 * Output Port for Incident persistence.
 */
public interface IncidentRepositoryPort {
    Incident save(Incident incident);
    Optional<Incident> findByRawLogHash(String rawLogHash);
    Optional<Incident> findById(UUID id);
    PageResult<ProjectHistoryItem> findHistoryByProject(String projectName, PageQuery pageQuery);
}
