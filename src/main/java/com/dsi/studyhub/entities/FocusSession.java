package com.dsi.studyhub.entities;

import com.dsi.studyhub.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_focus_session_user_id", columnList = "user_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FocusSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;
    private String title;
    private String timer;

    private Integer remainingSeconds;
    private LocalDateTime lastUpdated;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_user_id", nullable = false)
    private User user;
}
