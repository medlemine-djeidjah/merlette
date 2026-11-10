package com.merlette.backend.service;

import com.merlette.backend.dto.CreateSpeciesRequest;
import com.merlette.backend.dto.SpeciesDto;
import com.merlette.backend.entity.Species;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.SpeciesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final CategoryService categoryService;

    /** Recherche par nom commun ou scientifique (q ≥ 2 caractères si fourni). */
    @Transactional(readOnly = true)
    public List<SpeciesDto> search(String q, String category, Integer limit) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        if (q != null && (query == null || query.length() < 2)) {
            throw ApiException.badRequest("La recherche doit contenir au moins 2 caractères.");
        }
        int max = (limit == null) ? 20 : Math.clamp(limit, 1, 50);
        String slug = (category == null || category.isBlank()) ? null : category;
        String pattern = query == null ? null : escapeLike(query);
        return speciesRepository.search(pattern, slug, max).stream().map(SpeciesDto::from).toList();
    }

    /** Les caractères % et _ tapés par l'utilisateur sont cherchés tels quels. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional(readOnly = true)
    public SpeciesDto get(Integer id) {
        return SpeciesDto.from(speciesRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Espèce introuvable.")));
    }

    @Transactional
    public SpeciesDto create(CreateSpeciesRequest request) {
        if (speciesRepository.existsByScientificNameIgnoreCase(request.scientificName().trim())) {
            throw ApiException.conflict("Cette espèce existe déjà.");
        }
        Species species = new Species();
        species.setCommonName(request.commonName().trim());
        species.setScientificName(request.scientificName().trim());
        species.setCategory(categoryService.getForInput(request.categoryId()));
        return SpeciesDto.from(speciesRepository.save(species));
    }
}
