package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.infrastructure.persistence.entity.PostMortemEntity;
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
        return new PostMortemEntity(
                domain.id(),
                domain.incidentId(),
                domain.title(),
                domain.summary(),
                domain.timeline(),
                domain.rootCause(),
                domain.impact(),
                domain.detection(),
                domain.contributingFactors(),
                domain.actionItems(),
                domain.lessonsLearned(),
                domain.exportedMarkdown(),
                domain.createdAt()
        );
    }

    private PostMortem mapToDomain(PostMortemEntity entity) {
        return new PostMortem(
                entity.getId(),
                entity.getIncidentId(),
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
