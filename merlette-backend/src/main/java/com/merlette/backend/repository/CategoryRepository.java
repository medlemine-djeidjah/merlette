package com.merlette.backend.repository;

import com.merlette.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);
}
