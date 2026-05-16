package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link IncidentEntity}.
 *
 * <p>This interface is an infrastructure concern and must NOT be injected
 * directly into application services. Application services depend on the
 * domain repository interfaces defined in {@code domain.repository}.
 */
public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {

    List<IncidentEntity> findByProjectName(String projectName);

    Optional<IncidentEntity> findByRawLogHash(String rawLogHash);
}
