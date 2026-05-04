package com.dsi.studyhub.entities;

import com.dsi.studyhub.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships", indexes = {
        @Index(name = "idx_friendship_requester", columnList = "requester_id"),
        @Index(name = "idx_friendship_addressee", columnList = "addressee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

  @EmbeddedId
  private FriendshipId id;

  @ManyToOne
  @MapsId("requesterId")
  @JoinColumn(name = "requester_id")
  private User requester;

  @ManyToOne
  @MapsId("addresseeId")
  @JoinColumn(name = "addressee_id")
  private User addressee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FriendshipStatus status = FriendshipStatus.PENDING;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
}