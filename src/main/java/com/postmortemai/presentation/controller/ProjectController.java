package com.postmortemai.presentation.controller;

import com.postmortemai.application.dto.PageQuery;
import com.postmortemai.application.dto.PageResult;
import com.postmortemai.application.dto.ProjectHistoryItem;
import com.postmortemai.application.usecase.GetProjectHistoryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final GetProjectHistoryUseCase getProjectHistoryUseCase;

    public ProjectController(GetProjectHistoryUseCase getProjectHistoryUseCase) {
        this.getProjectHistoryUseCase = getProjectHistoryUseCase;
    }

    @GetMapping("/{projectName}/postmortems")
    public ResponseEntity<PageResult<ProjectHistoryItem>> getProjectHistory(
            @PathVariable String projectName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageQuery query = new PageQuery(page, size);
        PageResult<ProjectHistoryItem> result = getProjectHistoryUseCase.execute(projectName, query);
        return ResponseEntity.ok(result);
    }
}
