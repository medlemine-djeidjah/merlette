package com.merlette.backend.dto;

import com.merlette.backend.entity.Photo;

public record PhotoDto(Integer id, String url, Integer position) {

    public static PhotoDto from(Photo p) {
        return new PhotoDto(p.getId(), p.getUrl(), p.getPosition());
    }
}
