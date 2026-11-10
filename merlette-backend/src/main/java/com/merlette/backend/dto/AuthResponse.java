package com.merlette.backend.dto;

public record AuthResponse(String token, MeDto user) {}
