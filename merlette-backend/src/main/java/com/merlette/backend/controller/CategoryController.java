package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.CategoryDto;
import com.merlette.backend.dto.CreateCategoryRequest;
import com.merlette.backend.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Catégories", description = "Référentiel des catégories (création : admin)")
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> list() {
        return categoryService.list();
    }

    /** Admin uniquement (voir SecurityConfig). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }
}
