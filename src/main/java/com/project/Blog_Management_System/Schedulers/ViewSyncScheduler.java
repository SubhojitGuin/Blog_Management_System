package com.project.Blog_Management_System.Schedulers;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewSyncScheduler {

    private final ViewSyncTask viewSyncTask;

    @Scheduled(cron = "${blog.schedulers.viewSync.cron:0 */5 * * * *}")
    @SchedulerLock(name = "syncViews", lockAtMostFor = "PT15M", lockAtLeastFor = "PT4M55S")
    public void syncViews() {
        viewSyncTask.execute();
    }
}
