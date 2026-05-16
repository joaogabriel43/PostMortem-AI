package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {
    Optional<IncidentEntity> findByRawLogHash(String rawLogHash);
}
