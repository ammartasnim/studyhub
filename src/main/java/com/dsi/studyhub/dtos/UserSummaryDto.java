package com.dsi.studyhub.dtos;

import java.io.Serializable;

public record UserSummaryDto(
        Long id,
        String username,
        String pfp,
        String firstName,
        String lastName
) implements Serializable {
}
