package com.project.Blog_Management_System.Service;

import com.project.Blog_Management_System.Dto.*;
import com.project.Blog_Management_System.Entities.*;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Events.CommentAddedEvent;
import com.project.Blog_Management_System.Events.CommentRepliedEvent;
import com.project.Blog_Management_System.Events.NewPostPublishedEvent;
import com.project.Blog_Management_System.Events.PostLikedEvent;
import com.project.Blog_Management_System.Exceptions.ResourceConflictException;
import com.project.Blog_Management_System.Exceptions.ResourceNotFoundException;
import com.project.Blog_Management_System.Repositories.*;
import com.project.Blog_Management_System.Service.Interfaces.RedisViewCountService;
import com.project.Blog_Management_System.Utils.AppUtils;
import com.project.Blog_Management_System.Utils.MessageService;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import com.project.Blog_Management_System.Utils.ValidationUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RedisViewCountService redisViewCountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ValidationUtils validationUtils;

    @Mock
    private AppUtils appUtils;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private PostServiceImpl postService;

    private UserEntity currentUser;
    private CategoryEntity category;
    private PostEntity post;
    private PostEntity savedPost;
    private PostRequestDTO requestDTO;
    private PostResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        currentUser = TestEntityFactory.testUser("current");
        currentUser.setId(UUID.randomUUID());

        category = TestEntityFactory.testCategory("main");
        category.setId(UUID.randomUUID());

        post = new PostEntity();
        post.setTitle("Test Post");
        post.setDescription("Test Description");
        post.setContent("Test Content");

        savedPost = new PostEntity();
        savedPost.setId(UUID.randomUUID());
        savedPost.setTitle("Test Post");
        savedPost.setSlug("test-post");
        savedPost.setDescription("Test Description");
        savedPost.setContent("Test Content");
        savedPost.setUser(currentUser);
        savedPost.setCategory(category);

        requestDTO = new PostRequestDTO();
        requestDTO.setTitle("Test Post");
        requestDTO.setDescription("Test Description");
        requestDTO.setContent("Test Content");
        requestDTO.setCategorySlug(category.getSlug());
        requestDTO.setStatus(PostStatus.DRAFT);

        responseDTO = new PostResponseDTO();
        responseDTO.setId(savedPost.getId());
        responseDTO.setTitle(savedPost.getTitle());
        responseDTO.setSlug(savedPost.getSlug());
        responseDTO.setDescription(savedPost.getDescription());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title",
            "createdAt",
            "updatedAt",
            "likeCount",
            "commentCount",
            "readingTimeMinutes"
    );

    @Nested
    @DisplayName("createPost(PostRequestDTO)")
    class CreatePost {

        @Test
        @DisplayName("successfully creates and returns post when all required fields are valid")
        void returnsPostResponseDTOWhenCreatedSuccessfully() {
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(1);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            PostResponseDTO result = postService.createPost(requestDTO);

            assertNotNull(result);
            assertEquals(savedPost.getId(), result.getId());
            assertEquals(savedPost.getTitle(), result.getTitle());
            assertEquals(savedPost.getSlug(), result.getSlug());

            verify(categoryRepository).findBySlug(category.getSlug());
            verify(validationUtils).isInvalidCategory(category, category.getSlug());
            verify(modelMapper).map(requestDTO, PostEntity.class);
            verify(postRepository).saveAndFlush(any(PostEntity.class));
            verify(userRepository).incrementPostCount(currentUser.getId());
            verify(modelMapper).map(savedPost, PostResponseDTO.class);
            verify(eventPublisher, never()).publishEvent(any(NewPostPublishedEvent.class));
        }

        @Test
        @DisplayName("publishes NewPostPublishedEvent when post status is PUBLISHED")
        void publishesEventWhenPostIsPublished() {
            requestDTO.setStatus(PostStatus.PUBLISHED);

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(1);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            doNothing().when(eventPublisher).publishEvent(any(NewPostPublishedEvent.class));

            postService.createPost(requestDTO);

            ArgumentCaptor<NewPostPublishedEvent> eventCaptor = ArgumentCaptor.forClass(NewPostPublishedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            NewPostPublishedEvent publishedEvent = eventCaptor.getValue();
            assertEquals(savedPost.getId(), publishedEvent.postId());
            assertEquals(savedPost.getSlug(), publishedEvent.postSlug());
            assertEquals(savedPost.getTitle(), publishedEvent.postTitle());
            assertEquals(currentUser.getId(), publishedEvent.authorId());
            assertEquals(currentUser.getName(), publishedEvent.authorName());
        }

        @Test
        @DisplayName("does not publish NewPostPublishedEvent when post status is SCHEDULED")
        void doesNotPublishEventWhenPostIsScheduled() {
            requestDTO.setStatus(PostStatus.SCHEDULED);
            requestDTO.setPublishAt(LocalDateTime.now().plusDays(1));

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(1);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            postService.createPost(requestDTO);

            verify(eventPublisher, never()).publishEvent(any(NewPostPublishedEvent.class));
        }

        @Test
        @DisplayName("throws exception when category does not exist")
        void throwsWhenCategoryNotFound() {
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("invalid category"))
                    .when(validationUtils).isInvalidCategory(null, category.getSlug());

            assertThrows(ResourceNotFoundException.class, () -> postService.createPost(requestDTO));

            verify(categoryRepository).findBySlug(category.getSlug());
            verify(validationUtils).isInvalidCategory(null, category.getSlug());
            verifyNoInteractions(modelMapper, postRepository, userRepository, eventPublisher);
        }

        @Test
        @DisplayName("throws ResourceConflictException when post count increment fails")
        void throwsWhenPostCountIncrementFails() {
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "increment", "posts", "user"))
                    .thenReturn("Failed to increment post count");

            ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                    () -> postService.createPost(requestDTO));

            assertNotNull(ex);
            verify(postRepository).saveAndFlush(any(PostEntity.class));
            verify(userRepository).incrementPostCount(currentUser.getId());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("applies status and publish date using AppUtils before saving")
        void appliesStatusAndPublishDateBeforeSaving() {
            LocalDateTime publishAt = LocalDateTime.now().plusDays(1);
            requestDTO.setStatus(PostStatus.SCHEDULED);
            requestDTO.setPublishAt(publishAt);

            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(1);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            postService.createPost(requestDTO);

            verify(appUtils).applyStatusAndPublishAt(post, PostStatus.SCHEDULED, publishAt);
        }

        @Test
        @DisplayName("invokes category retrieval, post save, and count increment in correct sequence")
        void invokesOperationsInCorrectSequence() {
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(modelMapper.map(requestDTO, PostEntity.class)).thenReturn(post);
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(userRepository.incrementPostCount(currentUser.getId())).thenReturn(1);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            postService.createPost(requestDTO);

            InOrder inOrder = inOrder(categoryRepository, modelMapper, postRepository, userRepository);
            inOrder.verify(categoryRepository).findBySlug(category.getSlug());
            inOrder.verify(modelMapper).map(requestDTO, PostEntity.class);
            inOrder.verify(postRepository).saveAndFlush(any(PostEntity.class));
            inOrder.verify(userRepository).incrementPostCount(currentUser.getId());
        }
    }

    @Nested
    @DisplayName("getAllPosts(UUID, int)")
    class GetAllPosts {

        @Test
        @DisplayName("returns slice of published posts with cursor-based pagination")
        void returnsPublishedPostsWithCursorPagination() {
            UUID postCursor = UUID.randomUUID();
            int size = 10;

            PostInfoDTO dto1 = new PostInfoDTO();
            dto1.setTitle("Post 1");
            PostInfoDTO dto2 = new PostInfoDTO();
            dto2.setTitle("Post 2");

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(dto1, dto2), PageRequest.of(0, size), false);

            when(postRepository.findAllPosts(PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getAllPosts(postCursor, size);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertFalse(result.hasNext());
            verify(postRepository).findAllPosts(PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size));
        }

        @Test
        @DisplayName("returns empty slice when no published posts exist")
        void returnsEmptySliceWhenNoPostsExist() {
            UUID postCursor = null;
            int size = 10;

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findAllPosts(PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getAllPosts(postCursor, size);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertFalse(result.hasNext());
        }

        @Test
        @DisplayName("indicates more results available when slice has next page")
        void indicatesMoreResultsWhenHasNext() {
            UUID postCursor = UUID.randomUUID();
            int size = 5;

            PostInfoDTO dto = new PostInfoDTO();
            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(dto, dto, dto, dto, dto), PageRequest.of(0, size), true);

            when(postRepository.findAllPosts(PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getAllPosts(postCursor, size);

            assertTrue(result.hasNext());
            assertEquals(5, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("getAllPostsOfFollowings(UUID, int)")
    class GetAllPostsOfFollowings {

        @Test
        @DisplayName("returns published posts from followed users with cursor pagination")
        void returnsPublishedPostsFromFollowings() {
            UUID postCursor = UUID.randomUUID();
            int size = 10;

            PostInfoDTO dto1 = new PostInfoDTO();
            dto1.setTitle("Following's Post 1");
            PostInfoDTO dto2 = new PostInfoDTO();
            dto2.setTitle("Following's Post 2");

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(dto1, dto2), PageRequest.of(0, size), false);

            when(postRepository.findAllPostsOfFollowings(currentUser.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getAllPostsOfFollowings(postCursor, size);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            verify(postRepository).findAllPostsOfFollowings(currentUser.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size));
        }

        @Test
        @DisplayName("returns empty slice when user has no followings or followings have no posts")
        void returnsEmptySliceWhenNoFollowingsPosts() {
            UUID postCursor = null;
            int size = 10;

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findAllPostsOfFollowings(currentUser.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getAllPostsOfFollowings(postCursor, size);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("uses current user id for fetching followings posts")
        void usesCurrentUserIdForFollowingsPosts() {
            UUID postCursor = UUID.randomUUID();
            int size = 5;

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
            when(postRepository.findAllPostsOfFollowings(currentUser.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            postService.getAllPostsOfFollowings(postCursor, size);

            verify(postRepository).findAllPostsOfFollowings(currentUser.getId(), PostStatus.PUBLISHED, postCursor, PageRequest.of(0, size));
        }
    }

    @Nested
    @DisplayName("searchPosts(PostFilterRequestDTO, int, int, List)")
    class SearchPosts {

        @Test
        @DisplayName("returns mapped page of posts matching filter criteria")
        void returnsFilteredPostsWithMapping() {
            PostFilterRequestDTO filterDTO = new PostFilterRequestDTO();
            List<String> sort = List.of("title:asc", "createdAt:desc");
            int page = 0;
            int size = 10;

            PostEntity post1 = new PostEntity();
            post1.setId(UUID.randomUUID());
            post1.setTitle("Java Post");

            PostEntity post2 = new PostEntity();
            post2.setId(UUID.randomUUID());
            post2.setTitle("Spring Post");

            Page<PostEntity> pagedResult = new PageImpl<>(List.of(post1, post2), PageRequest.of(page, size), 2);

            PostInfoDTO dto1 = new PostInfoDTO();
            dto1.setTitle("Java Post");
            PostInfoDTO dto2 = new PostInfoDTO();
            dto2.setTitle("Spring Post");

            when(postRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(pagedResult);
            when(appUtils.convertToSort(sort, ALLOWED_SORT_FIELDS))
                    .thenReturn(Sort.by("title").ascending().and(Sort.by("createdAt").descending()));
            when(modelMapper.map(post1, PostInfoDTO.class)).thenReturn(dto1);
            when(modelMapper.map(post2, PostInfoDTO.class)).thenReturn(dto2);

            Page<PostInfoDTO> result = postService.searchPosts(filterDTO, page, size, sort);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals("Java Post", result.getContent().get(0).getTitle());
            assertEquals("Spring Post", result.getContent().get(1).getTitle());
            verify(postRepository).findAll(any(Specification.class), any(PageRequest.class));
            verify(modelMapper, times(2)).map(any(PostEntity.class), eq(PostInfoDTO.class));
        }

        @Test
        @DisplayName("returns empty page when no posts match filter criteria")
        void returnsEmptyPageWhenNoMatches() {
            PostFilterRequestDTO filterDTO = new PostFilterRequestDTO();
            List<String> sort = List.of();
            int page = 0;
            int size = 10;

            Page<PostEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);

            when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);
            when(appUtils.convertToSort(sort, ALLOWED_SORT_FIELDS))
                    .thenReturn(Sort.unsorted());

            Page<PostInfoDTO> result = postService.searchPosts(filterDTO, page, size, sort);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
        }

        @Test
        @DisplayName("filters invalid sort fields before querying repository")
        void filtersInvalidSortFields() {
            PostFilterRequestDTO filterDTO = new PostFilterRequestDTO();
            List<String> sortWithInvalid = List.of("title:asc", "invalidField:asc");
            int page = 0;
            int size = 10;

            Page<PostEntity> result = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);

            when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(result);
            when(appUtils.convertToSort(sortWithInvalid, ALLOWED_SORT_FIELDS))
                    .thenReturn(Sort.by("title").ascending());

            postService.searchPosts(filterDTO, page, size, sortWithInvalid);

            verify(appUtils).convertToSort(sortWithInvalid, ALLOWED_SORT_FIELDS);
            verify(postRepository).findAll(any(Specification.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("uses PostFilterSpecification to build query specification")
        void usesPostFilterSpecificationForQuerying() {
            PostFilterRequestDTO filterDTO = new PostFilterRequestDTO();
            List<String> sort = List.of("title:asc");
            int page = 1;
            int size = 20;

            Page<PostEntity> result = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);

            when(postRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(result);
            when(appUtils.convertToSort(sort, ALLOWED_SORT_FIELDS))
                    .thenReturn(Sort.by("title").ascending());

            postService.searchPosts(filterDTO, page, size, sort);

            verify(postRepository).findAll(any(Specification.class), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("getPostsByStatus(PostStatus, UUID, int)")
    class GetPostsByStatus {

        @Test
        @DisplayName("returns user's posts filtered by status with cursor pagination")
        void returnsUserPostsByStatus() {
            PostStatus status = PostStatus.DRAFT;
            UUID postCursor = UUID.randomUUID();
            int size = 10;

            PostInfoDTO dto1 = new PostInfoDTO();
            dto1.setTitle("Draft 1");
            PostInfoDTO dto2 = new PostInfoDTO();
            dto2.setTitle("Draft 2");

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(dto1, dto2), PageRequest.of(0, size), false);

            when(postRepository.findByUserIdAndStatus(currentUser.getId(), status, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getPostsByStatus(status, postCursor, size);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            verify(postRepository).findByUserIdAndStatus(currentUser.getId(), status, postCursor, PageRequest.of(0, size));
        }

        @Test
        @DisplayName("returns empty slice when user has no posts with given status")
        void returnsEmptySliceWhenNoPostsWithStatus() {
            PostStatus status = PostStatus.SCHEDULED;
            UUID postCursor = null;
            int size = 10;

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findByUserIdAndStatus(currentUser.getId(), status, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<PostInfoDTO> result = postService.getPostsByStatus(status, postCursor, size);

            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("uses current user id to fetch only their posts")
        void usesCurrentUserIdForFiltering() {
            PostStatus status = PostStatus.PUBLISHED;
            UUID postCursor = UUID.randomUUID();
            int size = 5;

            Slice<PostInfoDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
            when(postRepository.findByUserIdAndStatus(currentUser.getId(), status, postCursor, PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            postService.getPostsByStatus(status, postCursor, size);

            verify(postRepository).findByUserIdAndStatus(currentUser.getId(), status, postCursor, PageRequest.of(0, size));
        }
    }

    @Nested
    @DisplayName("getPost(String, UUID)")
    class GetPost {

        @Test
        @DisplayName("returns mapped post when published post is requested by any user")
        void returnsPublishedPostForAnyUser() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            PostResponseDTO result = postService.getPost(postSlug, postId);

            assertNotNull(result);
            assertFalse(result.getIsLiked());
            verify(postRepository).findById(postId);
            verify(validationUtils).isInvalidPost(savedPost, postSlug);
            verify(modelMapper).map(savedPost, PostResponseDTO.class);
            verify(redisViewCountService).addViewer(postId, currentUser.getId());
        }

        @Test
        @DisplayName("sets isOwner to true when post author is current user")
        void setsIsOwnerTrueForAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            postService.getPost(postSlug, postId);

            assertEquals(currentUser, savedPost.getUser());
        }

        @Test
        @DisplayName("sets isOwner to false when post author is different from current user")
        void setsIsOwnerFalseForNonAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            PostResponseDTO result = postService.getPost(postSlug, postId);

            assertNotNull(result);
            assertNotEquals(currentUser, savedPost.getUser());
        }

        @Test
        @DisplayName("sets isLiked to true when current user has liked the post")
        void setsIsLikedTrueWhenUserLiked() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.of(new LikeEntity()));
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            PostResponseDTO result = postService.getPost(postSlug, postId);

            assertNotNull(result);
            assertTrue(result.getIsLiked());
        }

        @Test
        @DisplayName("sets isLiked to false when current user has not liked the post")
        void setsIsLikedFalseWhenUserNotLiked() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            PostResponseDTO result = postService.getPost(postSlug, postId);

            assertNotNull(result);
            assertFalse(result.getIsLiked());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post is accessed by non-author")
        void throwsWhenDraftPostAccessedByNonAuthor() {
            String postSlug = "someone-else-draft";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.resource.not_found", "Post")).thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class, () -> postService.getPost(postSlug, postId));

            verify(postRepository).findById(postId);
            verify(validationUtils).isInvalidPost(savedPost, postSlug);
            verifyNoInteractions(modelMapper, likeRepository, redisViewCountService);
        }

        @Test
        @DisplayName("allows author to view their own draft post")
        void allowsAuthorToViewDraftPost() {
            String postSlug = "my-draft";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);
            savedPost.setUser(currentUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            PostResponseDTO result = postService.getPost(postSlug, postId);

            assertNotNull(result);
            verify(postRepository).findById(postId);
            verify(modelMapper).map(savedPost, PostResponseDTO.class);
        }

        @Test
        @DisplayName("throws exception when post does not exist")
        void throwsWhenPostNotFound() {
            String postSlug = "missing-post";
            UUID postId = UUID.randomUUID();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("Post not found"))
                    .when(validationUtils).isInvalidPost(null, postSlug);

            assertThrows(ResourceNotFoundException.class, () -> postService.getPost(postSlug, postId));

            verify(postRepository).findById(postId);
            verify(validationUtils).isInvalidPost(null, postSlug);
            verifyNoInteractions(modelMapper, likeRepository, redisViewCountService);
        }

        @Test
        @DisplayName("adds viewer to Redis for view count tracking")
        void addsViewerToRedis() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            postService.getPost(postSlug, postId);

            verify(redisViewCountService).addViewer(postId, currentUser.getId());
        }

        @Test
        @DisplayName("invokes post retrieval, validation, and like check in correct sequence")
        void invokesOperationsInCorrectSequence() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            doNothing().when(redisViewCountService).addViewer(postId, currentUser.getId());

            postService.getPost(postSlug, postId);

            InOrder inOrder = inOrder(postRepository, validationUtils, likeRepository);
            inOrder.verify(postRepository).findById(postId);
            inOrder.verify(validationUtils).isInvalidPost(savedPost, postSlug);
            inOrder.verify(likeRepository).findByUserIdAndPostId(currentUser.getId(), postId);
         }
    }

    @Nested
    @DisplayName("updatePost(String, UUID, PostRequestDTO)")
    class UpdatePost {

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post is accessed by non-author")
        void throwsWhenDraftPostAccessedByNonAuthor() {
            String postSlug = "someone-else-draft";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            doThrow(new ResourceNotFoundException("Post not found"))
                    .when(validationUtils).isInvalidPost(savedPost, postSlug);

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.updatePost(postSlug, postId, requestDTO));

            verify(postRepository).findById(postId);
        }

        @Test
        @DisplayName("throws AccessDeniedException when user is not post owner")
        void throwsWhenUserIsNotPostOwner() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.auth.access.denied", "update", "post"))
                    .thenReturn("Access denied");

            assertThrows(AccessDeniedException.class,
                    () -> postService.updatePost(postSlug, postId, requestDTO));

            verify(postRepository).findById(postId);
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("throws exception when category does not exist")
        void throwsWhenCategoryNotFound() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("Category not found"))
                    .when(validationUtils).isInvalidCategory(null, category.getSlug());

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.updatePost(postSlug, postId, requestDTO));

            verify(postRepository).findById(postId);
            verify(categoryRepository).findBySlug(category.getSlug());
        }

        @Test
        @DisplayName("updates post title, description, content and category")
        void updatesAllPostFields() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            requestDTO.setTitle("NewTitle");
            requestDTO.setDescription("NewDesc");
            requestDTO.setContent("NewContent");

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            postService.updatePost(postSlug, postId, requestDTO);

            ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
            verify(postRepository).saveAndFlush(postCaptor.capture());

            PostEntity savedPostArg = postCaptor.getValue();
            assertEquals("NewTitle", savedPostArg.getTitle());
            assertEquals("NewDesc", savedPostArg.getDescription());
            assertEquals("NewContent", savedPostArg.getContent());
        }

        @Test
        @DisplayName("invokes validation and operations in correct sequence")
        void invokesOperationsInCorrectSequence() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(categoryRepository.findBySlug(category.getSlug())).thenReturn(Optional.of(category));
            when(postRepository.saveAndFlush(any(PostEntity.class))).thenReturn(savedPost);
            when(modelMapper.map(savedPost, PostResponseDTO.class)).thenReturn(responseDTO);

            postService.updatePost(postSlug, postId, requestDTO);

            InOrder inOrder = inOrder(postRepository, validationUtils, categoryRepository);
            inOrder.verify(postRepository).findById(postId);
            inOrder.verify(validationUtils).isInvalidPost(savedPost, postSlug);
            inOrder.verify(categoryRepository).findBySlug(category.getSlug());
            inOrder.verify(postRepository).saveAndFlush(any(PostEntity.class));
        }
    }

    @Nested
    @DisplayName("deletePost(String, UUID)")
    class DeletePost {

        @Test
        @DisplayName("deletes post and decrements post count when user is owner")
        void deletesPostWhenUserIsOwner() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);
            savedPost.setUser(currentUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(userRepository.decrementPostCount(currentUser.getId())).thenReturn(1);

            postService.deletePost(postSlug, postId);

            verify(postRepository).delete(savedPost);
            verify(userRepository).decrementPostCount(currentUser.getId());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post accessed by non-author")
        void throwsWhenDraftPostAccessedByNonAuthor() {
            String postSlug = "draft-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            doThrow(new ResourceNotFoundException("Post not found"))
                    .when(validationUtils).isInvalidPost(savedPost, postSlug);

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.deletePost(postSlug, postId));

            verify(postRepository).findById(postId);
            verify(postRepository, never()).delete(any(PostEntity.class));
        }

        @Test
        @DisplayName("throws AccessDeniedException when non-owner tries to delete and is not admin")
        void throwsWhenNonOwnerTriesToDelete() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.auth.access.denied", "delete", "post"))
                    .thenReturn("Access denied");

            assertThrows(AccessDeniedException.class,
                    () -> postService.deletePost(postSlug, postId));

            verify(postRepository).findById(postId);
            verify(postRepository, never()).delete(any(PostEntity.class));
        }

        @Test
        @DisplayName("throws ResourceConflictException when post count decrement fails")
        void throwsWhenPostCountDecrementFails() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(userRepository.decrementPostCount(currentUser.getId())).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "decrement", "posts", "user"))
                    .thenReturn("Failed to decrement post count");

            assertThrows(ResourceConflictException.class,
                    () -> postService.deletePost(postSlug, postId));

            verify(postRepository).delete(savedPost);
        }

        @Test
        @DisplayName("throws exception when post does not exist")
        void throwsWhenPostNotFound() {
            String postSlug = "missing-post";
            UUID postId = UUID.randomUUID();

            when(postRepository.findById(postId)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("Post not found"))
                    .when(validationUtils).isInvalidPost(null, postSlug);

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.deletePost(postSlug, postId));

            verify(postRepository).findById(postId);
            verify(postRepository, never()).delete(any(PostEntity.class));
        }
    }

    @Nested
    @DisplayName("getTopLevelCommentsOfPost(String, UUID, UUID, int)")
    class GetTopLevelCommentsOfPost {

        @Test
        @DisplayName("returns slice of top level comments when post is published")
        void returnsCommentsWhenPostIsPublished() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentCursor = null;
            int size = 10;
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentResponseDTO comment1 = new CommentResponseDTO();
            comment1.setId(UUID.randomUUID());
            comment1.setBody("Comment 1");

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(comment1), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getTopLevelCommentsOfPost(postSlug, postId, commentCursor, size);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(postRepository).findById(postId);
            verify(commentRepository).findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size));
        }

        @Test
        @DisplayName("returns empty slice when no top level comments exist")
        void returnsEmptySliceWhenNoComments() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentCursor = null;
            int size = 10;
            savedPost.setStatus(PostStatus.PUBLISHED);

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getTopLevelCommentsOfPost(postSlug, postId, commentCursor, size);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("allows author to view comments on their draft post")
        void allowsAuthorToViewDraftPostComments() {
            String postSlug = "my-draft";
            UUID postId = savedPost.getId();
            UUID commentCursor = null;
            int size = 10;
            savedPost.setStatus(PostStatus.DRAFT);
            savedPost.setUser(currentUser);

            CommentResponseDTO comment = new CommentResponseDTO();
            comment.setId(UUID.randomUUID());

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(comment), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getTopLevelCommentsOfPost(postSlug, postId, commentCursor, size);

            assertNotNull(result);
            verify(commentRepository).findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post accessed by non-author")
        void throwsWhenDraftPostAccessedByNonAuthor() {
            String postSlug = "someone-else-draft";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.resource.not_found", "Post")).thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getTopLevelCommentsOfPost(postSlug, postId, null, 10));

            verify(postRepository).findById(postId);
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("uses cursor pagination with correct parameters")
        void usesCursorPaginationCorrectly() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentCursor = UUID.randomUUID();
            int size = 20;
            savedPost.setStatus(PostStatus.PUBLISHED);

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            postService.getTopLevelCommentsOfPost(postSlug, postId, commentCursor, size);

            verify(commentRepository).findTopLevelByPost(postId, commentCursor, currentUser.getId(), PageRequest.of(0, size));
        }
    }

    @Nested
    @DisplayName("getRepliesOfComment(String, UUID, UUID, UUID, int)")
    class GetRepliesOfComment {

        @Test
        @DisplayName("returns slice of replies when post is published")
        void returnsRepliesWhenPostIsPublished() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            UUID replyCursor = null;
            int size = 10;
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setBody("Parent comment");
            parentComment.setPost(savedPost);

            CommentResponseDTO reply = new CommentResponseDTO();
            reply.setId(UUID.randomUUID());
            reply.setBody("Reply");
            reply.setParentId(parentCommentId);

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(reply), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(commentRepository.findRepliesByParentId(parentCommentId, replyCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getRepliesOfComment(postSlug, postId, parentCommentId, replyCursor, size);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(commentRepository).findRepliesByParentId(parentCommentId, replyCursor, currentUser.getId(), PageRequest.of(0, size));
        }

        @Test
        @DisplayName("returns empty slice when no replies exist")
        void returnsEmptySliceWhenNoReplies() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            UUID replyCursor = null;
            int size = 10;
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setPost(savedPost);

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(commentRepository.findRepliesByParentId(parentCommentId, replyCursor, currentUser.getId(), PageRequest.of(0, size)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getRepliesOfComment(postSlug, postId, parentCommentId, replyCursor, size);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("allows author to view replies on their draft post")
        void allowsAuthorToViewDraftPostReplies() {
            String postSlug = "my-draft";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.DRAFT);
            savedPost.setUser(currentUser);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setPost(savedPost);

            Slice<CommentResponseDTO> expectedSlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(commentRepository.findRepliesByParentId(parentCommentId, null, currentUser.getId(), PageRequest.of(0, 10)))
                    .thenReturn(expectedSlice);

            Slice<CommentResponseDTO> result = postService.getRepliesOfComment(postSlug, postId, parentCommentId, null, 10);

            assertNotNull(result);
            verify(commentRepository).findRepliesByParentId(parentCommentId, null, currentUser.getId(), PageRequest.of(0, 10));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post accessed by non-author")
        void throwsWhenDraftPostAccessedByNonAuthor() {
            String postSlug = "someone-else-draft";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.resource.not_found", "Post")).thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getRepliesOfComment(postSlug, postId, parentCommentId, null, 10));

            verify(postRepository).findById(postId);
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when parent comment does not belong to post")
        void throwsWhenParentCommentNotBelongsToPost() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID differentPostId = UUID.randomUUID();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            PostEntity differentPost = new PostEntity();
            differentPost.setId(differentPostId);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setPost(differentPost);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(messageService.get("exception.illegal.argument.parent_comment_post_mismatch"))
                    .thenReturn("Parent comment does not belong to this post");

            assertThrows(IllegalArgumentException.class,
                    () -> postService.getRepliesOfComment(postSlug, postId, parentCommentId, null, 10));

            verify(commentRepository).findById(parentCommentId);
            verify(commentRepository, never()).findRepliesByParentId(any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws exception when parent comment does not exist")
        void throwsWhenParentCommentNotFound() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.empty());
            doThrow(new ResourceNotFoundException("Comment not found"))
                    .when(validationUtils).isInvalidComment(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getRepliesOfComment(postSlug, postId, parentCommentId, null, 10));

            verify(commentRepository).findById(parentCommentId);
        }
    }

    @Nested
    @DisplayName("addTopLevelComments(String, UUID, CommentRequestDTO)")
    class AddTopLevelComments {

        @Test
        @DisplayName("adds top-level comment, increments count, and notifies author when commenter is different")
        void addsCommentAndPublishesEventForDifferentAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity postAuthor = TestEntityFactory.testUser("author");
            postAuthor.setId(UUID.randomUUID());
            savedPost.setUser(postAuthor);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Great post from another user.");

            CommentEntity mappedComment = new CommentEntity();
            mappedComment.setBody(request.getBody());

            CommentEntity savedComment = new CommentEntity();
            savedComment.setId(UUID.randomUUID());
            savedComment.setBody(request.getBody());
            savedComment.setUser(currentUser);
            savedComment.setPost(savedPost);

            CommentResponseDTO mappedResponse = new CommentResponseDTO();
            mappedResponse.setId(savedComment.getId());
            mappedResponse.setBody(savedComment.getBody());

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(request, CommentEntity.class)).thenReturn(mappedComment);
            when(commentRepository.saveAndFlush(mappedComment)).thenReturn(savedComment);
            when(postRepository.incrementCommentCount(postId)).thenReturn(1);
            when(modelMapper.map(savedComment, CommentResponseDTO.class)).thenReturn(mappedResponse);

            CommentResponseDTO result = postService.addTopLevelComments(postSlug, postId, request);

            assertNotNull(result);
            assertTrue(result.getIsAuthor());

            ArgumentCaptor<CommentAddedEvent> eventCaptor = ArgumentCaptor.forClass(CommentAddedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(savedPost.getId(), eventCaptor.getValue().postId());
            assertEquals(request.getBody(), eventCaptor.getValue().commentSnippet());
            verify(postRepository).incrementCommentCount(postId);
        }

        @Test
        @DisplayName("adds top-level comment without notification when commenter is post author")
        void addsCommentWithoutPublishingEventForPostAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);
            savedPost.setUser(currentUser);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("My own post comment.");

            CommentEntity mappedComment = new CommentEntity();
            mappedComment.setBody(request.getBody());

            CommentEntity savedComment = new CommentEntity();
            savedComment.setId(UUID.randomUUID());
            savedComment.setBody(request.getBody());

            CommentResponseDTO mappedResponse = new CommentResponseDTO();
            mappedResponse.setId(savedComment.getId());

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(request, CommentEntity.class)).thenReturn(mappedComment);
            when(commentRepository.saveAndFlush(mappedComment)).thenReturn(savedComment);
            when(postRepository.incrementCommentCount(postId)).thenReturn(1);
            when(modelMapper.map(savedComment, CommentResponseDTO.class)).thenReturn(mappedResponse);

            CommentResponseDTO result = postService.addTopLevelComments(postSlug, postId, request);

            assertNotNull(result);
            assertTrue(result.getIsAuthor());
            verify(eventPublisher, never()).publishEvent(any(CommentAddedEvent.class));
        }

        @Test
        @DisplayName("throws ResourceConflictException when comment count increment fails after adding top-level comment")
        void throwsWhenTopLevelCommentCountIncrementFails() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Will fail after save");

            CommentEntity mappedComment = new CommentEntity();
            mappedComment.setBody(request.getBody());

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(modelMapper.map(request, CommentEntity.class)).thenReturn(mappedComment);
            when(commentRepository.saveAndFlush(mappedComment)).thenReturn(new CommentEntity());
            when(postRepository.incrementCommentCount(postId)).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "increment", "comment", "post"))
                    .thenReturn("Failed to increment comment count");

            assertThrows(ResourceConflictException.class,
                    () -> postService.addTopLevelComments(postSlug, postId, request));

            verify(commentRepository).saveAndFlush(mappedComment);
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("addReplyToComment(String, UUID, UUID, CommentRequestDTO)")
    class AddReplyToComment {

        @Test
        @DisplayName("adds reply and publishes reply and author notifications when users are distinct")
        void addsReplyAndPublishesBothEventsWhenUsersAreDistinct() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity postAuthor = TestEntityFactory.testUser("author");
            postAuthor.setId(UUID.randomUUID());
            savedPost.setUser(postAuthor);

            UserEntity parentCommentAuthor = TestEntityFactory.testUser("parent");
            parentCommentAuthor.setId(UUID.randomUUID());

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setBody("Parent comment body");
            parentComment.setPost(savedPost);
            parentComment.setUser(parentCommentAuthor);
            parentComment.setDepth(0);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Reply from current user");

            CommentEntity mappedReply = new CommentEntity();
            mappedReply.setBody(request.getBody());

            CommentEntity savedReply = new CommentEntity();
            savedReply.setId(UUID.randomUUID());
            savedReply.setBody(request.getBody());

            CommentResponseDTO mappedResponse = new CommentResponseDTO();
            mappedResponse.setId(savedReply.getId());

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(modelMapper.map(request, CommentEntity.class)).thenReturn(mappedReply);
            when(commentRepository.saveAndFlush(mappedReply)).thenReturn(savedReply);
            when(postRepository.incrementCommentCount(postId)).thenReturn(1);
            when(modelMapper.map(savedReply, CommentResponseDTO.class)).thenReturn(mappedResponse);
            doNothing().when(eventPublisher).publishEvent(any(CommentRepliedEvent.class));
            doNothing().when(eventPublisher).publishEvent(any(CommentAddedEvent.class));

            CommentResponseDTO result = postService.addReplyToComment(postSlug, postId, parentCommentId, request);

            assertNotNull(result);
            assertTrue(result.getIsAuthor());
            assertEquals(1, mappedReply.getDepth());
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
            verify(eventPublisher).publishEvent(any(CommentRepliedEvent.class));
            verify(eventPublisher).publishEvent(any(CommentAddedEvent.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when parent comment belongs to another post")
        void throwsWhenReplyParentBelongsToDifferentPost() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            PostEntity differentPost = new PostEntity();
            differentPost.setId(UUID.randomUUID());

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setPost(differentPost);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Reply body");

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(messageService.get("exception.illegal.argument.parent_comment_post_mismatch"))
                    .thenReturn("Parent comment does not belong to this post");

            assertThrows(IllegalArgumentException.class,
                    () -> postService.addReplyToComment(postSlug, postId, parentCommentId, request));

            verify(commentRepository, never()).saveAndFlush(any(CommentEntity.class));
            verify(postRepository, never()).incrementCommentCount(any(UUID.class));
        }

        @Test
        @DisplayName("throws ResourceConflictException when comment count increment fails after adding reply")
        void throwsWhenReplyCountIncrementFails() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID parentCommentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity parentComment = new CommentEntity();
            parentComment.setId(parentCommentId);
            parentComment.setBody("Parent comment");
            parentComment.setDepth(0);
            parentComment.setPost(savedPost);
            parentComment.setUser(TestEntityFactory.testUser("other"));

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Reply body");

            CommentEntity mappedReply = new CommentEntity();
            mappedReply.setBody(request.getBody());

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
            when(modelMapper.map(request, CommentEntity.class)).thenReturn(mappedReply);
            when(commentRepository.saveAndFlush(mappedReply)).thenReturn(new CommentEntity());
            when(postRepository.incrementCommentCount(postId)).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "increment", "comment", "post"))
                    .thenReturn("Failed to increment comment count");

            assertThrows(ResourceConflictException.class,
                    () -> postService.addReplyToComment(postSlug, postId, parentCommentId, request));

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("updateComment(String, UUID, UUID, CommentRequestDTO)")
    class UpdateComment {

        @Test
        @DisplayName("updates comment and returns response when current user is comment author")
        void updatesCommentWhenCurrentUserIsAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(currentUser);
            comment.setPost(savedPost);
            comment.setBody("Old comment");

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Updated comment");

            doAnswer(invocation -> {
                CommentRequestDTO source = invocation.getArgument(0);
                CommentEntity target = invocation.getArgument(1);
                target.setBody(source.getBody());
                return null;
            }).when(modelMapper).map(request, comment);

            CommentResponseDTO mappedResponse = new CommentResponseDTO();
            mappedResponse.setId(commentId);
            mappedResponse.setBody("Updated comment");

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.saveAndFlush(comment)).thenReturn(comment);
            when(modelMapper.map(comment, CommentResponseDTO.class)).thenReturn(mappedResponse);

            CommentResponseDTO result = postService.updateComment(postSlug, postId, commentId, request);

            assertNotNull(result);
            assertTrue(result.getIsAuthor());
            assertEquals("Updated comment", comment.getBody());
        }

        @Test
        @DisplayName("throws AccessDeniedException when current user is not comment author")
        void throwsWhenCurrentUserIsNotCommentAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(otherUser);
            comment.setPost(savedPost);

            CommentRequestDTO request = new CommentRequestDTO();
            request.setBody("Updated comment");

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(messageService.get("exception.auth.access.denied", "update", "comment"))
                    .thenReturn("Access denied");

            assertThrows(AccessDeniedException.class,
                    () -> postService.updateComment(postSlug, postId, commentId, request));

            verify(commentRepository, never()).saveAndFlush(any(CommentEntity.class));
        }
    }

    @Nested
    @DisplayName("deleteComment(String, UUID, UUID)")
    class DeleteComment {

        @Test
        @DisplayName("deletes comment and decrements count when current user is comment author")
        void deletesCommentWhenCurrentUserIsAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(currentUser);
            comment.setPost(savedPost);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(postRepository.decrementCommentCount(postId)).thenReturn(1);

            postService.deleteComment(postSlug, postId, commentId);

            verify(commentRepository).deleteById(commentId);
            verify(postRepository).decrementCommentCount(postId);
        }

        @Test
        @DisplayName("deletes comment when current user has admin role")
        void deletesCommentWhenCurrentUserIsAdmin() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UsernamePasswordAuthenticationToken adminAuth =
                    new UsernamePasswordAuthenticationToken(currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(adminAuth);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(otherUser);
            comment.setPost(savedPost);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(postRepository.decrementCommentCount(postId)).thenReturn(1);

            postService.deleteComment(postSlug, postId, commentId);

            verify(commentRepository).deleteById(commentId);
            verify(postRepository).decrementCommentCount(postId);
        }

        @Test
        @DisplayName("throws ResourceConflictException when comment count decrement fails after delete")
        void throwsWhenCommentCountDecrementFailsAfterDelete() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(currentUser);
            comment.setPost(savedPost);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(postRepository.decrementCommentCount(postId)).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "decrement", "comment", "post"))
                    .thenReturn("Failed to decrement comment count");

            assertThrows(ResourceConflictException.class,
                    () -> postService.deleteComment(postSlug, postId, commentId));

            verify(commentRepository).deleteById(commentId);
        }

        @Test
        @DisplayName("throws AccessDeniedException when current user is neither author nor admin")
        void throwsWhenCurrentUserIsNeitherAuthorNorAdmin() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID commentId = UUID.randomUUID();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());

            CommentEntity comment = new CommentEntity();
            comment.setId(commentId);
            comment.setUser(otherUser);
            comment.setPost(savedPost);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(messageService.get("exception.auth.access.denied", "delete", "comment"))
                    .thenReturn("Access denied");

            assertThrows(AccessDeniedException.class,
                    () -> postService.deleteComment(postSlug, postId, commentId));

            verify(commentRepository, never()).deleteById(any(UUID.class));
            verify(postRepository, never()).decrementCommentCount(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("getLikesOfPost(String, UUID, UUID, int)")
    class GetLikesOfPost {

        @Test
        @DisplayName("returns likes slice when published post exists")
        void returnsLikesSliceForPublishedPost() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            UUID likeCursor = UUID.randomUUID();
            int size = 10;
            savedPost.setStatus(PostStatus.PUBLISHED);

            LikeInfoDTO likeInfo = new LikeInfoDTO();
            UserInfoDTO userInfo = new UserInfoDTO();
            userInfo.setId(currentUser.getId());
            likeInfo.setUser(userInfo);

            Slice<LikeInfoDTO> expectedSlice = new SliceImpl<>(List.of(likeInfo), PageRequest.of(0, size), false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(likeRepository.findLikesOfPost(postId, likeCursor, PageRequest.of(0, size))).thenReturn(expectedSlice);

            Slice<LikeInfoDTO> result = postService.getLikesOfPost(postSlug, postId, likeCursor, size);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(postRepository).findById(postId);
            verify(likeRepository).findLikesOfPost(postId, likeCursor, PageRequest.of(0, size));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when draft post is requested by non-author")
        void throwsWhenDraftPostRequestedByNonAuthor() {
            String postSlug = "draft-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.DRAFT);

            UserEntity otherUser = TestEntityFactory.testUser("other");
            otherUser.setId(UUID.randomUUID());
            savedPost.setUser(otherUser);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(messageService.get("exception.resource.not_found", "Post")).thenReturn("Post not found");

            assertThrows(ResourceNotFoundException.class,
                    () -> postService.getLikesOfPost(postSlug, postId, null, 5));

            verify(postRepository).findById(postId);
            verifyNoInteractions(likeRepository);
        }
    }

    @Nested
    @DisplayName("likeOrDislikePost(String, UUID, LikeDTO)")
    class LikeOrDislikePost {

        @Test
        @DisplayName("creates a like, increments count, and publishes event when liking another user's published post")
        void likesPostAndPublishesEventForDifferentAuthor() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            UserEntity author = TestEntityFactory.testUser("author");
            author.setId(UUID.randomUUID());
            savedPost.setUser(author);

            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setLike(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            when(postRepository.incrementLikeCount(postId)).thenReturn(1);
            when(likeRepository.saveAndFlush(any(LikeEntity.class))).thenReturn(new LikeEntity());
            doNothing().when(eventPublisher).publishEvent(any(PostLikedEvent.class));

            postService.likeOrDislikePost(postSlug, postId, likeDTO);

            ArgumentCaptor<PostLikedEvent> eventCaptor = ArgumentCaptor.forClass(PostLikedEvent.class);
            verify(likeRepository).saveAndFlush(any(LikeEntity.class));
            verify(postRepository).incrementLikeCount(postId);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(postId, eventCaptor.getValue().postId());
            assertEquals(currentUser.getName(), eventCaptor.getValue().likerName());
            assertEquals(author.getName(), eventCaptor.getValue().authorName());
        }

        @Test
        @DisplayName("does nothing when liking a post that current user has already liked")
        void doesNotCreateDuplicateLikeWhenAlreadyLiked() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setLike(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.of(new LikeEntity()));

            postService.likeOrDislikePost(postSlug, postId, likeDTO);

            verify(likeRepository, never()).saveAndFlush(any(LikeEntity.class));
            verify(postRepository, never()).incrementLikeCount(any(UUID.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("removes existing like and decrements count when unliking a post")
        void removesLikeWhenDislikingExistingLike() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setLike(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.of(new LikeEntity()));
            when(postRepository.decrementLikeCount(postId)).thenReturn(1);

            postService.likeOrDislikePost(postSlug, postId, likeDTO);

            verify(likeRepository).deleteByUserIdAndPostId(currentUser.getId(), postId);
            verify(postRepository).decrementLikeCount(postId);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("throws ResourceConflictException when like count increment fails")
        void throwsWhenLikeCountIncrementFails() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setLike(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());
            when(postRepository.incrementLikeCount(postId)).thenReturn(0);
            when(messageService.get("exception.resource.conflict.count_update_failure", "increment", "like", "post"))
                    .thenReturn("Failed to increment like count");

            assertThrows(ResourceConflictException.class,
                    () -> postService.likeOrDislikePost(postSlug, postId, likeDTO));

            verify(likeRepository).saveAndFlush(any(LikeEntity.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("bookmarkOrUnbookmarkPost(String, UUID, BookmarkDTO)")
    class BookmarkOrUnbookmarkPost {

        @Test
        @DisplayName("creates bookmark when bookmarking a post that is not already bookmarked")
        void createsBookmarkWhenNotAlreadyBookmarked() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            BookmarkDTO bookmarkDTO = new BookmarkDTO();
            bookmarkDTO.setBookmark(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());

            postService.bookmarkOrUnbookmarkPost(postSlug, postId, bookmarkDTO);

            verify(bookmarkRepository).saveAndFlush(any(BookmarkEntity.class));
            verify(bookmarkRepository, never()).deleteByUserIdAndPostId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("does not create duplicate bookmark when post is already bookmarked")
        void doesNotCreateDuplicateBookmarkWhenAlreadyBookmarked() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            BookmarkDTO bookmarkDTO = new BookmarkDTO();
            bookmarkDTO.setBookmark(true);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.of(new BookmarkEntity()));

            postService.bookmarkOrUnbookmarkPost(postSlug, postId, bookmarkDTO);

            verify(bookmarkRepository, never()).saveAndFlush(any(BookmarkEntity.class));
            verify(bookmarkRepository, never()).deleteByUserIdAndPostId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("deletes bookmark when unbookmarking a post that is bookmarked")
        void deletesBookmarkWhenAlreadyBookmarkedAndUnbookmarking() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            BookmarkDTO bookmarkDTO = new BookmarkDTO();
            bookmarkDTO.setBookmark(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.of(new BookmarkEntity()));

            postService.bookmarkOrUnbookmarkPost(postSlug, postId, bookmarkDTO);

            verify(bookmarkRepository).deleteByUserIdAndPostId(currentUser.getId(), postId);
            verify(bookmarkRepository, never()).saveAndFlush(any(BookmarkEntity.class));
        }

        @Test
        @DisplayName("does nothing when unbookmarking a post that is not bookmarked")
        void doesNothingWhenUnbookmarkingWithoutExistingBookmark() {
            String postSlug = "test-post";
            UUID postId = savedPost.getId();
            savedPost.setStatus(PostStatus.PUBLISHED);

            BookmarkDTO bookmarkDTO = new BookmarkDTO();
            bookmarkDTO.setBookmark(false);

            when(postRepository.findById(postId)).thenReturn(Optional.of(savedPost));
            when(bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId)).thenReturn(Optional.empty());

            postService.bookmarkOrUnbookmarkPost(postSlug, postId, bookmarkDTO);

            verify(bookmarkRepository, never()).saveAndFlush(any(BookmarkEntity.class));
            verify(bookmarkRepository, never()).deleteByUserIdAndPostId(any(UUID.class), any(UUID.class));
        }
    }
}
