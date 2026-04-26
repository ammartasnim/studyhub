package com.dsi.studyhub.dtos;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.Comment}
 */
public record CommentResDto(Long id, Long postId, Long userId, String content) implements Serializable {
}
