package com.project.Blog_Management_System.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "follows",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"follower_id", "following_id"})})
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
