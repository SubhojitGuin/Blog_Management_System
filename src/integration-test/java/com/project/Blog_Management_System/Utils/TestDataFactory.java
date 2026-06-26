package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.CategoryEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Gender;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public final class TestDataFactory {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Category Entity Blueprints
     */
    public CategoryEntity.CategoryEntityBuilder createCategory() {
        return CategoryEntity.builder()
                .name("Default Category")
                .description("Default test category description");
    }

    public CategoryEntity createCustomCategory(String name, String description) {
        return createCategory()
                .name(name)
                .description(description)
                .build();
    }

     /**
     * User Entity Blueprints
     */
    public UserEntity.UserEntityBuilder createUser() {
        return UserEntity.builder()
                .name("Test User")
                .username("testuser")
                .email("testuser@gmail.com")
                .password(passwordEncoder.encode("TestPassword@123"))
                .gender(Gender.OTHERS)
                .roles(Set.of(Role.USER))
                .noOfFollowers(0)
                .noOfFollowings(0)
                .noOfPosts(0)
                .active(true)
                .isDeleted(false)
                .tokenVersion(0)
                .dateOfBirth(LocalDate.now().minusYears(20));
    }

    public UserEntity createCustomUser(String username, String email, String password) {
        return createUser()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
    }

    /**
     * Post Entity Blueprints
     */
    public PostEntity.PostEntityBuilder createPost() {
        return PostEntity.builder()
                .title("Test title")
                .description("Test description")
                .content("This is test content")
                .likeCount(0)
                .commentCount(0)
                .viewCount(0L)
                .status(PostStatus.PUBLISHED);
    }

    public PostEntity createCustomPost(UserEntity user, CategoryEntity category) {
        return createPost()
                .user(user)
                .category(category)
                .build();
    }

}
