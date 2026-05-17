package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.application.port.IncidentRepositoryPort;
import com.postmortemai.application.dto.PageQuery;
import com.postmortemai.application.dto.PageResult;
import com.postmortemai.application.dto.ProjectHistoryItem;
import com.postmortemai.domain.model.Incident;
import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IncidentRepositoryAdapter implements IncidentRepositoryPort {

    private final IncidentJpaRepository jpaRepository;

    public IncidentRepositoryAdapter(IncidentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Incident save(Incident incident) {
        IncidentEntity entity = mapToEntity(incident);
        IncidentEntity saved = jpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Incident> findByRawLogHash(String rawLogHash) {
        return jpaRepository.findByRawLogHash(rawLogHash)
                .map(this::mapToDomain);
    }

    @Override
    public PageResult<ProjectHistoryItem> findHistoryByProject(String projectName, PageQuery pageQuery) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(
                pageQuery.page(),
                pageQuery.size()
        );

        org.springframework.data.domain.Page<ProjectHistoryProjection> springPage = jpaRepository.findHistoryByProjectName(projectName, pageRequest);

        java.util.List<ProjectHistoryItem> mappedData = springPage.getContent().stream()
                .map(proj -> new ProjectHistoryItem(
                        proj.getId(),
                        proj.getTitle(),
                        proj.getSeverity() != null ? proj.getSeverity().name() : null,
                        proj.getStatus() != null ? proj.getStatus().name() : null,
                        proj.getCreatedAt()
                ))
                .toList();

        return new PageResult<>(
                mappedData,
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.getNumber()
        );
    }

    private IncidentEntity mapToEntity(Incident domain) {
        return new IncidentEntity(
                domain.id(),
                domain.projectName(),
                domain.serviceName(),
                domain.rawLogHash(),
                domain.severity(),
                domain.status(),
                domain.createdAt()
        );
    }

    private Incident mapToDomain(IncidentEntity entity) {
        return new Incident(
                entity.getId(),
                entity.getProjectName(),
                entity.getServiceName(),
                entity.getRawLogHash(),
                entity.getSeverity(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
