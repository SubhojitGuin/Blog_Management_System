package com.project.Blog_Management_System.Entities;

import com.project.Blog_Management_System.Annotations.uuidV7.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_bookmarks_user_id", columnList = "user_id")
        }
)
public class BookmarkEntity {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private PostEntity post;

    @CreationTimestamp
    @Column(name = "bookmarked_at", nullable = false, updatable = false)
    private LocalDateTime bookmarkedAt;
}
