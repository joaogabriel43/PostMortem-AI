package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.infrastructure.persistence.entity.PostMortemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostMortemJpaRepository extends JpaRepository<PostMortemEntity, UUID> {
    Optional<PostMortemEntity> findByIncidentId(UUID incidentId);
}
