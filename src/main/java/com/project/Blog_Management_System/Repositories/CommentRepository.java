package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.CommentResponseDTO;
import com.project.Blog_Management_System.Entities.CommentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @Query("""
            SELECT new com.project.Blog_Management_System.Dto.CommentResponseDTO(
                c.id,
                c.body,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                NULL,
                CASE WHEN ((SELECT COUNT(rc) FROM CommentEntity rc WHERE rc.parent.id = c.id) > 0) THEN true ELSE false END,
                c.createdAt,
                CASE WHEN u.id = :currentUserId THEN true ELSE false END
            )
            FROM CommentEntity c
            JOIN c.user u
            WHERE c.post.id = :postId
            AND c.depth = 0
            AND (:commentCursor IS NULL OR c.id < :commentCursor)
            ORDER BY c.id DESC
            """)
    Slice<CommentResponseDTO> findTopLevelByPost(
            @Param("postId") UUID postId,
            @Param("commentCursor") UUID commentCursor,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.project.Blog_Management_System.Dto.CommentResponseDTO(
                c.id,
                c.body,
                new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username, u.active),
                c.parent.id,
                CASE WHEN ((SELECT COUNT(rc) FROM CommentEntity rc WHERE rc.parent.id = c.id) > 0) THEN true ELSE false END,
                c.createdAt,
                CASE WHEN u.id = :currentUserId THEN true ELSE false END
            )
            FROM CommentEntity c
            JOIN c.user u
            WHERE c.parent.id = :parentId
            AND (:commentCursor IS NULL OR c.id < :commentCursor)
            ORDER BY c.id DESC
            """)
    Slice<CommentResponseDTO> findRepliesByParentId(
            @Param("parentId") UUID parentId,
            @Param("commentCursor") UUID commentCursor,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

}
