package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

}
