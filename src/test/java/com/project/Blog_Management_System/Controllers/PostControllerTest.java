package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.Constants.ApiRoutes;
import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Security.JWTService;
import com.project.Blog_Management_System.Security.WebSecurityConfig;
import com.project.Blog_Management_System.Service.Interfaces.PostService;
import com.project.Blog_Management_System.Service.Interfaces.UserService;
import com.project.Blog_Management_System.Utils.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import(WebSecurityConfig.class)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("searchPosts(PostFilterRequestDTO, List, int, int)")
    class SearchPosts {

        @Test
        @DisplayName("returns 200 OK with paged search results for valid filters")
        @WithMockUser(username = "reader")
        void returnsPagedSearchResultsWhenFilterValid() throws Exception {
            PostInfoDTO info = new PostInfoDTO();
            info.setId(UUID.randomUUID());
            info.setTitle("Search Result");

            Page<PostInfoDTO> page = new PageImpl<>(List.of(info));

            when(postService.searchPosts(any(PostFilterRequestDTO.class), eq(0), eq(10), any())).thenReturn(page);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("query", "keyword")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(info.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value(info.getTitle()));
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForSearch() throws Exception {
            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_SEARCH_PATH)
                            .param("query", "keyword")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getPost(String, UUID)")
    class GetPost {

        @Test
        @DisplayName("returns 200 OK with post details for valid slug and id")
        @WithMockUser(username = "reader")
        void returnsPostDetailsSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID id = UUID.randomUUID();

            PostResponseDTO response = new PostResponseDTO();
            response.setId(id);
            response.setTitle("Sample Post");
            response.setSlug(slug);

            when(postService.getPost(eq(slug), eq(id))).thenReturn(response);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.title").value("Sample Post"))
                    .andExpect(jsonPath("$.data.slug").value(slug));
        }

        @Test
        @DisplayName("returns 404 Not Found when post does not exist")
        @WithMockUser(username = "reader")
        void returnsNotFoundWhenPostMissing() throws Exception {
            String slug = "missing-post";
            UUID id = UUID.randomUUID();

            when(postService.getPost(eq(slug), eq(id))).thenThrow(new ResourceNotFoundException("Post not found"));

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForGetPost() throws Exception {
            String slug = "sample-post";
            UUID id = UUID.randomUUID();

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("updatePost(PostRequestDTO, String, UUID)")
    class UpdatePost {

        @Test
        @DisplayName("returns 200 OK when post is updated successfully")
        @WithMockUser(username = "author")
        void updatesPostSuccessfully() throws Exception {
            String slug = "existing-post";
            UUID id = UUID.randomUUID();

            PostRequestDTO request = new PostRequestDTO();
            request.setTitle("Updated Title");
            request.setContent("Some valid content for the post. This should be long enough to pass validation. Ensure it has more than 100 characters. Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
            request.setDescription("A brief description of the post. This should also be long enough to pass validation. Lorem ipsum dolor sit amet, consectetur adipiscing elit.");

            PostResponseDTO response = new PostResponseDTO();
            response.setId(id);
            response.setTitle(request.getTitle());
            response.setSlug(slug);
            response.setContent(request.getContent());
            response.setDescription(request.getDescription());

            when(postService.updatePost(eq(slug), eq(id), any(PostRequestDTO.class))).thenReturn(response);

            mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.title").value(request.getTitle()))
                    .andExpect(jsonPath("$.data.slug").value(slug))
                    .andExpect(jsonPath("$.data.content").value(request.getContent()));
        }

        @Test
        @DisplayName("returns 400 Bad Request when update payload invalid")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenUpdatePayloadInvalid() throws Exception {
            String slug = "existing-post";
            UUID id = UUID.randomUUID();

            PostRequestDTO invalid = new PostRequestDTO();
            invalid.setTitle("");

            mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deletePost(String, UUID)")
    class DeletePost {

        @Test
        @DisplayName("returns 204 No Content when post is deleted successfully")
        @WithMockUser(username = "author")
        void deletesPostSuccessfully() throws Exception {
            String slug = "old-post";
            UUID id = UUID.randomUUID();

            doNothing().when(postService).deletePost(eq(slug), eq(id));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 Not Found when post to delete does not exist")
        @WithMockUser(username = "author")
        void returnsNotFoundWhenDeletingMissingPost() throws Exception {
            String slug = "missing-post";
            UUID id = UUID.randomUUID();

            doThrow(new ResourceNotFoundException("Post not found")).when(postService).deletePost(eq(slug), eq(id));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForDelete() throws Exception {
            String slug = "old-post";
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_PATH_VARIABLE, slug, id.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("createPost(PostRequestDTO)")
    class CreatePost {

        @Test
        @DisplayName("returns 201 Created when post is created successfully")
        @WithMockUser(username = "author")
        void createsPostSuccessfully() throws Exception {
            PostRequestDTO request = new PostRequestDTO();
            request.setTitle("New Post Title");
            request.setCategorySlug("main");
            request.setContent("Some valid content for the post. This should be long enough to pass validation. Ensure it has more than 100 characters. Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
            request.setDescription("A brief description of the post. This should also be long enough to pass validation. Lorem ipsum dolor sit amet, consectetur adipiscing elit.");

            PostResponseDTO response = new PostResponseDTO();
            response.setId(UUID.randomUUID());
            response.setTitle(request.getTitle());
            response.setSlug("new-post-title");
            response.setContent(request.getContent());
            response.setDescription(request.getDescription());

            when(postService.createPost(any(PostRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.data.title").value(response.getTitle()))
                    .andExpect(jsonPath("$.data.slug").value(response.getSlug()))
                    .andExpect(jsonPath("$.data.content").value(response.getContent()))
                    .andExpect(jsonPath("$.data.description").value(response.getDescription()));
        }

        @Test
        @DisplayName("returns 400 Bad Request when required fields are missing")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenMissingFields() throws Exception {
            PostRequestDTO invalid = new PostRequestDTO();
            invalid.setContent("No title provided");

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForCreate() throws Exception {
            PostRequestDTO request = new PostRequestDTO();
            request.setTitle("Title");
            request.setContent("Content");

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getAllPosts(UUID, Integer)")
    class GetAllPosts {

        @Test
        @DisplayName("returns 200 OK with paginated posts using default size")
        @WithMockUser(username = "reader")
        void returnsPaginatedPostsWithDefaultSize() throws Exception {
            PostInfoDTO info = new PostInfoDTO();
            info.setId(UUID.randomUUID());
            info.setTitle("Info Post");

            Slice<PostInfoDTO> slice = new SliceImpl<>(List.of(info));

            when(postService.getAllPosts(any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(info.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value(info.getTitle()))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("returns 200 OK with paginated posts using custom size parameter")
        @WithMockUser(username = "reader")
        void returnsPaginatedPostsWithCustomSize() throws Exception {
            Slice<PostInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getAllPosts(any(), eq(20))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH)
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("returns 200 OK when post_cursor parameter is provided")
        @WithMockUser(username = "reader")
        void returnsPaginatedPostsWithCursor() throws Exception {
            UUID cursor = UUID.randomUUID();
            Slice<PostInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getAllPosts(eq(cursor), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH)
                            .param("post_cursor", cursor.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForGetAll() throws Exception {
            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getAllPostsOfFollowings(UUID, Integer)")
    class GetAllPostsOfFollowings {

        @Test
        @DisplayName("returns 200 OK with followings' posts using default size")
        @WithMockUser(username = "reader")
        void returnsFollowingsPostsSuccessfully() throws Exception {
            PostInfoDTO info = new PostInfoDTO();
            info.setId(UUID.randomUUID());
            info.setTitle("Following Post");

            Slice<PostInfoDTO> slice = new SliceImpl<>(List.of(info));

            when(postService.getAllPostsOfFollowings(any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(info.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value(info.getTitle()));
        }

        @Test
        @DisplayName("returns 200 OK with custom size for followings' posts")
        @WithMockUser(username = "reader")
        void returnsFollowingsPostsWithCustomSize() throws Exception {
            Slice<PostInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getAllPostsOfFollowings(any(), eq(5))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH)
                            .param("size", "5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForFollowings() throws Exception {
            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_FOLLOWING_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("getAllUnpublishedPosts(PostStatus, UUID, Integer)")
    class GetAllUnpublishedPosts {

        @Test
        @DisplayName("returns 200 OK with unpublished posts when status provided")
        @WithMockUser(username = "author")
        void returnsUnpublishedPostsWhenStatusProvided() throws Exception {
            PostInfoDTO info = new PostInfoDTO();
            info.setId(UUID.randomUUID());
            info.setTitle("Draft Post");

            Slice<PostInfoDTO> slice = new SliceImpl<>(List.of(info));

            when(postService.getPostsByStatus(eq(PostStatus.DRAFT), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_UNPUBLISHED_PATH)
                            .param("status", PostStatus.DRAFT.name())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(info.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].title").value(info.getTitle()));
        }

        @Test
        @DisplayName("returns 400 Bad Request when status parameter is missing")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenStatusMissing() throws Exception {
            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_UNPUBLISHED_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 200 OK with custom size when provided")
        @WithMockUser(username = "author")
        void returnsUnpublishedPostsWithCustomSize() throws Exception {
            Slice<PostInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getPostsByStatus(eq(PostStatus.DRAFT), any(), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_UNPUBLISHED_PATH)
                            .param("status", PostStatus.DRAFT.name())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("findTopLevelCommentsOfPost(String, UUID, UUID, int)")
    class FindTopLevelCommentsOfPost {

        @Test
        @DisplayName("returns 200 OK with top level comments for valid post")
        @WithMockUser(username = "reader")
        void returnsTopLevelCommentsSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            CommentResponseDTO comment = new CommentResponseDTO();
            comment.setId(UUID.randomUUID());
            comment.setBody("Great post");
            comment.setHasReplies(true);
            comment.setIsAuthor(false);

            UserInfoDTO user = new UserInfoDTO();
            user.setId(UUID.randomUUID());
            user.setName("Commenter");
            user.setUsername("commenter");
            user.setActive(true);
            comment.setUser(user);

            Slice<CommentResponseDTO> slice = new SliceImpl<>(List.of(comment));

            when(postService.getTopLevelCommentsOfPost(eq(slug), eq(postId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(comment.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].body").value(comment.getBody()))
                    .andExpect(jsonPath("$.data.content[0].user.username").value(user.getUsername()))
                    .andExpect(jsonPath("$.data.content[0].hasReplies").value(true));
        }

        @Test
        @DisplayName("returns 200 OK with top level comments when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsTopLevelCommentsUsingCursorAndCustomSize() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();

            Slice<CommentResponseDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getTopLevelCommentsOfPost(eq(slug), eq(postId), eq(cursor), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, slug, postId.toString())
                            .param("comment_cursor", cursor.toString())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForTopLevelComments() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("findRepliesOfComment(String, UUID, UUID, UUID, int)")
    class FindRepliesOfComment {

        @Test
        @DisplayName("returns 200 OK with replies for a valid parent comment")
        @WithMockUser(username = "reader")
        void returnsRepliesOfCommentSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentResponseDTO reply = new CommentResponseDTO();
            reply.setId(UUID.randomUUID());
            reply.setBody("I agree");
            reply.setParentId(commentId);
            reply.setHasReplies(false);
            reply.setIsAuthor(true);

            UserInfoDTO user = new UserInfoDTO();
            user.setId(UUID.randomUUID());
            user.setName("Reply User");
            user.setUsername("reply-user");
            user.setActive(true);
            reply.setUser(user);

            Slice<CommentResponseDTO> slice = new SliceImpl<>(List.of(reply));

            when(postService.getRepliesOfComment(eq(slug), eq(postId), eq(commentId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(reply.getId().toString()))
                    .andExpect(jsonPath("$.data.content[0].body").value(reply.getBody()))
                    .andExpect(jsonPath("$.data.content[0].parentId").value(commentId.toString()))
                    .andExpect(jsonPath("$.data.content[0].user.username").value(user.getUsername()));
        }

        @Test
        @DisplayName("returns 400 Bad Request when replies are requested for a comment from another post")
        @WithMockUser(username = "reader")
        void returnsBadRequestWhenParentCommentDoesNotBelongToPost() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            when(postService.getRepliesOfComment(eq(slug), eq(postId), eq(commentId), any(), eq(10)))
                    .thenThrow(new IllegalArgumentException("Parent comment does not belong to the provided post"));

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("addTopLevelComment(String, UUID, CommentRequestDTO)")
    class AddTopLevelComment {

        @Test
        @DisplayName("returns 201 Created when a top level comment is added successfully")
        @WithMockUser(username = "author")
        void createsTopLevelCommentSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Nice article");

            CommentResponseDTO response = new CommentResponseDTO();
            response.setId(UUID.randomUUID());
            response.setBody(request.getBody());
            response.setIsAuthor(true);

            when(postService.addTopLevelComments(eq(slug), eq(postId), any(CommentRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.data.body").value(response.getBody()))
                    .andExpect(jsonPath("$.data.isAuthor").value(true));
        }

        @Test
        @DisplayName("returns 400 Bad Request when top level comment body is blank")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenTopLevelCommentBodyIsBlank() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("");

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENTS_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("addReplyToComment(String, UUID, UUID, CommentRequestDTO)")
    class AddReplyToComment {

        @Test
        @DisplayName("returns 201 Created when a reply is added successfully")
        @WithMockUser(username = "author")
        void createsReplySuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Thanks for sharing");

            CommentResponseDTO response = new CommentResponseDTO();
            response.setId(UUID.randomUUID());
            response.setBody(request.getBody());
            response.setParentId(commentId);
            response.setIsAuthor(true);

            when(postService.addReplyToComment(eq(slug), eq(postId), eq(commentId), any(CommentRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                    .andExpect(jsonPath("$.data.body").value(response.getBody()))
                    .andExpect(jsonPath("$.data.parentId").value(commentId.toString()))
                    .andExpect(jsonPath("$.data.isAuthor").value(true));
        }

        @Test
        @DisplayName("returns 400 Bad Request when reply body is too short")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenReplyBodyIsTooShort() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("");

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_REPLIES_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("updateComment(String, UUID, UUID, CommentRequestDTO)")
    class UpdateComment {

        @Test
        @DisplayName("returns 200 OK when a comment is updated successfully")
        @WithMockUser(username = "author")
        void updatesCommentSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Updated comment body");

            CommentResponseDTO response = new CommentResponseDTO();
            response.setId(commentId);
            response.setBody(request.getBody());
            response.setIsAuthor(true);

            when(postService.updateComment(eq(slug), eq(postId), eq(commentId), any(CommentRequestDTO.class))).thenReturn(response);

            mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(commentId.toString()))
                    .andExpect(jsonPath("$.data.body").value(response.getBody()))
                    .andExpect(jsonPath("$.data.isAuthor").value(true));
        }

        @Test
        @DisplayName("returns 404 Not Found when the comment to update does not exist")
        @WithMockUser(username = "author")
        void returnsNotFoundWhenUpdatingMissingComment() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Updated comment body");

            when(postService.updateComment(eq(slug), eq(postId), eq(commentId), any(CommentRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Comment not found"));

            mockMvc.perform(put(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("deleteComment(String, UUID, UUID)")
    class DeleteComment {

        @Test
        @DisplayName("returns 204 No Content when a comment is deleted successfully")
        @WithMockUser(username = "author")
        void deletesCommentSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            doNothing().when(postService).deleteComment(eq(slug), eq(postId), eq(commentId));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 Not Found when the comment to delete does not exist")
        @WithMockUser(username = "author")
        void returnsNotFoundWhenDeletingMissingComment() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            doThrow(new ResourceNotFoundException("Comment not found")).when(postService).deleteComment(eq(slug), eq(postId), eq(commentId));

            mockMvc.perform(delete(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_COMMENT_PATH, slug, postId.toString(), commentId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("getLikesOfPost(String, UUID, UUID, int)")
    class GetLikesOfPost {

        @Test
        @DisplayName("returns 200 OK with liked users for a valid post")
        @WithMockUser(username = "reader")
        void returnsLikesSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            UserInfoDTO user = new UserInfoDTO();
            user.setId(UUID.randomUUID());
            user.setName("Liker");
            user.setUsername("liker");
            user.setActive(true);

            LikeInfoDTO likeInfoDTO = new LikeInfoDTO(user, UUID.randomUUID());
            Slice<LikeInfoDTO> slice = new SliceImpl<>(List.of(likeInfoDTO));

            when(postService.getLikesOfPost(eq(slug), eq(postId), any(), eq(10))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].likeId").value(likeInfoDTO.getLikeId().toString()))
                    .andExpect(jsonPath("$.data.content[0].user.username").value(user.getUsername()));
        }

        @Test
        @DisplayName("returns 200 OK with likes when cursor and custom size are provided")
        @WithMockUser(username = "reader")
        void returnsLikesUsingCursorAndCustomSize() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();
            UUID cursor = UUID.randomUUID();

            Slice<LikeInfoDTO> slice = new SliceImpl<>(Collections.emptyList());

            when(postService.getLikesOfPost(eq(slug), eq(postId), eq(cursor), eq(25))).thenReturn(slice);

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, slug, postId.toString())
                            .param("like_cursor", cursor.toString())
                            .param("size", "25")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("requires authentication and returns 401 when unauthenticated")
        void requiresAuthenticationWhenUnauthenticatedForLikes() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            mockMvc.perform(get(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("likeOrDislikePost(String, UUID, LikeDTO)")
    class LikeOrDislikePost {

        @Test
        @DisplayName("returns 204 No Content when a post is liked successfully")
        @WithMockUser(username = "author")
        void likesPostSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            LikeDTO request = new LikeDTO();
            request.setLike(true);

            doNothing().when(postService).likeOrDislikePost(eq(slug), eq(postId), any(LikeDTO.class));

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 400 Bad Request when like flag is missing")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenLikeFlagMissing() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            LikeDTO request = new LikeDTO();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_LIKES_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("bookmarkOrUnbookmarkPost(String, UUID, BookmarkDTO)")
    class BookmarkOrUnbookmarkPost {

        @Test
        @DisplayName("returns 204 No Content when a post is bookmarked successfully")
        @WithMockUser(username = "author")
        void bookmarksPostSuccessfully() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            BookmarkDTO request = new BookmarkDTO();
            request.setBookmark(true);

            doNothing().when(postService).bookmarkOrUnbookmarkPost(eq(slug), eq(postId), any(BookmarkDTO.class));

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 400 Bad Request when bookmark flag is missing")
        @WithMockUser(username = "author")
        void returnsBadRequestWhenBookmarkFlagMissing() throws Exception {
            String slug = "sample-post";
            UUID postId = UUID.randomUUID();

            BookmarkDTO request = new BookmarkDTO();

            mockMvc.perform(post(ApiRoutes.POSTS_BASE_PATH + ApiRoutes.POST_BOOKMARK_PATH, slug, postId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
