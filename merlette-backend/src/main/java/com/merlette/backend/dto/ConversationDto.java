package com.merlette.backend.dto;

import java.time.Instant;
import java.util.List;

/** Une ligne de « Mes discussions ». myRole : AUTHOR, COMMENTER, PROPOSER. */
public record ConversationDto(Integer observationId, String title, String status, String coverUrl,
                              Instant lastActivity, List<String> myRole) {}
