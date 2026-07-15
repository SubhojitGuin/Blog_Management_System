package com.project.Blog_Management_System;

import com.project.Blog_Management_System.Advice.ApiError;
import com.project.Blog_Management_System.Annotations.WithMockBlogUser;
import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Enums.Role;
import com.project.Blog_Management_System.Events.NewPostPublishedEvent;
import com.project.Blog_Management_System.Repositories.*;
import com.project.Blog_Management_System.Utils.TestPageResponse;
import com.project.Blog_Management_System.Utils.TestSliceResponse;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Post Management")
@RecordApplicationEvents
public class PostControllerIT extends BaseIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private LikeRepository likeRepository;

    private UserEntity user;
    private UserEntity adminUser;

    private static final String USERNAME = "testuser";
    private static final String ADMIN_USERNAME = "testadmin";

    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(testDataFactory.createUser()
                .username(USERNAME)
                .build());

        adminUser = userRepository.saveAndFlush(testDataFactory.createUser()
                .username(ADMIN_USERNAME)
                .email("admin@gmail.com")
                .roles(Set.of(Role.ADMIN))
                .build());

        category = categoryRepository.saveAndFlush(testDataFactory.createCategory()
                .build());
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.POSTS_BASE_PATH)
    @Story("User creates post")
    @Severity(SeverityLevel.CRITICAL)
    class CreatePost {

        @Test
        @DisplayName("Should return 201 when the PUBLISHED post is created successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn201WhenThePUBLISHEDPostIsCreatedSuccessfully() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Test Post Title")
                    .description("This is a test post description with more than fifty characters.")
                    .content("<p>This is a test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.PUBLISHED)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn();

            PostResponseDTO postResponseDTO = testResponseExtractor.extractPayload(response, PostResponseDTO.class);

            assertThat(postResponseDTO.getId()).isNotNull();
            assertThat(postResponseDTO.getTitle()).isEqualTo(postRequestDTO.getTitle());
            assertThat(postResponseDTO.getDescription()).isEqualTo(postRequestDTO.getDescription());
            assertThat(postResponseDTO.getContent()).isEqualTo(postRequestDTO.getContent());
            assertThat(postResponseDTO.getSlug()).isEqualTo("test-post-title");
            assertThat(postResponseDTO.getReadingTimeMinutes()).isEqualTo(1);
            assertThat(postResponseDTO.getLikeCount()).isEqualTo(0);
            assertThat(postResponseDTO.getCommentCount()).isEqualTo(0);
            assertThat(postResponseDTO.getViewCount()).isEqualTo(0L);
            assertThat(postResponseDTO.getCategory().getSlug()).isEqualTo(postRequestDTO.getCategorySlug());
            assertThat(postResponseDTO.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(postResponseDTO.getIsOwner()).isTrue();
            assertThat(postResponseDTO.getIsLiked()).isFalse();

            assertThat(postRepository.findById(postResponseDTO.getId())).isPresent();

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(1);

            NewPostPublishedEvent publishedEvent = applicationEvents.stream(NewPostPublishedEvent.class)
                    .findFirst()
                    .orElseThrow();

            assertThat(publishedEvent.postTitle()).isEqualTo(postRequestDTO.getTitle());
            assertThat(publishedEvent.authorName()).isEqualTo(user.getName());
        }

        @Test
        @DisplayName("Should return 201 when the DRAFT post is created successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn201WhenTheDRAFTPostIsCreatedSuccessfully() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Test Post Title")
                    .description("This is a test post description with more than fifty characters.")
                    .content("<p>This is a test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn();

            PostResponseDTO postResponseDTO = testResponseExtractor.extractPayload(response, PostResponseDTO.class);

            assertThat(postResponseDTO.getId()).isNotNull();
            assertThat(postResponseDTO.getTitle()).isEqualTo(postRequestDTO.getTitle());
            assertThat(postResponseDTO.getDescription()).isEqualTo(postRequestDTO.getDescription());
            assertThat(postResponseDTO.getContent()).isEqualTo(postRequestDTO.getContent());
            assertThat(postResponseDTO.getSlug()).isEqualTo("test-post-title");
            assertThat(postResponseDTO.getReadingTimeMinutes()).isEqualTo(1);
            assertThat(postResponseDTO.getLikeCount()).isEqualTo(0);
            assertThat(postResponseDTO.getCommentCount()).isEqualTo(0);
            assertThat(postResponseDTO.getViewCount()).isEqualTo(0L);
            assertThat(postResponseDTO.getCategory().getSlug()).isEqualTo(postRequestDTO.getCategorySlug());
            assertThat(postResponseDTO.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(postResponseDTO.getIsOwner()).isTrue();
            assertThat(postResponseDTO.getIsLiked()).isFalse();

            assertThat(postRepository.findById(postResponseDTO.getId())).isPresent();

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 400 when SCHEDULED post is created and publishAt is null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenSCHEDULEDPostIsCreatedAndPublishAtIsNull() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Test Post Title")
                    .description("This is a test post description with more than fifty characters.")
                    .content("<p>This is a test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.SCHEDULED)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.publish_at_required_for_scheduled_status"));

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 400 for invalid input data")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400ForInvalidInputData() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("T")
                    .description("This is a test post description.")
                    .content("<p>This is a test post content.</p>")
                    .categorySlug("incorrect slug")
                    .status(PostStatus.DRAFT)
                    .publishAt(LocalDateTime.now().minusYears(1))
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(5)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("title", messageService.get("validation.post.title.size")
                                    .replace("{min}", String.valueOf(PostRequestDTO.TITLE_MIN))
                                    .replace("{max}", String.valueOf(PostRequestDTO.TITLE_MAX))),
                            tuple("description", messageService.get("validation.post.description.size")
                                    .replace("{max}", String.valueOf(PostRequestDTO.DESCRIPTION_MAX))
                                    .replace("{min}", String.valueOf(PostRequestDTO.DESCRIPTION_MIN))),
                            tuple("content", messageService.get("validation.post.content.size")
                                    .replace("{min}", String.valueOf(PostRequestDTO.CONTENT_MIN))
                                    .replace("{max}", String.valueOf(PostRequestDTO.CONTENT_MAX))),
                            tuple("categorySlug", messageService.get("validation.category.slug")),
                            tuple("publishAt", messageService.get("validation.post.publish_at.future"))
                    );

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 400 for blank input fields")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400ForBlankInputFields() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(3)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("title", messageService.get("validation.post.title.not_blank")),
                            tuple("description", messageService.get("validation.post.description.not_blank")),
                            tuple("content", messageService.get("validation.post.content.not_blank"))
                    );

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 404 when the category does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheCategoryDoesNotExist()  throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Test Post Title")
                    .description("This is a test post description with more than fifty characters.")
                    .content("<p>This is a test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug("non-existent-category")
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 401 when user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Test Post Title")
                    .description("This is a test post description with more than fifty characters.")
                    .content("<p>This is a test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.PUBLISHED)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            long eventCount = applicationEvents.stream(NewPostPublishedEvent.class).count();
            assertThat(eventCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH)
    @Story("User retrieves all posts")
    @Severity(SeverityLevel.NORMAL)
    class GetAllPosts {

        @Test
        @DisplayName("Should return 200 when all posts are retrieved successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenAllPostsAreRetrievedSuccessfully() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 1")
                    .user(user)
                    .category(category)
                    .build());

            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 2")
                    .user(user)
                    .category(category)
                    .build());

            postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Draft Post")
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);
            assertThat(postSlice.getContent())
                    .hasSize(2)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(post1.getTitle(), post2.getTitle());
            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(2);
            assertThat(postSlice.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return 200 and empty post list in case of no PUBLISHED posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyPostListInCaseOfNoPublishedPosts() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);
            assertThat(postSlice.getContent())
                    .hasSize(0);

            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(0);
            assertThat(postSlice.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should return 401 when user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH)
    @Story("User retrieves all posts of followings")
    @Severity(SeverityLevel.NORMAL)
    class GetAllPostsOfFollowings {

        UserEntity followingUser;

        @BeforeEach
        void addFollower() {
            followingUser = userRepository.saveAndFlush(testDataFactory.createUser()
                    .username("followingUser")
                    .email("followingUser@gmail.com")
                    .build());

            followRepository.saveAndFlush(FollowEntity.builder()
                    .follower(user)
                    .following(followingUser)
                    .build());
        }

        @Test
        @DisplayName("Should return 200 when all posts of followings are retrieved successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200WhenAllPostsOfFollowingsAreRetrievedSuccessfully() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 1")
                    .user(followingUser)
                    .category(category)
                    .build());

            PostEntity post2 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 2")
                    .user(followingUser)
                    .category(category)
                    .build());

            postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 3")
                    .user(followingUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);

            assertThat(postSlice.getContent())
                    .hasSize(2)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(post1.getTitle(), post2.getTitle());
            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(2);
            assertThat(postSlice.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return 200 and empty post list in case of no PUBLISHED posts from followings")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyPostListInCaseOfNoPublishedPostsFromFollowings() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);

            assertThat(postSlice.getContent())
                    .hasSize(0);
            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(0);
            assertThat(postSlice.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should return 401 when user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH)
    @Story("User retrieves all personal posts")
    @Severity(SeverityLevel.NORMAL)
    class GetAllPersonalPosts {

        PostEntity scheduledPost, publishedPost, draftPost;

        @BeforeEach
        void addPosts() {
            scheduledPost = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Scheduled Post")
                    .user(user)
                    .category(category)
                    .status(PostStatus.SCHEDULED)
                    .publishAt(LocalDateTime.now().plusDays(1))
                    .build());

            publishedPost = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Published Post")
                    .user(user)
                    .category(category)
                    .status(PostStatus.PUBLISHED)
                    .build());

            draftPost = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Draft Post")
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
        }

        @Test
        @DisplayName("Should return 200 and all the PUBLISHED posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndAllThePUBLISHEDPostsAreRetrievedSuccessfully() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH)
                            .param("status", PostStatus.PUBLISHED.name()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);

            assertThat(postSlice.getContent())
                    .hasSize(1)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(publishedPost.getTitle());

            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(1);
            assertThat(postSlice.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return 200 and all the DRAFT posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndAllTheDRAFTPostsAreRetrievedSuccessfully() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH)
                            .param("status", PostStatus.DRAFT.name()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);

            assertThat(postSlice.getContent())
                    .hasSize(1)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(draftPost.getTitle());

            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(1);
            assertThat(postSlice.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return 200 and all the SCHEDULED posts")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndAllTheSCHEDULEDPostsAreRetrievedSuccessfully() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH)
                            .param("status", PostStatus.SCHEDULED.name()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<PostInfoDTO> postSlice = testResponseExtractor.extractSlicePayload(response, PostInfoDTO.class);

            assertThat(postSlice.getContent())
                    .hasSize(1)
                    .extracting(PostInfoDTO::getTitle)
                    .containsExactlyInAnyOrder(scheduledPost.getTitle());

            assertThat(postSlice.isLast()).isTrue();
            assertThat(postSlice.isFirst()).isTrue();
            assertThat(postSlice.getNumberOfElements()).isEqualTo(1);
            assertThat(postSlice.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return 400 when the post status is not provided")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenThePostStatusIsNotProvided() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Required request parameter 'status' for method parameter type PostStatus is not present");
        }

        @Test
        @DisplayName("Should return 401 when user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PERSONAL_PATH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
    @Story("User searches posts")
    @Severity(SeverityLevel.NORMAL)
    class SearchPosts {

        PostEntity post1, post2, post3, post4, post5;

        @BeforeEach
        void setup() {
            post2 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 2")
                    .user(user)
                    .category(category)
                    .likeCount(5)
                    .build());

            post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post 1")
                    .user(user)
                    .category(category)
                    .build());

            post3 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post")
                    .user(user)
                    .category(category)
                    .build());

            post4 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post")
                    .user(user)
                    .category(category)
                    .likeCount(5)
                    .build());

            post5 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Post")
                    .user(user)
                    .category(category)
                    .likeCount(5)
                    .build());
        }

        @Test
        @DisplayName("Should return 200 and posts sorted as per the default sort field when the sort field is not provided")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndPostsSortedAsPerTheDefaultSortFieldWhenTheSortFieldIsNotProvided() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH))
                    .andExpect(status().isOk())
                    .andReturn();

            TestPageResponse<PostInfoDTO> pageResponse = testResponseExtractor.extractPagePayload(response, PostInfoDTO.class);

            assertThat(pageResponse.getContent())
                    .hasSize(5)
                    .extracting(PostInfoDTO::getId)
                    .isSortedAccordingTo(Comparator.naturalOrder());

            assertThat(pageResponse.getPage().getTotalElements()).isEqualTo(5);
            assertThat(pageResponse.getPage().getTotalPages()).isEqualTo(1);
            assertThat(pageResponse.getPage().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 200 and empty list when the minReadingTime is greater than maxReadingTime")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndEmptyListWhenTheMinReadingTimeIsGreaterThanMaxReadingTime() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("minReadingTime", "10")
                            .param("maxReadingTime", "5"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestPageResponse<PostInfoDTO> pageResponse = testResponseExtractor.extractPagePayload(response, PostInfoDTO.class);

            assertThat(pageResponse.getContent())
                    .hasSize(0);

            assertThat(pageResponse.getPage().getTotalElements()).isEqualTo(0);
            assertThat(pageResponse.getPage().getTotalPages()).isEqualTo(0);
            assertThat(pageResponse.getPage().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 200 and posts sorted in ascending order with respect to the sort field")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndPostsSortedInAscendingOrderWithRespectToTheSortField() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("sort", "title:asc"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestPageResponse<PostInfoDTO> pageResponse = testResponseExtractor.extractPagePayload(response, PostInfoDTO.class);

            assertThat(pageResponse.getContent())
                    .hasSize(5)
                    .extracting(PostInfoDTO::getTitle)
                    .isSortedAccordingTo(Comparator.naturalOrder());

            assertThat(pageResponse.getPage().getTotalElements()).isEqualTo(5);
            assertThat(pageResponse.getPage().getTotalPages()).isEqualTo(1);
            assertThat(pageResponse.getPage().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 200 and posts sorted in descending order with respect to the sort field")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndPostsSortedInDescendingOrderWithRespectToTheSortField() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("sort", "title:desc"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestPageResponse<PostInfoDTO> pageResponse = testResponseExtractor.extractPagePayload(response, PostInfoDTO.class);

            assertThat(pageResponse.getContent())
                    .hasSize(5)
                    .extracting(PostInfoDTO::getTitle)
                    .isSortedAccordingTo(Comparator.reverseOrder());

            assertThat(pageResponse.getPage().getTotalElements()).isEqualTo(5);
            assertThat(pageResponse.getPage().getTotalPages()).isEqualTo(1);
            assertThat(pageResponse.getPage().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 200 and posts sorted based on sort fields order")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndPostsSortedBasedOnSortFieldOrder() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("sort", "title:asc")
                            .param("sort", "likeCount:asc")
                            .param("sort", "createdAt:desc"))
                    .andExpect(status().isOk())
                    .andReturn();

            TestPageResponse<PostInfoDTO> pageResponse = testResponseExtractor.extractPagePayload(response, PostInfoDTO.class);

            assertThat(pageResponse.getContent())
                    .hasSize(5)
                    .isSortedAccordingTo((post1, post2) -> {
                        if (!post1.getTitle().equals(post2.getTitle()))
                            return post1.getTitle().compareTo(post2.getTitle());

                        if (!post1.getLikeCount().equals(post2.getLikeCount()))
                            return post1.getLikeCount().compareTo(post2.getLikeCount());

                        return post2.getId().compareTo(post1.getId());
                    });

            assertThat(pageResponse.getPage().getTotalElements()).isEqualTo(5);
            assertThat(pageResponse.getPage().getTotalPages()).isEqualTo(1);
            assertThat(pageResponse.getPage().getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 400 when the sort field is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenTheSortFieldIsInvalid() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("sort", "invalid-param:desc"))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError pageResponse = testResponseExtractor.extractError(response);

            assertThat(pageResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.invalid_sort_field", "invalid-param"));
        }

        @Test
        @DisplayName("Should return 400 when the category slug is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenTheCategorySlugIsInvalid() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("categorySlug", "invalid_category"))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("categorySlug", messageService.get("validation.category.slug"))
                    );
        }

        @Test
        @DisplayName("Should return 400 when the maxReadingTime or minReadingTime is negative")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenTheMaxReadingTimeOrMinReadingTimeIsNegative() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("maxReadingTime", "-1"))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("maxReadingTime", messageService.get("validation.post.reading_time.positive_or_zero"))
                    );
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError pageResponse = testResponseExtractor.extractError(response);

            assertThat(pageResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE)
    @Story("User retrieves a post")
    @Severity(SeverityLevel.NORMAL)
    class GetPost {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .title("Test Post")
                    .user(user)
                    .category(category)
                    .build());
        }

        @Test
        @DisplayName("Should return 200 and retrieve the post successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndRetrieveThePostSuccessfully() throws Exception {
           MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

           PostResponseDTO postResponse = testResponseExtractor.extractPayload(response, PostResponseDTO.class);
           assertThat(postResponse.getId()).isEqualTo(post.getId());
           assertThat(postResponse.getTitle()).isEqualTo(post.getTitle());
        }

        @Test
        @DisplayName("Should return 200 ans isOwner status set to true, when a user retrieves their own post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndIsOwnerStatusSetToTrueWhenAUserRetrievesTheirOwnPost() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            PostResponseDTO postResponse = testResponseExtractor.extractPayload(response, PostResponseDTO.class);
            assertThat(postResponse.getId()).isEqualTo(post.getId());
            assertThat(postResponse.getTitle()).isEqualTo(post.getTitle());
            assertThat(postResponse.getIsOwner()).isTrue();
        }

        @Test
        @DisplayName("Should return 200 and likeStatus set to true after it is liked by the user")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndLikeStatusTrueAfterPostIsLikedByTheUser() throws Exception {
            likeRepository.saveAndFlush(LikeEntity.builder()
                    .post(post)
                    .user(user)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            PostResponseDTO postResponse = testResponseExtractor.extractPayload(response, PostResponseDTO.class);
            assertThat(postResponse.getId()).isEqualTo(post.getId());
            assertThat(postResponse.getTitle()).isEqualTo(post.getTitle());
            assertThat(postResponse.getIsLiked()).isTrue();
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, "non-existent-slug", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when trying to retrieve the unpublished post of another author")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTryingToRetrieveTheUnpublishedPostOfAnotherAuthor() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("otherUser", "otheruser@gmail.com", "testPassword@123"));

            PostEntity post = postRepository.saveAndFlush(
                    testDataFactory.createPost()
                            .title("Unpublished Post")
                            .user(otherUser)
                            .category(category)
                            .status(PostStatus.DRAFT)
                            .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 401 when user is unauthenticated")
        void shouldReturn401WhenUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("PUT " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE)
    @Story("User updates post")
    @Severity(SeverityLevel.NORMAL)
    class UpdatePost {

        PostEntity post;

        @BeforeEach
        void addPost() {
            post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .build());
        }

        @Test
        @DisplayName("Should return 200 and the updated post successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheUpdatedPostSuccessfully() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.PUBLISHED)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isOk())
                    .andReturn();

            PostResponseDTO postResponse = testResponseExtractor.extractPayload(response, PostResponseDTO.class);

            assertThat(postResponse.getId()).isEqualTo(post.getId());
            assertThat(postResponse.getTitle()).isEqualTo(postRequestDTO.getTitle());
            assertThat(postResponse.getDescription()).isEqualTo(postRequestDTO.getDescription());
            assertThat(postResponse.getContent()).isEqualTo(postRequestDTO.getContent());
            assertThat(postResponse.getCategory().getSlug()).isEqualTo(postRequestDTO.getCategorySlug());
            assertThat(postResponse.getIsOwner()).isTrue();

            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getCategory().getSlug()).isEqualTo(postRequestDTO.getCategorySlug());
                        assertThat(updatedPost.getStatus()).isEqualTo(postRequestDTO.getStatus());
                    });
        }

        @Test
        @DisplayName("Should return 400, when the input data is invalid")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenTheInputDataIsInvalid() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("T")
                    .description("This is invalid description")
                    .content("This is invalid content")
                    .categorySlug("category_slug")
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(4)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(
                            tuple("title", messageService.get("validation.post.title.size").replace("{min}", String.valueOf(PostRequestDTO.TITLE_MIN)).replace("{max}", String.valueOf(PostRequestDTO.TITLE_MAX))),
                            tuple("description", messageService.get("validation.post.description.size").replace("{min}", String.valueOf(PostRequestDTO.DESCRIPTION_MIN)).replace("{max}", String.valueOf(PostRequestDTO.DESCRIPTION_MAX))),
                            tuple("content", messageService.get("validation.post.content.size").replace("{min}", String.valueOf(PostRequestDTO.CONTENT_MIN)).replace("{max}", String.valueOf(PostRequestDTO.CONTENT_MAX))),
                            tuple("categorySlug", messageService.get("validation.category.slug"))
                    );
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });
        }

        @Test
        @DisplayName("Should return 400 when SCHEDULED post is updated and publishAt is null")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenSCHEDULEDPostIsUpdatedAndPublishAtIsNull() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.SCHEDULED)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.publish_at_required_for_scheduled_status"));
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });
        }

        @Test
        @DisplayName("Should return 403 when the user is not the owner of the PUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn403WhenTheUserIsNotTheOwnerOfThePUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("otherUser", "otheruser@gmail.com", "OtherPassword@123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .build());

            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.auth.access.denied", "update", "post"));
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.PUBLISHED)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, "non-existent-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner of the UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("otherUser", "otheruser@gmail.com", "OtherPassword@123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.PUBLISHED)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });
        }

        @Test
        @DisplayName("Should return 404 when the post is assigned to a non existent category")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostIsAssignedToANonExistentCategory() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug("non-existent-category")
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Category"));
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getCategory().getSlug()).isNotEqualTo(postRequestDTO.getCategorySlug());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });

        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            PostRequestDTO postRequestDTO = PostRequestDTO.builder()
                    .title("Updated Test Post Title")
                    .description("This is an updated test post description with more than fifty characters.")
                    .content("<p>This is an updated test post content with more than one hundred characters. It includes HTML tags and should be sanitized.</p>")
                    .categorySlug(category.getSlug())
                    .status(PostStatus.DRAFT)
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(postRequestDTO)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
            assertThat(postRepository.findById(post.getId())).isPresent()
                    .get()
                    .satisfies(updatedPost -> {
                        assertThat(updatedPost.getTitle()).isNotEqualTo(postRequestDTO.getTitle());
                        assertThat(updatedPost.getDescription()).isNotEqualTo(postRequestDTO.getDescription());
                        assertThat(updatedPost.getContent()).isNotEqualTo(postRequestDTO.getContent());
                        assertThat(updatedPost.getStatus()).isNotEqualTo(postRequestDTO.getStatus());
                    });
        }
    }

    @Nested
    @DisplayName("DELETE" + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE)
    @Story("User deletes post")
    @Severity(SeverityLevel.CRITICAL)
    class DeletePost {

        PostEntity post;
        UserEntity otherUser;
        private static final String OTHERUSER_USERNAME = "testotheruser";

        @BeforeEach
        void addPost() {
            post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .build());

            otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser(OTHERUSER_USERNAME, "other@gmail.com", "OtherPassword@123"));
        }

        @Test
        @DisplayName("Should return 204, when post is deleted successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenPostIsDeletedSuccessfully() throws Exception {
            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isNoContent())
                    .andReturn();

            assertThat(postRepository.findById(post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 204, when a PUBLISHED post is deleted successfully by an ADMIN user")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn204WhenAPUBLISHEDPostIsDeletedSuccessfullyByAnADMINUser() throws Exception {
            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isNoContent())
                    .andReturn();

            assertThat(postRepository.findById(post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 403 when the user is not the owner of the PUBLISHED post and the user doesn't have ADMIN role")
        @WithMockBlogUser(USERNAME)
        void shouldReturn403WhenTheUserIsNotTheOwnerOfThePUBLISHEDPostAndTheUserDoesntHaveADMINRole() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .build());

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post1.getSlug(), post1.getId()))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.auth.access.denied", "delete", "post"));

            assertThat(postRepository.findById(post1.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404, when post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenPostDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, "invalid-post", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner of the UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPost() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post1.getSlug(), post1.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));

            assertThat(postRepository.findById(post1.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 when Admin deletes UNPUBLISHED post")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn404WhenTheAdminDeletesUNPUBLISHEDPost() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post1.getSlug(), post1.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));

            assertThat(postRepository.findById(post1.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, post.getSlug(), post.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
            assertThat(postRepository.findById(post.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH)
    @Story("User retrieves top level comments of a post")
    @Severity(SeverityLevel.NORMAL)
    class FindTopLevelCommentsOfPost {

        PostEntity post;
        CommentEntity comment1, comment2, comment3, comment4;

        @BeforeEach
        void setUp() {
            post = postRepository.saveAndFlush(testDataFactory.createPost().user(user).category(category).build());
            comment1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post, null));
            comment2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            comment3 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            comment4 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post, comment1));
        }

        @Test
        @DisplayName("Should return 200 and the top level comments successfully of the PUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheTopLevelCommentsSuccessfullyOfThePUBLISHEDPost() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<CommentResponseDTO> slice = testResponseExtractor.extractSlicePayload(response, CommentResponseDTO.class);
            assertThat(slice.getContent()).hasSize(3)
                    .extracting(CommentResponseDTO::getId)
                    .containsExactlyInAnyOrder(comment1.getId(), comment2.getId(), comment3.getId());
        }

        @Test
        @DisplayName("Should return 200 and the top level comments successfully of the UNPUBLISHED post when the user is the owner of the post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTopLevelCommentsSuccessfullyOfUNPUBLISHEDPostWhenUserIsTheOwnerOfPost() throws Exception {
            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentEntity comment1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post1, null));
            CommentEntity comment2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post1, null));
            CommentEntity comment3 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post1, null));
            CommentEntity comment4 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post1, comment1));

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post1.getSlug(), post1.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<CommentResponseDTO> slice = testResponseExtractor.extractSlicePayload(response, CommentResponseDTO.class);
            assertThat(slice.getContent()).hasSize(3)
                    .extracting(CommentResponseDTO::getId)
                    .containsExactlyInAnyOrder(comment1.getId(), comment2.getId(), comment3.getId());
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, "invalid-slug", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner of the UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("otherUser", "otherUser@example.com", "OtherPassword@123"));

            PostEntity post1 = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentEntity comment1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post1, null));
            CommentEntity comment2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post1, null));
            CommentEntity comment3 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post1, null));
            CommentEntity comment4 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(adminUser, post1, comment1));

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post1.getSlug(), post1.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));

        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH)
    @Story("User retrieves replies of comment")
    @Severity(SeverityLevel.NORMAL)
    class FindRepliesOfComment {

        PostEntity post;
        CommentEntity parentComment;
        CommentEntity reply1, reply2;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
            parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            reply1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));
            reply2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));
        }


        @Test
        @DisplayName("Should return 200 and the replies of the comment successfully of the PUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheRepliesOfTheCommentSuccessfullyOfThePUBLISHEDPost() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<CommentResponseDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, CommentResponseDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(2)
                    .extracting(CommentResponseDTO::getId)
                    .containsExactlyInAnyOrder(reply1.getId(), reply2.getId());
        }

        @Test
        @DisplayName("Should return 200 and the replies of the comment successfully of the UNPUBLISHED post when the user is the owner of the post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheRepliesOfTheCommentSuccessfullyOfTheUNPUBLISHEDPostWhenTheUserIsTheOwnerOfThePost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
            CommentEntity parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            CommentEntity reply1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));
            CommentEntity reply2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<CommentResponseDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, CommentResponseDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(2)
                    .extracting(CommentResponseDTO::getId)
                    .containsExactlyInAnyOrder(reply1.getId(), reply2.getId());
        }

        @Test
        @DisplayName("Should return 404 when post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, "invalid-slug", UUID.randomUUID(), parentComment.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when parent comment does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheParentCommentDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Comment"));
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner of the UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("otheruser", "other@gmail.com", "OtherPassword@123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentEntity parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            CommentEntity reply1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));
            CommentEntity reply2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 400 in case of Parent comment and post mismatch")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400InCaseOfParentCommentAndPostMismatch() throws Exception {
            PostEntity anotherPost = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
            CommentEntity anotherParentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, anotherPost, null));
            CommentEntity reply1 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, anotherPost, anotherParentComment));
            CommentEntity reply2 = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, anotherPost, anotherParentComment));

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), anotherParentComment.getId()))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.parent_comment_post_mismatch"));
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("POST" + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH)
    @Story("User adds a top level comment to post")
    @Severity(SeverityLevel.NORMAL)
    class AddTopLevelComment {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 201 and the created comment successfully for the PUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn201AndTheCreatedCommentSuccessfullyForThePUBLISHEDPost() throws Exception {
            CommentRequestDTO comment = CommentRequestDTO.builder()
                    .body("This is a test comment for the PUBLISHED post.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isCreated())
                    .andReturn();

            CommentResponseDTO commentResponse = testResponseExtractor.extractPayload(response, CommentResponseDTO.class);

            assertThat(commentResponse.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(commentResponse.getIsAuthor()).isTrue();
            assertThat(commentResponse.getBody()).isEqualTo(comment.getBody());
            assertThat(commentResponse.getParentId()).isNull();
            assertThat(commentResponse.getHasReplies()).isFalse();

            assertThat(commentRepository.findById(commentResponse.getId()))
                    .isPresent()
                    .get()
                    .satisfies(savedComment -> {
                        assertThat(savedComment.getBody()).isEqualTo(comment.getBody());
                        assertThat(savedComment.getUser().getUsername()).isEqualTo(USERNAME);
                        assertThat(savedComment.getPost().getId()).isEqualTo(post.getId());
                        assertThat(savedComment.getParent()).isNull();
                    });
        }

        @Test
        @DisplayName("Should return 400 when input data is invalid")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenInputDataIsInvalid() throws Exception {
            CommentRequestDTO comment = CommentRequestDTO.builder().build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("body", messageService.get("validation.comment.body.not_blank")));
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            CommentRequestDTO comment = CommentRequestDTO.builder()
                    .body("This is a test comment for the PUBLISHED post.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, "invalid-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));

        }

        @Test
        @DisplayName("Should return 404, when non author adds comment to an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheNonAuthorAddsCommentToUNPUBLISHEDPost() throws Exception {
            UserEntity anotherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("anotheruser", "another@gmail.com", "Another@123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(anotherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentRequestDTO comment = CommentRequestDTO.builder()
                    .body("This is a test comment for the UNPUBLISHED post.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 406, when author adds comment to unpublished post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenAuthorAddsCommentToUnpublishedPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentRequestDTO comment = CommentRequestDTO.builder()
                    .body("This is a test comment for the UNPUBLISHED post.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            CommentRequestDTO comment = CommentRequestDTO.builder()
                    .body("This is a test comment for the UNPUBLISHED post.")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(comment)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH)
    @Story("User adds a reply to a comment")
    @Severity(SeverityLevel.NORMAL)
    class AddReplyToComment {

        PostEntity post;
        CommentEntity parentComment;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
            parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
        }

        @Test
        @DisplayName("Should return 201, when a reply to a top level comment is added successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn201AndTheCreatedCommentSuccessfullyForTheTopLevelCommentOfThePUBLISHEDPost() throws Exception {
            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            CommentResponseDTO commentResponse = testResponseExtractor.extractPayload(response, CommentResponseDTO.class);

            assertThat(commentResponse).isNotNull();
            assertThat(commentResponse.getBody()).isEqualTo(commentRequest.getBody());
            assertThat(commentResponse.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(commentResponse.getParentId()).isEqualTo(parentComment.getId());
            assertThat(commentResponse.getHasReplies()).isFalse();
            assertThat(commentResponse.getIsAuthor()).isTrue();

            assertThat(commentRepository.findById(commentResponse.getId())).isPresent()
                    .get()
                    .satisfies(replyComment -> {
                        assertThat(replyComment.getBody()).isEqualTo(commentRequest.getBody());
                        assertThat(replyComment.getUser().getUsername()).isEqualTo(USERNAME);
                        assertThat(replyComment.getPost().getId()).isEqualTo(post.getId());
                        assertThat(replyComment.getParent().getId()).isEqualTo(parentComment.getId());
                        assertThat(replyComment.getDepth()).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("Should return 404 when post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, "invalid-slug", UUID.randomUUID(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when parent comment does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheParentCommentDoesNotExist() throws Exception {
            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Comment"));
        }

        @Test
        @DisplayName("Should return 404, when non author adds a reply to top level comment of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheNonAuthorAddsReplyToTopLevelCommentToUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
            CommentEntity parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 406, when author adds reply to a top level comment to UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenAuthorAddsReplyToTopLevelCommentToUNPUBLISHEDPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
            CommentEntity parentComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));
        }

        @Test
        @DisplayName("Should return 400 in case of Parent comment and post mismatch")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400InCaseOfParentCommentAndPostMismatch() throws Exception {
            PostEntity otherPost = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, otherPost.getSlug(), otherPost.getId(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.parent_comment_post_mismatch"));
        }

        @Test
        @DisplayName("Should return 400, when user tries to add a reply to another comment reply")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenUserTriesToAddAnotherCommentReply() throws Exception {
            CommentEntity childComment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, parentComment));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), childComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.illegal.argument.invalid_comment_depth"));
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is a test reply comment")
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, post.getSlug(), post.getId(), parentComment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("PUT " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH)
    @Story("User updates a comment")
    @Severity(SeverityLevel.NORMAL)
    class UpdateComment {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 200 and the updated comment successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheUpdatedCommentSuccessfully() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));
            commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, comment));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is the updated Comment")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            CommentResponseDTO commentResponse = testResponseExtractor.extractPayload(response, CommentResponseDTO.class);
            assertThat(commentResponse.getBody()).isEqualTo(commentRequest.getBody());
            assertThat(commentResponse.getUser().getUsername()).isEqualTo(USERNAME);
            assertThat(commentResponse.getParentId()).isNull();
            assertThat(commentResponse.getHasReplies()).isTrue();
            assertThat(commentResponse.getIsAuthor()).isTrue();

            assertThat(commentRepository.findById(commentResponse.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(commentRequest.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 400 when the input data is invalid")
        @WithMockBlogUser(USERNAME)
        void shouldReturn400WhenTheInputDataIsInvalid() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder().build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("body", messageService.get("validation.comment.body.not_blank")));

            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is the updated comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, "nonexistent-post-slug", UUID.randomUUID(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 404 when a non-author user tries to update a comment of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheNonAuthorUserTriesToUpdateAnUnpublishedPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is an updated comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 404 when the comment does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheCommentDoesNotExist() throws Exception {
            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Comment"));
        }

        @Test
        @DisplayName("Should return 406 when the author updates the comment of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenAuthorUpdatesCommentOfUnpublishedPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 403 when when the user is not author of the comment")
        @WithMockBlogUser(USERNAME)
        void shouldReturn403WhenTheUserIsNotAuthorOfTheComment() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(otherUser, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.auth.access.denied", "update", "comment"));

            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            CommentRequestDTO commentRequest = CommentRequestDTO.builder()
                    .body("This is comment body")
                    .build();

            MvcResult response = mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(commentRepository.findById(comment.getId())).isPresent()
                    .get()
                    .satisfies(updatedComment -> {
                        assertThat(updatedComment.getBody()).isEqualTo(comment.getBody());
                    });
        }
    }

    @Nested
    @DisplayName("DELETE " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH)
    @Story("User deletes a comment")
    @Severity(SeverityLevel.NORMAL)
    class DeleteComment {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 204, when comment of a PUBLISHED post is deleted successfully by the author")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenCommentOfAPUBLISHEDPostIsDeletedSuccessfullyByTheAuthor() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                            .andExpect(status().isNoContent());

            assertThat(commentRepository.findById(comment.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 204, when comment of a PUBLISHED post is deleted successfully by an ADMIN")
        @WithMockBlogUser(ADMIN_USERNAME)
        void shouldReturn204WhenCommentOfAPUBLISHEDPostIsDeletedSuccessfullyByADMIN() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.findById(comment.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, "invalid-slug", post.getId(), comment.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 when a non-author user tries to delete a comment of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheNonAuthorUserTriesToDeleteAnUnpublishedPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(otherUser, post, null));

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 when the comment does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheCommentDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Comment"));
        }

        @Test
        @DisplayName("Should return 406 when the author deletes the comment of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenAuthorDeletesCommentOfUnpublishedPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));
            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 403 when when the user is not author of the comment")
        @WithMockBlogUser(USERNAME)
        void shouldReturn403WhenTheUserIsNotAuthorOfTheComment() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(otherUser, post, null));

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                    .andExpect(status().isForbidden())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.auth.access.denied", "delete", "comment"));
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            CommentEntity comment = commentRepository.saveAndFlush(testDataFactory.createCustomComment(user, post, null));

            MvcResult response = mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, post.getSlug(), post.getId(), comment.getId()))
                            .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");

            assertThat(commentRepository.findById(comment.getId())).isPresent();

        }
    }


    @Nested
    @DisplayName("GET " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH)
    @Story("User retrieves likes of a post")
    @Severity(SeverityLevel.NORMAL)
    class GetLikesOfPost{

        PostEntity post;

        @BeforeEach
        void setUp() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 200 and the likes of the post successfully")
        @WithMockBlogUser(USERNAME)
        void shouldReturn200AndTheLikesOfThePostSuccessfully() throws Exception {
            LikeEntity like1 = likeRepository.saveAndFlush(LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            LikeEntity like2 = likeRepository.saveAndFlush(LikeEntity.builder()
                    .user(adminUser)
                    .post(post)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            TestSliceResponse<LikeInfoDTO> sliceResponse = testResponseExtractor.extractSlicePayload(response, LikeInfoDTO.class);

            assertThat(sliceResponse.getContent())
                    .hasSize(2)
                    .extracting(LikeInfoDTO::getLikeId)
                    .containsExactlyInAnyOrder(like1.getId(), like2.getId());
        }

        @Test
        @DisplayName("Should return 404 when the post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, "invalid-slug", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when a non author tries to retrieve likes of an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenNonAuthorRetrievesLikesOfUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            LikeEntity like = likeRepository.saveAndFlush(LikeEntity.builder()
                    .user(otherUser)
                    .post(post)
                    .build());

            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId()))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            MvcResult response = mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId()))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH)
    @Story("User likes or dislikes a post")
    @Severity(SeverityLevel.NORMAL)
    class LikeOrDislikePost {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 204 when the user successfully likes a post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserSuccessfullyLikesPost() throws Exception {
            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNoContent());

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user likes a liked post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserLikesALikedPost() throws Exception {
            likeRepository.save(LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNoContent());

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user successfully dislikes a post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserSuccessfullyDislikesPost() throws Exception {
            likeRepository.save(LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(false)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNoContent());

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user dislikes a disliked post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserDislikesADislikedPost() throws Exception {
            LikeDTO like = LikeDTO.builder()
                    .like(false)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNoContent());

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 400 when input data is invalid")
        @WithMockBlogUser(USERNAME)
        void  shouldReturn400WhenTheInputDataIsInvalid() throws Exception {
            LikeDTO like = LikeDTO.builder()
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.validation.failed"));
            assertThat(errorResponse.getSubErrors())
                    .hasSize(1)
                    .extracting(ApiError.FieldError::getField, ApiError.FieldError::getMessage)
                    .containsExactlyInAnyOrder(tuple("like", messageService.get("validation.like.like.not_null")));
        }

        @Test
        @DisplayName("Should return 404 when post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, "invalid-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner and likes an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner and dislikes an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerOfTheUNPUBLISHEDPostWhileDisliking() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));

            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            likeRepository.save(LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(false)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 406 when the user is the owner and likes an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenTheUserIsTheOwnerOfTheUNPUBLISHEDPostWhileLiking() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 406 when the user is the owner and dislikes an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenTheUserIsTheOwnerOfTheUNPUBLISHEDPostWhileDisliking() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            likeRepository.save(LikeEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            LikeDTO like = LikeDTO.builder()
                    .like(false)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

            assertThat(likeRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            LikeDTO like = LikeDTO.builder()
                    .like(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(like)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }

    @Nested
    @DisplayName("POST " + ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH)
    @Story("User bookmarks or unbookmarks a post")
    @Severity(SeverityLevel.NORMAL)
    class BookmarkOrUnbookmarkPost {

        PostEntity post;

        @BeforeEach
        void setup() {
            post = postRepository.saveAndFlush(testDataFactory.createCustomPost(user, category));
        }

        @Test
        @DisplayName("Should return 204 when the user successfully bookmarks a post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserSuccessfullyBookmarksPost() throws Exception {
            BookmarkDTO bookmarkRequest = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmarkRequest)))
                    .andExpect(status().isNoContent());

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user bookmarks a bookmarked post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserBookmarksABookmarkedPost() throws Exception {
            bookmarkRepository.save(BookmarkEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNoContent());

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user successfully unbookmarks a post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserSuccessfullyUnbookmarksPost() throws Exception {
            bookmarkRepository.save(BookmarkEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(false)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNoContent());

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 204 when the user unbookmarks an unbookmarked post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn204WhenTheUserUnbookmarksAUnbookmarkedPost() throws Exception {
            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(false)
                    .build();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNoContent());

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 404 when post does not exist")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenThePostDoesNotExist() throws Exception {
            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, "invalid-slug", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
        }

        @Test
        @DisplayName("Should return 406 when the author bookmarks a UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn406WhenTheAuthorBookmarksABookmarksABookmarkedPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @WithMockBlogUser(USERNAME)
        @DisplayName("Should return 406 when the author unbookmarks a UNPUBLISHED post")
        void shouldReturn406WhenTheAuthorUnbookmarksAUnbookmarkedPost() throws Exception {
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(user)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            bookmarkRepository.saveAndFlush(BookmarkEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(false)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNotAcceptable())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);

            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.invalid.action.unpublished_post"));

            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner and bookmarks an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerAndBookmarksAnUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isNotPresent();
        }

        @Test
        @DisplayName("Should return 404 when the user is not the owner and unbookmarks an UNPUBLISHED post")
        @WithMockBlogUser(USERNAME)
        void shouldReturn404WhenTheUserIsNotTheOwnerAndUnbookmarksAnUNPUBLISHEDPost() throws Exception {
            UserEntity otherUser = userRepository.saveAndFlush(testDataFactory.createCustomUser("OtherUser", "otheruser@gmail.com","other@Password123"));
            PostEntity post = postRepository.saveAndFlush(testDataFactory.createPost()
                    .user(otherUser)
                    .category(category)
                    .status(PostStatus.DRAFT)
                    .build());

            bookmarkRepository.saveAndFlush(BookmarkEntity.builder()
                    .user(user)
                    .post(post)
                    .build());

            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(false)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo(messageService.get("exception.resource.not_found", "Post"));
            assertThat(bookmarkRepository.findByUserIdAndPostId(user.getId(), post.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 401 when the user is unauthenticated")
        void shouldReturn401WhenTheUserIsUnauthenticated() throws Exception {
            BookmarkDTO bookmark = BookmarkDTO.builder()
                    .bookmark(true)
                    .build();

            MvcResult response = mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, post.getSlug(), post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookmark)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            ApiError errorResponse = testResponseExtractor.extractError(response);
            assertThat(errorResponse.getMessage()).isEqualTo("Full authentication is required to access this resource");
        }
    }
}
