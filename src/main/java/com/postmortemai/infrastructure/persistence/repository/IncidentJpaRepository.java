package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {
    Optional<IncidentEntity> findByRawLogHash(String rawLogHash);

    @Query("SELECT i.id AS id, p.title AS title, i.severity AS severity, i.status AS status, i.createdAt AS createdAt " +
           "FROM IncidentEntity i LEFT JOIN PostMortemEntity p ON p.incident = i " +
           "WHERE i.projectName = :projectName ORDER BY i.createdAt DESC")
    org.springframework.data.domain.Page<ProjectHistoryProjection> findHistoryByProjectName(
            @org.springframework.data.repository.query.Param("projectName") String projectName, 
            org.springframework.data.domain.Pageable pageable);
}
