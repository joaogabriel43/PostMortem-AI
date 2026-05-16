package com.postmortemai.application.port;

import com.postmortemai.domain.model.PostMortem;

import java.util.Optional;
import java.util.UUID;

/**
 * Output Port for PostMortem persistence.
 */
public interface PostMortemRepositoryPort {
    PostMortem save(PostMortem postMortem);
    Optional<PostMortem> findByIncidentId(UUID incidentId);
}
