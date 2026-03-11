package com.project.Blog_Management_System.schedulers;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Repositories.PostRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

        Page<UserEntity> users;

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

        Page<UserEntity> users;
        do {
            users = userRepository.findAll(PageRequest.of(page, size));
            for (UserEntity user : users) {
                if (!user.getActive() && !user.getIsDeleted() && ChronoUnit.DAYS.between(user.getUpdatedAt(), LocalDateTime.now()) >= 15) {
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
            }

            userRepository.saveAll(users.getContent());

            page++;
        } while (!users.isLast());
    }
}
