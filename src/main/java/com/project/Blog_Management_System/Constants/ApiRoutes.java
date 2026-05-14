package com.project.Blog_Management_System.Constants;

public final class ApiRoutes {

    // Regex Patterns
    private static final String UUID_REGEX = "[0-9a-fA-F\\-]{36}";

    // User Module Paths
    public static final String USERS_BASE_PATH = "/users";
    public static final String USER_UPDATE_PASSWORD_PATH = "/update_password";
    public static final String USER_UPDATE_USERNAME_PATH = "/update_username";
    public static final String USER_UPDATE_EMAIL_PATH = "/update_email";
    public static final String USER_SEARCH_PATH = "/search";
    public static final String USER_BOOKMARKS_PATH = "/bookmarks";
    public static final String USER_PATH_VARIABLE = "/{username}-{user_id:" + UUID_REGEX + "}";
    public static final String USER_FOLLOWERS_PATH = USER_PATH_VARIABLE + "/followers";
    public static final String USER_FOLLOWINGS_PATH = USER_PATH_VARIABLE + "/followings";
    public static final String USER_POSTS_PATH = USER_PATH_VARIABLE + "/posts";
    public static final String USER_FOLLOW_PATH = USER_PATH_VARIABLE + "/follow";

    // Post Module Paths
    public static final String POSTS_BASE_PATH = "/posts";
    public static final String POST_FOLLOWING_PATH = "/following";
    public static final String POST_SEARCH_PATH = "/search";
    public static final String POST_UNPUBLISHED_PATH = "/unpublished";
    public static final String POST_PATH_VARIABLE = "/{post_slug}-{post_id:" + UUID_REGEX + "}";
    public static final String POST_COMMENTS_PATH = POST_PATH_VARIABLE + "/comments";
    public static final String POST_COMMENT_PATH = POST_COMMENTS_PATH + "/{comment_id:" + UUID_REGEX + "}";
    public static final String POST_COMMENT_REPLIES_PATH = POST_COMMENT_PATH + "/replies";
    public static final String POST_LIKES_PATH = POST_PATH_VARIABLE + "/likes";
    public static final String POST_BOOKMARK_PATH = POST_PATH_VARIABLE + "/bookmark";

    // Category Module Paths
    public static final String CATEGORY_BASE_PATH = "/category";
    public static final String CATEGORY_PATH_VARIABLE = "/{category_slug}-{category_id:" + UUID_REGEX + "}";
    public static final String CATEGORY_POSTS = CATEGORY_PATH_VARIABLE + POSTS_BASE_PATH;

    // Admin Module Paths
    public static final String ADMIN_BASE_PATH = "/admin";
    public static final String ADMIN_CATEGORY_PATH = CATEGORY_BASE_PATH + CATEGORY_PATH_VARIABLE;

    // Auth Module Paths
    public static final String AUTH_BASE_PATH = "/auth";
    public static final String AUTH_SIGNUP = "/signup";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_REFRESH = "/refresh";

}
