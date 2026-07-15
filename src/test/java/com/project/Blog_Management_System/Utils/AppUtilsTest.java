package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.BaseTest;
import com.project.Blog_Management_System.Entities.CommentEntity;
import com.project.Blog_Management_System.Entities.PostEntity;
import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.PostStatus;
import com.project.Blog_Management_System.Enums.Role;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Feature("Utils / AppUtils Tests")
@ExtendWith(MockitoExtension.class)
class AppUtilsTest extends BaseTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private AppUtils appUtils;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUser()")
    @Story("Retrieves the currently authenticated user entity from the security context")
    @Severity(SeverityLevel.CRITICAL)
    class GetCurrentUser {

        @Test
        @DisplayName("returns authenticated user entity from security context")
        void returnsAuthenticatedUserEntity() {
            UserEntity mockUser = new UserEntity();
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(mockUser);

            UserEntity result = AppUtils.getCurrentUser();

            assertEquals(mockUser, result);
        }
    }

    @Nested
    @DisplayName("hasRole()")
    @Story("Checks if the currently authenticated user has a specific role")
    @Severity(SeverityLevel.CRITICAL)
    class HasRole {

        @Test
        @DisplayName("returns true when user has matching role authority")
        void returnsTrueWhenUserHasMatchingRole() {
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            when(securityContext.getAuthentication()).thenReturn(authentication);
            doReturn(authorities).when(authentication).getAuthorities();

            boolean result = AppUtils.hasRole(Role.ADMIN);

            assertTrue(result);
        }

        @Test
        @DisplayName("returns false when user does not have matching role authority")
        void returnsFalseWhenUserDoesNotHaveMatchingRole() {
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            when(securityContext.getAuthentication()).thenReturn(authentication);
            doReturn(authorities).when(authentication).getAuthorities();

            boolean result = AppUtils.hasRole(Role.ADMIN);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("generateSlug()")
    @Story("Generates URL-friendly slugs from input strings")
    @Severity(SeverityLevel.CRITICAL)
    class GenerateSlug {

        @ParameterizedTest
        @CsvSource({
                "'Hello World', 'hello-world'",
                "'Spring Boot 3.0!', 'spring-boot-30'",
                "'Java & Microservices', 'java-microservices'",
                "'   spaces   test   ', 'spaces-test'"
        })
        @DisplayName("converts various string types to URL-friendly slugs")
        void convertsStringToSlug(String input, String expectedSlug) {
            String result = AppUtils.generateSlug(input);
            assertEquals(expectedSlug, result);
        }
    }

    @Nested
    @DisplayName("convertToSort()")
    @Story("Converts a list of sort field strings into a Spring Data Sort object")
    @Severity(SeverityLevel.CRITICAL)
    class ConvertToSort {

        private final Set<String> allowedFields = Set.of(PostEntity.Fields.title, PostEntity.Fields.createdAt);

        @Test
        @DisplayName("successfully parses valid sort fields with direction parameters")
        void parsesValidSortFieldsWithDirection() {
            List<String> sortFields = List.of("title:asc", "createdAt:desc");

            Sort sort = appUtils.convertToSort(sortFields, allowedFields);

            assertNotNull(sort);
            assertEquals(Sort.Direction.ASC, sort.getOrderFor("title").getDirection());
            assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("defaults sorting direction to ASC if direction parameter is omitted")
        void defaultsToAscIfDirectionIsOmitted() {
            List<String> sortFields = List.of(PostEntity.Fields.title);

            Sort sort = appUtils.convertToSort(sortFields, allowedFields);

            assertEquals(Sort.Direction.ASC, sort.getOrderFor("title").getDirection());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when sort property is not allowed")
        void throwsExceptionForDisallowedField() {
            List<String> sortFields = List.of("invalidField:asc");
            when(messageService.get(eq("exception.illegal.argument.invalid_sort_field"), any()))
                    .thenReturn("Invalid sort field");

            assertThrows(IllegalArgumentException.class, () ->
                    appUtils.convertToSort(sortFields, allowedFields)
            );
        }
    }

    @Nested
    @DisplayName("applyStatusAndPublishAt()")
    @Story("Applies post status and publish date to a PostEntity based on input parameters")
    @Severity(SeverityLevel.CRITICAL)
    class ApplyStatusAndPublishAt {

        private PostEntity post;

        @BeforeEach
        void setUp() {
            post = new PostEntity();
        }

        @Test
        @DisplayName("defaults status to DRAFT and sets publish date to null if status input is null")
        void defaultsToDraftWhenStatusIsNull() {
            appUtils.applyStatusAndPublishAt(post, null, LocalDateTime.now());

            assertEquals(PostStatus.DRAFT, post.getStatus());
            assertNull(post.getPublishAt());
        }

        @Test
        @DisplayName("applies SCHEDULED status and tracking timestamp cleanly")
        void appliesScheduledStatusCorrectly() {
            LocalDateTime publishTime = LocalDateTime.now().plusDays(2);

            appUtils.applyStatusAndPublishAt(post, PostStatus.SCHEDULED, publishTime);

            assertEquals(PostStatus.SCHEDULED, post.getStatus());
            assertEquals(publishTime, post.getPublishAt());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when publish date is missing for SCHEDULED status")
        void throwsExceptionWhenScheduledHasNoPublishDate() {
            when(messageService.get(anyString())).thenReturn("Publish date required");

            assertThrows(IllegalArgumentException.class, () ->
                    appUtils.applyStatusAndPublishAt(post, PostStatus.SCHEDULED, null)
            );
        }

        @Test
        @DisplayName("applies PUBLISHED status and wipes publish date to null")
        void appliesPublishedStatusCorrectly() {
            appUtils.applyStatusAndPublishAt(post, PostStatus.PUBLISHED, LocalDateTime.now());

            assertEquals(PostStatus.PUBLISHED, post.getStatus());
            assertNull(post.getPublishAt());
        }
    }

    @Nested
    @DisplayName("getCommentSnippet()")
    @Story("Generates a snippet from a comment's body")
    @Severity(SeverityLevel.CRITICAL)
    class GetCommentSnippet {

        @Mock
        private CommentEntity comment;

        @Test
        @DisplayName("returns original body unmodified if character length is exactly 100 or less")
        void returnsFullBodyIfShort() {
            String shortBody = "This is a short comment.";
            when(comment.getBody()).thenReturn(shortBody);

            String result = AppUtils.getCommentSnippet(comment);

            assertEquals(shortBody, result);
        }

        @Test
        @DisplayName("truncates body to exactly 100 characters and appends tracking ellipsis if string is long")
        void truncatesAndAppendsEllipsisIfLong() {
            String longBody = "a".repeat(150);
            String expectedSnippet = "a".repeat(100) + "...";
            when(comment.getBody()).thenReturn(longBody);

            String result = AppUtils.getCommentSnippet(comment);

            assertEquals(expectedSnippet, result);
        }
    }
}
