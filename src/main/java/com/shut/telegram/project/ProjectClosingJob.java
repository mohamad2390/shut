package com.shut.telegram.project;

import com.shut.project.BidService;
import com.shut.project.Project;
import com.shut.project.ProjectService;
import com.shut.telegram.config.TelegramBotEnabled;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Every few minutes, tells owners whose project deadline has passed that it is time to choose an offer. */
@Component
@TelegramBotEnabled
class ProjectClosingJob {

    private static final long CHECK_EVERY_MILLIS = 5 * 60 * 1000L;

    private final ProjectService projects;
    private final BidService bids;
    private final ProjectNotifier notifier;

    ProjectClosingJob(ProjectService projects, BidService bids, ProjectNotifier notifier) {
        this.projects = projects;
        this.bids = bids;
        this.notifier = notifier;
    }

    @Scheduled(initialDelay = 60_000L, fixedDelay = CHECK_EVERY_MILLIS)
    public void notifyClosedProjects() {
        for (Project project : projects.claimClosed()) {
            notifier.deadlinePassed(project, bids.count(project.getId()));
        }
    }
}
