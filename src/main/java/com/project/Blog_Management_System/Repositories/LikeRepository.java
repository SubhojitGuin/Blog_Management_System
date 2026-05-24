package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Annotations.ReadFast;
import com.project.Blog_Management_System.Dto.LikeInfoDTO;
import com.project.Blog_Management_System.Entities.LikeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

    @ReadFast
    Optional<LikeEntity> findByUserIdAndPostId(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.LikeInfoDTO(
                    new com.project.Blog_Management_System.Dto.UserInfoDTO(
                            u.id,
                            u.name,
                            u.username,
                            u.active
                    ),
                    l.id
                )
                FROM LikeEntity l
                LEFT JOIN l.user u
                WHERE l.post.id = :postId
                AND (:likeCursor IS NULL OR l.id < :likeCursor)
                ORDER BY l.id DESC
            """)
    @ReadFast
    Slice<LikeInfoDTO> findLikesOfPost(
            @Param("postId") UUID postId,
            @Param("likeCursor") UUID likeCursor,
            Pageable pageable
    );

}
