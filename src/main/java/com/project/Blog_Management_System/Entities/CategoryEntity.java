package com.project.Blog_Management_System.Entities;

import com.project.Blog_Management_System.Annotations.uuidV7.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.generateSlug;

@Entity
@Getter
@Setter
@FieldNameConstants
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_slug", columnList = "slug")
})
public class CategoryEntity {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void compute() {
        this.slug = generateSlug(this.name);
    }
}
