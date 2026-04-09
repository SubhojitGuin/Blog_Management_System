package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    @ReadFast
    Optional<CategoryEntity> findBySlug(String slug);

}
