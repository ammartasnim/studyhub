package com.dsi.studyhub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_community_moderator_id", columnList = "moderator_id"),
        @Index(name = "idx_community_nbr_members", columnList = "nbrMembers")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Community {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private int nbrMembers;
    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @ManyToMany(mappedBy = "joinedCommunities")
    private List<User> members = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "moderator_id")
    private User moderator;

}
