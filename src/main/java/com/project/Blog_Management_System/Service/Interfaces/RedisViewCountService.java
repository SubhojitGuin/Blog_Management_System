package com.project.Blog_Management_System.Service.Interfaces;

import java.util.UUID;

public interface RedisViewCountService {

    void addViewer(UUID postId, UUID userId);

}
