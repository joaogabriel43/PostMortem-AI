package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.infrastructure.persistence.entity.PostMortemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PostMortemEntity}.
 *
 * <p>This interface is an infrastructure concern and must NOT be injected
 * directly into application services. Application services depend on the
 * domain repository interfaces defined in {@code domain.repository}.
 */
public interface PostMortemJpaRepository extends JpaRepository<PostMortemEntity, UUID> {

    /**
     * Fetches all post-mortems for a given incident.
     * The incident association is LAZY, so we fetch-join here to avoid N+1.
     */
    @Query("SELECT pm FROM PostMortemEntity pm JOIN FETCH pm.incident WHERE pm.incident.id = :incidentId")
    List<PostMortemEntity> findByIncidentId(@Param("incidentId") UUID incidentId);

    /**
     * Fetches a single post-mortem with its incident in one query (no N+1).
     */
    @Query("SELECT pm FROM PostMortemEntity pm JOIN FETCH pm.incident WHERE pm.id = :id")
    Optional<PostMortemEntity> findByIdWithIncident(@Param("id") UUID id);
}
