package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.Community}
 */
public record CommunityReqDto(
        @NotBlank String title,
        @NotBlank String description,
        Integer nbrMembers
) implements Serializable {
}
