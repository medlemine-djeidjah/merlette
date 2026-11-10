package com.merlette.backend.service;

import com.merlette.backend.dto.CategoryDto;
import com.merlette.backend.dto.CreateCategoryRequest;
import com.merlette.backend.entity.Category;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryRepository.findAll(Sort.by("id")).stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())
                || categoryRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Cette catégorie existe déjà.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(request.slug());
        return CategoryDto.from(categoryRepository.save(category));
    }

    /** Catégorie existante, sinon 400 (utilisé à la création d'une observation ou d'une espèce). */
    @Transactional(readOnly = true)
    public Category getForInput(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.badRequest("Catégorie inconnue."));
    }
}
