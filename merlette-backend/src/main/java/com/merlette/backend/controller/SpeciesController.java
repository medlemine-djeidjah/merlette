package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.CreateSpeciesRequest;
import com.merlette.backend.dto.SpeciesDto;
import com.merlette.backend.service.SpeciesService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Espèces", description = "Recherche et référentiel des espèces (création : admin)")
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping
    public List<SpeciesDto> search(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) Integer limit) {
        return speciesService.search(q, category, limit);
    }

    @GetMapping("/{id}")
    public SpeciesDto get(@PathVariable Integer id) {
        return speciesService.get(id);
    }

    /** Admin uniquement (voir SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpeciesDto create(@Valid @RequestBody CreateSpeciesRequest request) {
        return speciesService.create(request);
    }
}
