package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Entities.LikeEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

    Optional<LikeEntity> findByUserAndPost(UserEntity user, PostEntity post);

}
