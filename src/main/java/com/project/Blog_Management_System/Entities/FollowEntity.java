package com.project.Blog_Management_System.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"follower_id", "following_id"})
        },
        indexes = {
                @Index(name = "idx_follows_following_id_followed_at", columnList = "following_id, followed_at DESC"),
                @Index(name = "idx_follows_follower_id_followed_at", columnList = "follower_id, followed_at DESC")
        }
)
public class FollowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false, updatable = false)
    private UserEntity follower; // The user who is following

    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false, updatable = false)
    private UserEntity following; // The user being followed

    @CreationTimestamp
    @Column(name = "followed_at", nullable = false, updatable = false)
    private LocalDateTime followedAt;
}
