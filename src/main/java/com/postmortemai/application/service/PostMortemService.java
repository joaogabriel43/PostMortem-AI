package com.postmortemai.application.service;

import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.application.usecase.GeneratePostMortemUseCase;
import com.postmortemai.domain.model.PostMortem;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PostMortemService {

    private final GeneratePostMortemUseCase generatePostMortemUseCase;
    private final PostMortemRepositoryPort postMortemRepositoryPort;

    public PostMortemService(
            GeneratePostMortemUseCase generatePostMortemUseCase,
            PostMortemRepositoryPort postMortemRepositoryPort
    ) {
        this.generatePostMortemUseCase = generatePostMortemUseCase;
        this.postMortemRepositoryPort = postMortemRepositoryPort;
    }

    public PostMortem generatePostMortem(String projectName, String serviceName, String rawLog) {
        return generatePostMortemUseCase.execute(projectName, serviceName, rawLog);
    }

    public Optional<PostMortem> getPostMortemByIncidentId(UUID incidentId) {
        return postMortemRepositoryPort.findByIncidentId(incidentId);
    }
}
