package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Annotations.ReadFast;
import com.project.Blog_Management_System.Dto.BookmarkInfoDTO;
import com.project.Blog_Management_System.Entities.BookmarkEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkEntity, UUID> {
    @ReadFast
    Optional<BookmarkEntity> findByUserIdAndPostId(UUID userId, UUID postId);

    void deleteByUserIdAndPostId(UUID userId, UUID postId);

    @Query("""
             SELECT new com.project.Blog_Management_System.Dto.BookmarkInfoDTO(
                    b.id,
                    new com.project.Blog_Management_System.Dto.PostInfoDTO(
                        p.id,
                        p.slug,
                        p.title,
                        p.description,
                        p.readingTimeMinutes,
                        p.likeCount,
                        p.commentCount,
                        p.viewCount
                    ),
                    b.bookmarkedAt
                )
                FROM BookmarkEntity b
                JOIN b.post p
                WHERE b.user.id = :userId
                AND (:bookmarkCursor IS NULL OR b.id < :bookmarkCursor)
                ORDER BY b.id DESC
            """)
    @ReadFast
    Slice<BookmarkInfoDTO> findByUser(@Param("userId") UUID userId,
                                      @Param("bookmarkCursor") UUID bookmarkCursor,
                                      Pageable pageable);

}
