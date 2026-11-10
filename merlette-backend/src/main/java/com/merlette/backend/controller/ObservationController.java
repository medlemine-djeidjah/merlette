package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.ObservationDetailDto;
import com.merlette.backend.dto.ObservationForm;
import com.merlette.backend.dto.ObservationSummaryDto;
import com.merlette.backend.dto.PageResponse;
import com.merlette.backend.dto.UpdateObservationRequest;
import com.merlette.backend.service.ObservationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Observations", description = "Publication, fil, détail, photos")
@RequestMapping("/api/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @GetMapping
    public PageResponse<ObservationSummaryDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return observationService.list(category, status, userId, sort, page, limit);
    }

    @GetMapping("/{id}")
    public ObservationDetailDto get(@PathVariable Integer id) {
        return observationService.get(id);
    }

    /** Multipart : champ "photos" (1 à 5 fichiers) + champs texte du formulaire. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationDetailDto create(@Valid @ModelAttribute ObservationForm form,
                                       @RequestParam(name = "photos", required = false) List<MultipartFile> photos) {
        return observationService.create(form, photos);
    }

    @PatchMapping("/{id}")
    public ObservationDetailDto update(@PathVariable Integer id,
                                       @Valid @RequestBody UpdateObservationRequest request) {
        return observationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        observationService.delete(id);
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationDetailDto addPhotos(@PathVariable Integer id,
                                          @RequestParam(name = "photos", required = false) List<MultipartFile> photos) {
        return observationService.addPhotos(id, photos);
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Integer id, @PathVariable Integer photoId) {
        observationService.deletePhoto(id, photoId);
    }
}
