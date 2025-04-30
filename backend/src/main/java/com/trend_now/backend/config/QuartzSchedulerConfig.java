package com.trend_now.backend.config;

import com.trend_now.backend.board.application.SignalKeywordJob;
import com.trend_now.backend.board.application.SignalKeywordJobListener;
import com.trend_now.backend.post.application.PostLikesSyncDbJob;
import com.trend_now.backend.post.application.PostLikesSyncDbJobListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "quartzScheduler.enabled", havingValue = "true", matchIfMissing = false)
public class QuartzSchedulerConfig {

    private static final String SIGNAL_KEYWORD_GROUP = "SignalGroup";
    private static final String SIGNAL_KEYWORD_JOB = "SignalKeywordJob";
    private static final String SIGNAL_KEYWORD_TRIGGER = "SignalKeywordTrigger";
    private static final int SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS = 11;
    private static final String POST_LIKES_SYNC_DB_JOB = "PostLikesSyncDbJob";
    private static final String POST_LIKES_SYNC_DB_JOB_GROUP = "PostLikesSyncDbJobGroup";
    private static final String POST_LIKES_SYNC_DB_JOB_TRIGGER = "PostLikesSyncDbJobTrigger";
    private static final int POST_LIKES_SYNC_DB_JOB_INTERVAL_SECONDS = 11;

    private final Scheduler scheduler;
    private final ApplicationContext applicationContext;

    @PostConstruct
    public void scheduleTopKeywordJob() throws SchedulerException {
        JobDataMap ctx = new JobDataMap();
        ctx.put("applicationContext", applicationContext);

        JobDetail job = JobBuilder
                .newJob(SignalKeywordJob.class)
                .withIdentity(SIGNAL_KEYWORD_JOB, SIGNAL_KEYWORD_GROUP)
                .withDescription("TOP10 검색어 순위 리스트 Job")
                .setJobData(ctx)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(SIGNAL_KEYWORD_TRIGGER, SIGNAL_KEYWORD_GROUP)
                .withDescription("TOP10 검색어 순위 리스트 Trigger")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withIntervalInSeconds(SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS)
                                .repeatForever())
                .build();

        JobDataMap postLikesCtx = new JobDataMap();
        postLikesCtx.put("applicationContext", applicationContext);

        JobDetail postLikesJob = JobBuilder
                .newJob(PostLikesSyncDbJob.class)
                .withIdentity(POST_LIKES_SYNC_DB_JOB, POST_LIKES_SYNC_DB_JOB_GROUP)
                .withDescription("DB 동기화 job")
                .setJobData(postLikesCtx)
                .build();

        Trigger postLikesTrigger = TriggerBuilder
                .newTrigger()
                .withIdentity(POST_LIKES_SYNC_DB_JOB_TRIGGER, POST_LIKES_SYNC_DB_JOB_GROUP)
                .withDescription("DB 동기화 Trigger")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withIntervalInSeconds(POST_LIKES_SYNC_DB_JOB_INTERVAL_SECONDS)
                                .repeatForever())
                .build();

        SignalKeywordJobListener signalKeywordJobListener = new SignalKeywordJobListener();
        PostLikesSyncDbJobListener postLikesSyncDbJobListener = new PostLikesSyncDbJobListener();
        scheduler.getListenerManager().addJobListener(signalKeywordJobListener);
        scheduler.getListenerManager().addJobListener(postLikesSyncDbJobListener);

        scheduler.start();
        scheduler.scheduleJob(job, trigger);
        scheduler.scheduleJob(postLikesJob, postLikesTrigger);
    }
}
