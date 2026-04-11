package com.project.Blog_Management_System.Specifications;

import com.project.Blog_Management_System.Dto.PostFilterRequestDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import org.springframework.data.jpa.domain.Specification;

public class PostFilterSpecification {

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String likePattern(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private static Specification<PostEntity> hasTitle(String title) {
        return (root, query, cb) ->
                !hasText(title) ? null :
                        cb.like(cb.lower(root.get(PostEntity.Fields.title)), likePattern(title));
    }

    private static Specification<PostEntity> hasCategory(String categorySlug) {
        return (root, query, cb) ->
                !hasText(categorySlug) ? null :
                        cb.equal(root.get(PostEntity.Fields.category).get(CategoryEntity.Fields.slug), categorySlug.toLowerCase());
    }

    public static Specification<PostEntity> buildSpecification(PostFilterRequestDTO params) {
        return Specification.where(hasTitle(params.getTitle()))
                .and(hasCategory(params.getCategorySlug()));
    }
}