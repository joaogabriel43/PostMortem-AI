package com.postmortemai.presentation.dto;

import java.util.UUID;

public record TaskResponse(UUID taskId, String message) {
}
