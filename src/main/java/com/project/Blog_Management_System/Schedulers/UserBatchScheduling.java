package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserBatchScheduling {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void reconcileUsersInBatches() {
        int page = 0;
        int size = 1000;

        Slice<UserEntity> users;

        do {
            users = userRepository.findAll(PageRequest.of(page, size));

            for (UserEntity user : users) {
                Integer postCount = postRepository.countByUser(user);
                Integer followerCount = followRepository.countByFollowing(user);
                Integer followingCount = followRepository.countByFollower(user);

                user.setNoOfPosts(postCount);
                user.setNoOfFollowers(followerCount);
                user.setNoOfFollowings(followingCount);
            }

            userRepository.saveAll(users.getContent());

            page++;

        } while (!users.isLast());

    }

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteUsersInBatches() {
        int page = 0;
        int size = 1000;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(15);

        Slice<UserEntity> users;
        do {
            users = userRepository.findInactiveUsers(cutoff, PageRequest.of(page, size));

            for (UserEntity user : users) {
                followRepository.deleteByFollowerOrFollowing(user, user);
                user.setName("Deleted User");
                user.setUsername("deleted_user_" + user.getId());
                user.setEmail(null);
                user.setPassword(null);
                user.setBio(null);
                user.setGender(null);
                user.setDateOfBirth(null);
                user.setNoOfFollowings(0);
                user.setNoOfFollowers(0);
                user.setNoOfPosts(0);
                user.setIsDeleted(true);
                user.setRoles(null);
            }

            userRepository.saveAll(users.getContent());

            page++;
        } while (!users.isLast());
    }
}
