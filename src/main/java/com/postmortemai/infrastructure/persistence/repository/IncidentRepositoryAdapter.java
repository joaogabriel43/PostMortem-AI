package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.application.port.IncidentRepositoryPort;
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
