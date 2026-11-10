package com.merlette.backend.dto;

import com.merlette.backend.entity.Species;

/** Forme courte d'une espèce (propositions, espèce retenue). */
public record SpeciesSummaryDto(Integer id, String commonName, String scientificName) {

    public static SpeciesSummaryDto from(Species s) {
        return s == null ? null : new SpeciesSummaryDto(s.getId(), s.getCommonName(), s.getScientificName());
    }
}
