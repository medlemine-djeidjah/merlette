package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.ConversationDto;
import com.merlette.backend.dto.MeDto;
import com.merlette.backend.dto.ObservationSummaryDto;
import com.merlette.backend.dto.PageResponse;
import com.merlette.backend.dto.UpdateProfileRequest;
import com.merlette.backend.dto.UserProfileDto;
import com.merlette.backend.security.CurrentUser;
import com.merlette.backend.service.ObservationService;
import com.merlette.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Utilisateurs", description = "Profils, mes observations, mes discussions")
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ObservationService observationService;
    private final CurrentUser currentUser;

    @GetMapping("/{id}")
    public UserProfileDto profile(@PathVariable Integer id) {
        return userService.profile(id);
    }

    @PatchMapping("/me")
    public MeDto updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMe(currentUser.id(), request);
    }

    @GetMapping("/me/observations")
    public PageResponse<ObservationSummaryDto> myObservations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return observationService.list(category, status, currentUser.id(), sort, page, limit);
    }

    @GetMapping("/me/conversations")
    public Map<String, List<ConversationDto>> myConversations() {
        return Map.of("data", userService.conversations(currentUser.id()));
    }
}
