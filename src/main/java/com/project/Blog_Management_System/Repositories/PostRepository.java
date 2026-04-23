package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.PostResponseDTO;
import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID>, JpaSpecificationExecutor<PostEntity> {

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.PostResponseDTO(
                p.id, p.slug, p.title, p.description, p.content, p.readingTimeMinutes, p.likeCount, p.commentCount, p.viewCount,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                new com.project.Blog_Management_System.Dto.CategoryResponseDTO(c.id, c.slug, c.name, c.description),
                CASE WHEN u = :currentUser THEN true ELSE false END,
                CASE WHEN l.id IS NOT NULL THEN true ELSE false END
                )
                FROM PostEntity p
                JOIN p.user u
                JOIN p.category c
                LEFT JOIN LikeEntity l ON l.post = p AND l.user = :currentUser
                WHERE u = :profileUser
                ORDER BY p.createdAt DESC
            """)
    @ReadFast
    Slice<PostResponseDTO> findPostsByUser(
            @Param("profileUser") UserEntity profileUser,
            @Param("currentUser") UserEntity currentUser,
            Pageable pageable
    );

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.PostResponseDTO(
                p.id, p.slug, p.title, p.description, p.content, p.readingTimeMinutes, p.likeCount, p.commentCount, p.viewCount,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                new com.project.Blog_Management_System.Dto.CategoryResponseDTO(c.id, c.slug, c.name, c.description),
                CASE WHEN u = :currentUser THEN true ELSE false END,
                CASE WHEN l.id IS NOT NULL THEN true ELSE false END
                )
                FROM PostEntity p
                JOIN p.user u
                JOIN p.category c
                LEFT JOIN LikeEntity l ON l.post = p AND l.user = :currentUser
                WHERE c = :category
                ORDER BY p.createdAt DESC
            """)
    @ReadFast
    Slice<PostResponseDTO> findPostsByCategory(
            @Param("category") CategoryEntity category,
            @Param("currentUser") UserEntity currentUser,
            Pageable pageable
    );

    @Modifying
    @Query("""
                UPDATE PostEntity p
                SET p.category = :newCategory
                WHERE p.category = :oldCategory
            """)
    void updatePostsCategory(
            @Param("oldCategory") CategoryEntity oldCategory,
            @Param("newCategory") CategoryEntity newCategory
    );

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.PostResponseDTO(
                p.id, p.slug, p.title, p.description, p.content, p.readingTimeMinutes, p.likeCount, p.commentCount, p.viewCount,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                new com.project.Blog_Management_System.Dto.CategoryResponseDTO(c.id, c.slug, c.name, c.description),
                CASE WHEN u = :currentUser THEN true ELSE false END,
                CASE WHEN l.id IS NOT NULL THEN true ELSE false END
                )
                FROM PostEntity p
                JOIN p.user u
                JOIN p.category c
                LEFT JOIN LikeEntity l ON l.post = p AND l.user = :currentUser
                ORDER BY p.createdAt DESC
            """)
    @ReadFast
    Slice<PostResponseDTO> findAllPosts(
            @Param("currentUser") UserEntity currentUser,
            Pageable pageable
    );

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.PostResponseDTO(
                p.id, p.slug, p.title, p.description, p.content, p.readingTimeMinutes, p.likeCount, p.commentCount, p.viewCount,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                new com.project.Blog_Management_System.Dto.CategoryResponseDTO(c.id, c.slug, c.name, c.description),
                CASE WHEN u = :currentUser THEN true ELSE false END,
                CASE WHEN l.id IS NOT NULL THEN true ELSE false END
                )
                FROM PostEntity p
                JOIN p.user u
                JOIN p.category c
                JOIN FollowEntity f ON f.following = u AND f.follower = :currentUser
                LEFT JOIN LikeEntity l ON l.post = p AND l.user = :currentUser
                ORDER BY p.createdAt DESC
            """)
    @ReadFast
    Slice<PostResponseDTO> findAllPostsOfFollowings(
            @Param("currentUser") UserEntity currentUser,
            Pageable pageable
    );

    @Modifying
    @Query(
            """
                        UPDATE PostEntity p
                        SET p.likeCount = p.likeCount + 1
                        WHERE p = :post
                    """
    )
    int incrementLikeCount(@Param("post") PostEntity post);

    @Modifying
    @Query(
            """
                        UPDATE PostEntity p
                        SET p.likeCount = p.likeCount - 1
                        WHERE p = :post
                    """
    )
    int decrementLikeCount(@Param("post") PostEntity post);

    @Modifying
    @Query(
            """
                        UPDATE PostEntity p
                        SET p.commentCount = p.commentCount + 1
                        WHERE p = :post
                    """
    )
    int incrementCommentCount(@Param("post") PostEntity post);

    @Modifying
    @Query(
            """
                        UPDATE PostEntity p
                        SET p.commentCount = p.commentCount - 1
                        WHERE p = :post
                    """
    )
    int decrementCommentCount(@Param("post") PostEntity post);

    @Modifying
    @Query("""
                UPDATE PostEntity p
                SET p.viewCount = p.viewCount + 1
                WHERE p = :post
            """
    )
    void incrementViewCount(@Param("post") PostEntity post);

}
