package com.project.Blog_Management_System.Schedulers;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostPublishScheduler {

    private final PostPublishTask postPublishTask;

    @Scheduled(cron = "${blog.schedulers.publishPost.cron:0 * * * * *}")
    @SchedulerLock(name = "publishScheduledPosts", lockAtMostFor = "PT4M", lockAtLeastFor = "PT55S")
    public void publishScheduledPosts() {
        postPublishTask.execute();
    }
}
