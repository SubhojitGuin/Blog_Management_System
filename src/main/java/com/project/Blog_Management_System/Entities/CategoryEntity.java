package com.project.Blog_Management_System.Entities;

import com.project.Blog_Management_System.Annotations.uuidV7.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static com.project.Blog_Management_System.Utils.AppUtils.generateSlug;

@Entity
@Getter
@Setter
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_slug", columnList = "slug")
})
public class CategoryEntity extends Auditable {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void compute() {
        this.slug = generateSlug(this.name);
    }
}
