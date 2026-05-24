package com.project.Blog_Management_System.Entities;

import com.project.Blog_Management_System.Annotations.uuidV7.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_depth", columnList = "post_id, depth"),
        @Index(name = "idx_comments_parent_id", columnList = "parent_id")
})
public class CommentEntity {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String body;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", updatable = false)
    private CommentEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CommentEntity> replies = new ArrayList<>();

    @Column(nullable = false)
    private Integer depth = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
