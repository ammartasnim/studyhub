package com.dsi.studyhub.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResponseDTO {
    private Long id;
    private String title;
    private String description;
    private int nbrMembers;
}

