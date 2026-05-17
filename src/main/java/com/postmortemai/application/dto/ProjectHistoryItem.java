package com.postmortemai.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectHistoryItem(
        UUID id,
        String title,
        String severity,
        String status,
        LocalDateTime createdAt
) {}
