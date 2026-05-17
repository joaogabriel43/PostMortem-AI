package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.infrastructure.persistence.entity.PostMortemEntity;
import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PostMortemRepositoryAdapter implements PostMortemRepositoryPort {

    private final PostMortemJpaRepository jpaRepository;

    public PostMortemRepositoryAdapter(PostMortemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PostMortem save(PostMortem postMortem) {
        PostMortemEntity entity = mapToEntity(postMortem);
        PostMortemEntity saved = jpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<PostMortem> findByIncidentId(UUID incidentId) {
        return jpaRepository.findByIncidentId(incidentId)
                .map(this::mapToDomain);
    }

    private PostMortemEntity mapToEntity(PostMortem domain) {
        org.springframework.data.jpa.repository.support.SimpleJpaRepository proxy;
        IncidentEntity incidentProxy = new IncidentEntity(domain.incidentId(), null, null, null, null, null, null);

        PostMortemEntity entity = new PostMortemEntity(
                domain.id(),
                incidentProxy,
                domain.title(),
                domain.summary(),
                domain.timeline(),
                domain.rootCause(),
                domain.impact(),
                domain.detection(),
                domain.contributingFactors(),
                domain.actionItems(),
                domain.lessonsLearned(),
                domain.createdAt()
        );
        entity.setExportedMarkdown(domain.exportedMarkdown());
        return entity;
    }

    private PostMortem mapToDomain(PostMortemEntity entity) {
        return new PostMortem(
                entity.getId(),
                entity.getIncident().getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getTimeline(),
                entity.getRootCause(),
                entity.getImpact(),
                entity.getDetection(),
                entity.getContributingFactors(),
                entity.getActionItems(),
                entity.getLessonsLearned(),
                entity.getExportedMarkdown(),
                entity.getCreatedAt()
        );
    }
}
