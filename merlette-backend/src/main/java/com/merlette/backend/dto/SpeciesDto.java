package com.merlette.backend.dto;

import com.merlette.backend.entity.Species;

public record SpeciesDto(Integer id, String commonName, String scientificName, CategoryDto category) {

    public static SpeciesDto from(Species s) {
        return new SpeciesDto(s.getId(), s.getCommonName(), s.getScientificName(),
                CategoryDto.from(s.getCategory()));
    }
}
