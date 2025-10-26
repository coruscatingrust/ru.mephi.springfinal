package ru.mephi.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record HotelDto(
        Long id,
        @NotBlank String name,
        @NotBlank String address
) {}
