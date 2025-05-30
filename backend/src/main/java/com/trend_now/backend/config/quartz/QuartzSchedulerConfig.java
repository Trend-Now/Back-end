package com.trend_now.backend.config.quartz;

import com.trend_now.backend.board.application.SignalKeywordJobListener;
import com.trend_now.backend.post.application.PostLikesSyncDbJobListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "quartzScheduler.enabled", havingValue = "true", matchIfMissing = false)
public class QuartzSchedulerConfig {

    private final Scheduler scheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleTopKeywordJob() throws SchedulerException {
        SignalKeywordJobListener signalKeywordJobListener = new SignalKeywordJobListener();
        PostLikesSyncDbJobListener postLikesSyncDbJobListener = new PostLikesSyncDbJobListener();
        scheduler.getListenerManager().addJobListener(signalKeywordJobListener);
        scheduler.getListenerManager().addJobListener(postLikesSyncDbJobListener);
    }
}
