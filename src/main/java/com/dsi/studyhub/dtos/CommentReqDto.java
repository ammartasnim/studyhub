package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.Comment}
 */
public record CommentReqDto(Long postId, @NotBlank String content) implements Serializable {
}
