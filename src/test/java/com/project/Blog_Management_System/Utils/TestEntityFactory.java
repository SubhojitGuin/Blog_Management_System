package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Enums.Role;

import java.util.Set;

public class TestEntityFactory {

    public static UserEntity testUser(String suffix) {
        UserEntity user = new UserEntity();
        user.setName("Test User " + suffix);
        user.setUsername("test-user-" + suffix);
        user.setEmail("test-user-" + suffix + "@example.com");
        user.setPassword("test-password");
        user.setRoles(Set.of(Role.USER));
        return user;
    }

    public static CategoryEntity testCategory(String suffix) {
        CategoryEntity category = new CategoryEntity();
        category.setName("Test Category " + suffix);
        category.setDescription("Test description of category " + suffix);
        category.setSlug("test-category-" + suffix);
        return category;
    }

    public static PostEntity testPost(UserEntity user, CategoryEntity category, String suffix) {
        PostEntity post = new PostEntity();
        post.setTitle("Test Post " + suffix);
        post.setSlug("test-post-" + suffix);
        post.setDescription("Test description of post " + suffix);
        post.setContent("This is the content of post " + suffix + ".");
        post.setUser(user);
        post.setCategory(category);
        return post;
    }

    public static BookmarkEntity testBookmark(UserEntity user, PostEntity post) {
        return BookmarkEntity.builder()
                .user(user)
                .post(post)
                .build();
    }

    public static CommentEntity testTopLevelComment(PostEntity targetPost, UserEntity commentAuthor, String suffix) {
        CommentEntity comment = new CommentEntity();
        comment.setBody("Comment body " + suffix);
        comment.setUser(commentAuthor);
        comment.setPost(targetPost);
        comment.setDepth(0);
        return comment;
    }

    public static CommentEntity testReplyComment(PostEntity targetPost, CommentEntity parent, UserEntity commentAuthor, String suffix) {
        CommentEntity reply = new CommentEntity();
        reply.setBody("Reply body " + suffix);
        reply.setUser(commentAuthor);
        reply.setPost(targetPost);
        reply.setParent(parent);
        reply.setDepth(parent.getDepth() + 1);
        return reply;
    }

    public static FollowEntity testFollow(UserEntity follower, UserEntity following) {
        return FollowEntity.builder()
                .follower(follower)
                .following(following)
                .build();
    }

    public static LikeEntity testLike(UserEntity likeUser, PostEntity targetPost) {
        return LikeEntity.builder()
                .user(likeUser)
                .post(targetPost)
                .build();
    }
}
