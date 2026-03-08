package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Dto.UserInfoDTO;
import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    List<UserInfoDTO> findByUsernameContainingIgnoreCase(String query, Limit limit);
}
