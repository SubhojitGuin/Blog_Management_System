package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.FollowInfoDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, UUID> {

    @Query("""
       SELECT new com.project.Blog_Management_System.Dto.FollowInfoDTO(
            new com.project.Blog_Management_System.Dto.UserInfoDTO(
                u.id,
                u.name,
                u.username,
                u.active
            ),
            f.id
       )
       FROM FollowEntity f
       JOIN f.follower u
       WHERE f.following.id = :userId
       AND (:followCursor IS NULL OR f.id < :followCursor)
       ORDER BY f.id DESC
       """)
    @ReadFast
    Slice<FollowInfoDTO> findFollowers(@Param("userId") UUID userId, @Param("followCursor") UUID followCursor, Pageable pageable);

    @Query("""
         SELECT new com.project.Blog_Management_System.Dto.FollowInfoDTO(
             new com.project.Blog_Management_System.Dto.UserInfoDTO(
                u.id,
                u.name,
                u.username,
                u.active
             ),
             f.id
         )
         FROM FollowEntity f
         JOIN f.following u
         WHERE f.follower.id = :userId
         AND (:followCursor IS NULL OR f.id < :followCursor)
         ORDER BY f.id DESC
         """
    )
    @ReadFast
    Slice<FollowInfoDTO> findFollowing(@Param("userId") UUID userId, @Param("followCursor") UUID followCursor, Pageable pageable);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    void deleteByFollowerIdOrFollowingId(UUID followerId, UUID followingId);

    @ReadFast
    Optional<FollowEntity> findByFollowerIdAndFollowingId(UUID followerId, UUID followeeId);

    @ReadFast
    List<FollowEntity> findByFollowingId(UUID followingId);

}
