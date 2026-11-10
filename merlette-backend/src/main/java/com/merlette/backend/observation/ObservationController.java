package com.merlette.backend.observation;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class ObservationController {
    private final ObservationService service;
    private final CategoryRepository categories;
    public ObservationController(ObservationService service, CategoryRepository categories) {
        this.service = service; this.categories = categories;
    }
    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }
    @GetMapping("/categories") public List<ObservationService.CategoryView> categories() {
        return categories.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(ObservationService.CategoryView::of).toList();
    }
    @PostMapping(value = "/observations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationService.ObservationView create(@AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute ObservationService.Creation input) {
        return service.create(Integer.valueOf(jwt.getSubject()), input);
    }
    @GetMapping("/observations/{id}")
    public ObservationService.ObservationView detail(@PathVariable Integer id) { return service.detail(id); }
    @GetMapping("/observations")
    public ObservationService.Feed feed(@RequestParam(required = false) String category,
        @RequestParam(required = false) Observation.Status status, @RequestParam(required = false) Integer userId,
        @RequestParam(defaultValue = "recent") String sort, @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit) {
        return service.feed(category, status, userId, sort, page, limit);
    }
    @GetMapping("/users/me/observations")
    public ObservationService.Feed mine(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {
        return service.feed(null, null, Integer.valueOf(jwt.getSubject()), "recent", page, limit);
    }
}
