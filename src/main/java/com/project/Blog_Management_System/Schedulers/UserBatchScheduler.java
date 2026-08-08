package com.project.Blog_Management_System.Schedulers;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBatchScheduler {

    private final UserDeletionTask userDeletionTask;

    @Scheduled(cron = "${blog.schedulers.userDeletion.cron:0 0 0 * * *}")
    @SchedulerLock(name = "deleteInactiveUsers", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void deleteUsersInBatches() {
        userDeletionTask.execute();
    }
}
