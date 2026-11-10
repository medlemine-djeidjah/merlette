package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.NotificationDto;
import com.merlette.backend.dto.PageResponse;
import com.merlette.backend.security.CurrentUser;
import com.merlette.backend.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Notifications", description = "Notifications in-app (polling)")
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<NotificationDto> list(@RequestParam(defaultValue = "false") boolean unread,
                                              @RequestParam(defaultValue = "1") int page) {
        return notificationService.list(currentUser.id(), unread, page);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(currentUser.id()));
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(@PathVariable Integer id) {
        return notificationService.markAsRead(id, currentUser.id());
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead() {
        return Map.of("updated", notificationService.markAllAsRead(currentUser.id()));
    }
}
