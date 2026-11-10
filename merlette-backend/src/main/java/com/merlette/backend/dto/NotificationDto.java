package com.merlette.backend.dto;

import com.merlette.backend.entity.Notification;
import java.time.Instant;

public record NotificationDto(Integer id, String type, String message, Integer observationId,
                              UserSummaryDto actor, boolean isRead, Instant createdAt) {

    public static NotificationDto from(Notification n) {
        Integer observationId = n.getObservation() == null ? null : n.getObservation().getId();
        return new NotificationDto(n.getId(), n.getType().name(), n.getMessage(), observationId,
                UserSummaryDto.from(n.getActor()), n.isRead(), n.getCreatedAt());
    }
}
