package com.postmortemai.application.usecase;

import com.postmortemai.application.dto.PageQuery;
import com.postmortemai.application.dto.PageResult;
import com.postmortemai.application.dto.ProjectHistoryItem;
import com.postmortemai.application.port.IncidentRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class GetProjectHistoryUseCase {

    private final IncidentRepositoryPort incidentRepositoryPort;

    public GetProjectHistoryUseCase(IncidentRepositoryPort incidentRepositoryPort) {
        this.incidentRepositoryPort = incidentRepositoryPort;
    }

    public PageResult<ProjectHistoryItem> execute(String projectName, PageQuery pageQuery) {
        return incidentRepositoryPort.findHistoryByProject(projectName, pageQuery);
    }
}
