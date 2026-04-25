package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, UUID> {

    @Query("""
       SELECT new com.project.Blog_Management_System.Dto.UserInfoDTO(
            u.id,
            u.name,
            u.username,
            u.active
       )
       FROM FollowEntity f
       JOIN f.follower u
       WHERE f.following.id = :userId
       ORDER BY f.followedAt DESC
       """)
    @ReadFast
    Slice<UserInfoDTO> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
         SELECT new com.project.Blog_Management_System.Dto.UserInfoDTO(
                u.id,
                u.name,
                u.username,
                u.active
         )
         FROM FollowEntity f
         JOIN f.following u
         WHERE f.follower.id = :userId
         ORDER BY f.followedAt DESC
         """
    )
    @ReadFast
    Slice<UserInfoDTO> findFollowing(@Param("userId") UUID userId, Pageable pageable);

    void deleteByFollower_IdAndFollowing_Id(UUID follower_id, UUID following_id);

    void deleteByFollower_IdOrFollowing_Id(UUID follower_id, UUID following_id);

    @ReadFast
    Optional<FollowEntity> findByFollower_IdAndFollowing_Id(UUID follower_id, UUID followee_id);

}
