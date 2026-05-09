package com.dsi.studyhub.dtos;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
public record CommentReqDto(Long postId, @NotBlank String content) implements Serializable {
}
