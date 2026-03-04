package com.project.Blog_Management_System.Repositories;

import com.project.Blog_Management_System.Entities.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, UUID> {
}
