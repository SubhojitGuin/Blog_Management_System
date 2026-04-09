package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.LikeEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
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
public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

    @ReadFast
    Optional<LikeEntity> findByUserAndPost(UserEntity user, PostEntity post);

    void deleteByUserAndPost(UserEntity user, PostEntity post);

    @Query("""
                SELECT new com.project.Blog_Management_System.Dto.UserInfoDTO(u.id, u.name, u.username,u.active)
                FROM LikeEntity l
                LEFT JOIN l.user u
                WHERE l.post = :post
            """)
    @ReadFast
    Slice<UserInfoDTO> findLikesOfPost(
            @Param("post") PostEntity post,
            Pageable pageable
    );

}
