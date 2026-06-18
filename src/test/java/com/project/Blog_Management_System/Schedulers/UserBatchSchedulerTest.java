package com.project.Blog_Management_System.Schedulers;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Repositories.FollowRepository;
import com.project.Blog_Management_System.Repositories.UserRepository;
import com.project.Blog_Management_System.Utils.TestEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserBatchSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserBatchScheduler userBatchScheduler;

    @Nested
    @DisplayName("deleteUsersInBatches()")
    class DeleteUsersInBatches {

        @Test
        @DisplayName("deletes single inactive user and anonymizes all fields")
        void deletesSingleInactiveUserAndAnonymizesAllFields() {
            UserEntity inactiveUser = TestEntityFactory.testUser("inactive");
            inactiveUser.setId(UUID.randomUUID());
            inactiveUser.setNoOfFollowers(10);
            inactiveUser.setNoOfFollowings(5);
            inactiveUser.setNoOfPosts(3);

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(inactiveUser), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<List<UserEntity>> usersCaptor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(usersCaptor.capture());

            List<UserEntity> savedUsers = usersCaptor.getValue();
            assertEquals(1, savedUsers.size());
            UserEntity deletedUser = savedUsers.getFirst();
            assertAll(
                    () -> assertEquals("Deleted User", deletedUser.getName()),
                    () -> assertTrue(deletedUser.getUsername().startsWith("deleted_user_")),
                    () -> assertNull(deletedUser.getEmail()),
                    () -> assertNull(deletedUser.getPassword()),
                    () -> assertNull(deletedUser.getBio()),
                    () -> assertNull(deletedUser.getGender()),
                    () -> assertNull(deletedUser.getDateOfBirth()),
                    () -> assertEquals(0, deletedUser.getNoOfFollowers()),
                    () -> assertEquals(0, deletedUser.getNoOfFollowings()),
                    () -> assertEquals(0, deletedUser.getNoOfPosts()),
                    () -> assertTrue(deletedUser.getIsDeleted()),
                    () -> assertNull(deletedUser.getRoles())
            );
        }

        @Test
        @DisplayName("deletes all follow relationships for each inactive user")
        void deletesAllFollowRelationshipsForEachUser() {
            UserEntity inactiveUser = TestEntityFactory.testUser("inactive");
            inactiveUser.setId(UUID.randomUUID());

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(inactiveUser), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);

            userBatchScheduler.deleteUsersInBatches();

            verify(followRepository).deleteByFollowerIdOrFollowingId(inactiveUser.getId(), inactiveUser.getId());
        }

        @Test
        @DisplayName("processes multiple inactive users in single batch")
        void processesMulitpleUsersInSingleBatch() {
            UserEntity user1 = TestEntityFactory.testUser("inactive1");
            user1.setId(UUID.randomUUID());
            UserEntity user2 = TestEntityFactory.testUser("inactive2");
            user2.setId(UUID.randomUUID());

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user1, user2), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<List<UserEntity>> usersCaptor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(usersCaptor.capture());
            assertEquals(2, usersCaptor.getValue().size());
        }

        @Test
        @DisplayName("processes users across multiple pages until all are deleted")
        void processesUsersAcrossMultiplePages() {
            UserEntity user1 = TestEntityFactory.testUser("inactive1");
            user1.setId(UUID.randomUUID());
            UserEntity user2 = TestEntityFactory.testUser("inactive2");
            user2.setId(UUID.randomUUID());

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user1), PageRequest.of(0, 1000), true);
            Slice<UserEntity> secondPage = new SliceImpl<>(List.of(user2), PageRequest.of(1, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage)
                    .thenReturn(secondPage);

            userBatchScheduler.deleteUsersInBatches();

            verify(userRepository, times(2)).saveAll(any(List.class));
        }

        @Test
        @DisplayName("queries users with 15 day cutoff from current time")
        void queriesUsersWithCorrectCutoffDate() {
            Slice<UserEntity> emptyPage = new SliceImpl<>(List.of(), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(userRepository).findInactiveUsers(cutoffCaptor.capture(), any(PageRequest.class));

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertNotNull(capturedCutoff);
        }

        @Test
        @DisplayName("uses batch size of 1000 for pagination")
        void usesBatchSizeOf1000() {
            Slice<UserEntity> emptyPage = new SliceImpl<>(List.of(), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository).findInactiveUsers(any(LocalDateTime.class), pageCaptor.capture());

            PageRequest capturedPage = pageCaptor.getValue();
            assertEquals(1000, capturedPage.getPageSize());
        }

        @Test
        @DisplayName("does not process when no inactive users are found")
        void doesNotProcessWhenNoInactiveUsersFound() {
            Slice<UserEntity> emptyPage = new SliceImpl<>(List.of(), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            userBatchScheduler.deleteUsersInBatches();

            verify(userRepository, never()).saveAll(any(List.class));
            verify(followRepository, never()).deleteByFollowerIdOrFollowingId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("handles exception during deletion gracefully")
        void handlesExceptionDuringDeletionGracefully() {
            UserEntity user = TestEntityFactory.testUser("inactive");
            user.setId(UUID.randomUUID());

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);
            doThrow(new RuntimeException("Database error"))
                    .when(userRepository).saveAll(any(List.class));

            assertThrows(RuntimeException.class, () -> userBatchScheduler.deleteUsersInBatches());

            verify(followRepository).deleteByFollowerIdOrFollowingId(user.getId(), user.getId());
        }

        @Test
        @DisplayName("preserves user ID in deleted username")
        void preservesUserIdInDeletedUsername() {
            UserEntity user = TestEntityFactory.testUser("inactive");
            UUID userId = UUID.randomUUID();
            user.setId(userId);

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<List<UserEntity>> usersCaptor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(usersCaptor.capture());

            UserEntity deletedUser = usersCaptor.getValue().getFirst();
            assertTrue(deletedUser.getUsername().contains(userId.toString()));
        }

        @Test
        @DisplayName("starts pagination from page 0")
        void startsPaginationFromPage0() {
            Slice<UserEntity> emptyPage = new SliceImpl<>(List.of(), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository).findInactiveUsers(any(LocalDateTime.class), pageCaptor.capture());

            assertEquals(0, pageCaptor.getValue().getPageNumber());
        }

        @Test
        @DisplayName("continues paging when more pages exist")
        void continuesPagingWhenMorePagesExist() {
            UserEntity user1 = TestEntityFactory.testUser("inactive1");
            user1.setId(UUID.randomUUID());
            UserEntity user2 = TestEntityFactory.testUser("inactive2");
            user2.setId(UUID.randomUUID());
            UserEntity user3 = TestEntityFactory.testUser("inactive3");
            user3.setId(UUID.randomUUID());

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user1), PageRequest.of(0, 1000), true);
            Slice<UserEntity> secondPage = new SliceImpl<>(List.of(user2), PageRequest.of(1, 1000), true);
            Slice<UserEntity> thirdPage = new SliceImpl<>(List.of(user3), PageRequest.of(2, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage)
                    .thenReturn(secondPage)
                    .thenReturn(thirdPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userRepository, times(3)).findInactiveUsers(any(LocalDateTime.class), pageCaptor.capture());

            List<PageRequest> capturedPages = pageCaptor.getAllValues();
            assertEquals(0, capturedPages.get(0).getPageNumber());
            assertEquals(1, capturedPages.get(1).getPageNumber());
            assertEquals(2, capturedPages.get(2).getPageNumber());
        }

        @Test
        @DisplayName("marks users as deleted when no more pages are available")
        void marksUsersAsDeletedWhenNoMorePages() {
            UserEntity user = TestEntityFactory.testUser("inactive");
            user.setId(UUID.randomUUID());
            user.setIsDeleted(false);

            Slice<UserEntity> lastPage = new SliceImpl<>(List.of(user), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(lastPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<List<UserEntity>> usersCaptor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(usersCaptor.capture());

            assertTrue(usersCaptor.getValue().getFirst().getIsDeleted());
        }

        @Test
        @DisplayName("clears user data before marking as deleted")
        void clearsUserDataBeforeMarkingAsDeleted() {
            UserEntity user = TestEntityFactory.testUser("inactive");
            user.setId(UUID.randomUUID());
            user.setBio("User bio");

            Slice<UserEntity> firstPage = new SliceImpl<>(List.of(user), PageRequest.of(0, 1000), false);

            when(userRepository.findInactiveUsers(any(LocalDateTime.class), any(PageRequest.class)))
                    .thenReturn(firstPage);

            userBatchScheduler.deleteUsersInBatches();

            ArgumentCaptor<List<UserEntity>> usersCaptor = ArgumentCaptor.forClass(List.class);
            verify(userRepository).saveAll(usersCaptor.capture());

            UserEntity deletedUser = usersCaptor.getValue().getFirst();
            assertAll(
                    () -> assertNull(deletedUser.getBio()),
                    () -> assertNull(deletedUser.getGender())
            );
        }
    }
}

