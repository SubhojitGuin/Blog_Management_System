package com.project.Blog_Management_System.Schedulers;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@Feature("Scheduler / Scheduler Tests")
@ExtendWith(MockitoExtension.class)
public class SchedulerLockAnnotationTest {

    @Mock
    private PostPublishTask postPublishTask;

    @Mock
    private ViewSyncTask viewSyncTask;

    @Mock
    private UserDeletionTask userDeletionTask;

    @InjectMocks
    private PostPublishScheduler postPublishScheduler;

    @InjectMocks
    private UserBatchScheduler userBatchScheduler;

    @InjectMocks
    private ViewSyncScheduler viewSyncScheduler;

    private static Stream<Arguments> schedulerLockConfigs() throws NoSuchMethodException {
        return Stream.of(
                Arguments.of(PostPublishScheduler.class.getMethod("publishScheduledPosts"), "PT55S", "PT4M"),
                Arguments.of(ViewSyncScheduler.class.getMethod("syncViews"), "PT4M55S", "PT15M"),
                Arguments.of(UserBatchScheduler.class.getMethod("deleteUsersInBatches"), "PT1M", "PT30M")
        );
    }

    @ParameterizedTest
    @MethodSource("schedulerLockConfigs")
    void schedulerMethod_hasExpectedLockTiming(Method method, String expectedLockAtLeastFor, String expectedLockAtMostFor) {
        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);

        assertThat(lock)
                .as("%s must be annotated with @SchedulerLock", method.getName())
                .isNotNull();

        assertThat(lock.lockAtLeastFor()).isEqualTo(expectedLockAtLeastFor);
        assertThat(lock.lockAtMostFor()).isEqualTo(expectedLockAtMostFor);
    }

    @ParameterizedTest
    @MethodSource("schedulerLockConfigs")
    void lockAtMostFor_isAlwaysGreaterThan_lockAtLeastFor(Method method, String lockAtLeastFor, String lockAtMostFor) {
        Duration least = Duration.parse(lockAtLeastFor);
        Duration most = Duration.parse(lockAtMostFor);

        assertThat(most).isGreaterThan(least);
    }

    @Nested
    @DisplayName("Schedulers Execution Tests")
    @Story("Ensure that scheduler tasks are executed correctly")
    class SchedulerExecutionTests {
        @Test
        @DisplayName("PostPublishScheduler executes PostPublishTask")
        void postPublishScheduler_executesPostPublishTask() {
            doNothing().when(postPublishTask).execute();

            postPublishScheduler.publishScheduledPosts();

            verify(postPublishTask).execute();
        }

        @Test
        @DisplayName("UserBatchScheduler executes UserDeletionTask")
        void userBatchScheduler_executesUserDeletionTask() {
            doNothing().when(userDeletionTask).execute();

            userBatchScheduler.deleteUsersInBatches();

            verify(userDeletionTask).execute();
        }

        @Test
        @DisplayName("ViewSyncScheduler executes ViewSyncTask")
        void viewSyncScheduler_executesViewSyncTask() {
            doNothing().when(viewSyncTask).execute();

            viewSyncScheduler.syncViews();

            verify(viewSyncTask).execute();
        }
    }

}
