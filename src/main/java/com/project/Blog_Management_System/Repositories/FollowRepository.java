package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.FollowEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, UUID> {

    Integer countByFollower(UserEntity follower);

    Integer countByFollowing(UserEntity following);

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
    Slice<UserInfoDTO> findFollowers(UUID userId, Pageable pageable);

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
    Slice<UserInfoDTO> findFollowing(UUID userId, Pageable pageable);

    void deleteByFollowerAndFollowing(UserEntity follower, UserEntity followee);

    void deleteByFollowerOrFollowing(UserEntity follower, UserEntity followee);

    @ReadFast
    Optional<FollowEntity> findByFollowerAndFollowing(UserEntity follower, UserEntity followee);

}
