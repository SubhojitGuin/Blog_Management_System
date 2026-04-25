package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.CommentResponseDTO;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
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
                c.createdAt,
                CASE WHEN u.id = :currentUserId THEN true ELSE false END
            )
            FROM CommentEntity c
            JOIN c.user u
            WHERE c.post.id = :postId
            """)
    @ReadFast
    Slice<CommentResponseDTO> findByPost(
            @Param("postId") UUID postId,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

}
