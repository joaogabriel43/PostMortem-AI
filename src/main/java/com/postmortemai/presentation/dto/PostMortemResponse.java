package com.postmortemai.presentation.dto;

import com.postmortemai.domain.model.PostMortem;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostMortemResponse(
        UUID id,
        UUID incidentId,
        String title,
        String summary,
        String timeline,
        String rootCause,
        String impact,
        String detection,
        String contributingFactors,
        String actionItems,
        String lessonsLearned,
        String exportedMarkdown,
        LocalDateTime createdAt
) {
    public static PostMortemResponse fromDomain(PostMortem postMortem) {
        return new PostMortemResponse(
                postMortem.id(),
                postMortem.incidentId(),
                postMortem.title(),
                postMortem.summary(),
                postMortem.timeline(),
                postMortem.rootCause(),
                postMortem.impact(),
                postMortem.detection(),
                postMortem.contributingFactors(),
                postMortem.actionItems(),
                postMortem.lessonsLearned(),
                postMortem.exportedMarkdown(),
                postMortem.createdAt()
        );
    }
}
