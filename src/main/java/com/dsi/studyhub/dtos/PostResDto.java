package com.dsi.studyhub.dtos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link com.dsi.studyhub.entities.Post}
 */
public record PostResDto(Long id, String title, String content, List<String> imgs, String userUsername,
                         String userFirstName, String userLastName, String communityTitle) implements Serializable {
}