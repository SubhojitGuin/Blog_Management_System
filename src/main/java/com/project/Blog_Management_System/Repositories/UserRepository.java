package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.annotations.ReadFast;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @ReadFast
    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    @ReadFast
    Optional<UserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @ReadFast
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT new com.project.Blog_Management_System.Dto.UserInfoDTO(
                u.id,
                u.name,
                u.username,
                u.active
            )
            FROM UserEntity u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    @ReadFast
    List<UserInfoDTO> findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT u
            FROM UserEntity u
            WHERE u.active = false
              AND u.isDeleted = false
              AND u.updatedAt <= :cutoff
            ORDER BY u.updatedAt ASC
            """)
    Slice<UserEntity> findInactiveUsers(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfPosts = u.noOfPosts + 1
                WHERE u.id = :userId
            """)
    int incrementPostCount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfPosts = u.noOfPosts - 1
                WHERE u.id = :userId
            """)
    int decrementPostCount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfFollowers = u.noOfFollowers + 1
                WHERE u.id = :userId
            """)
    int incrementFollowersCount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfFollowers = u.noOfFollowers - 1
                WHERE u.id = :userId
            """)
    int decrementFollowersCount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfFollowings = u.noOfFollowings + 1
                WHERE u.id = :userId
            """)
    int incrementFollowingsCount(@Param("userId") UUID userId);

    @Modifying
    @Query("""
                UPDATE UserEntity u
                SET u.noOfFollowings = u.noOfFollowings - 1
                WHERE u.id = :userId
            """)
    int decrementFollowingsCount(@Param("userId") UUID userId);
}
