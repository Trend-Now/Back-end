package com.trend_now.backend.config.quartz;

import com.trend_now.backend.board.application.SignalKeywordJob;
import com.trend_now.backend.post.application.PostLikesSyncDbJob;
import lombok.RequiredArgsConstructor;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class QuartzJobConfig {

    private static final String SIGNAL_KEYWORD_GROUP = "SignalGroup";
    private static final String SIGNAL_KEYWORD_JOB = "SignalKeywordJob";
    private static final String SIGNAL_KEYWORD_TRIGGER = "SignalKeywordTrigger";
    private static final int SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS = 301;
    private static final String POST_LIKES_SYNC_DB_JOB = "PostLikesSyncDbJob";
    private static final String POST_LIKES_SYNC_DB_JOB_GROUP = "PostLikesSyncDbJobGroup";
    private static final String POST_LIKES_SYNC_DB_JOB_TRIGGER = "PostLikesSyncDbJobTrigger";
    private static final int POST_LIKES_SYNC_DB_JOB_INTERVAL_SECONDS = 301;

    private final ApplicationContext applicationContext;

    @Bean
    public JobDetail signalKeywordJobDetail() {
        JobDataMap ctx = new JobDataMap();
        ctx.put("applicationContext", applicationContext);

        return JobBuilder
            .newJob(SignalKeywordJob.class)
            .withIdentity(SIGNAL_KEYWORD_JOB, SIGNAL_KEYWORD_GROUP)
            .withDescription("TOP10 검색어 순위 리스트 Job")
            .setJobData(ctx)
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger signalKeywordTrigger(JobDetail signalKeywordJobDetail) {
        return TriggerBuilder
            .newTrigger()
            .forJob(signalKeywordJobDetail)
            .withIdentity(SIGNAL_KEYWORD_TRIGGER, SIGNAL_KEYWORD_GROUP)
            .withDescription("TOP10 검색어 순위 리스트 Trigger")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder
                    .simpleSchedule()
                    .withIntervalInSeconds(SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS)
                    .repeatForever())
            .build();
    }

    @Bean
    public JobDetail postLikesSyncDbJobDetail() {
        JobDataMap postLikesCtx = new JobDataMap();
        postLikesCtx.put("applicationContext", applicationContext);

        return JobBuilder
            .newJob(PostLikesSyncDbJob.class)
            .withIdentity(POST_LIKES_SYNC_DB_JOB, POST_LIKES_SYNC_DB_JOB_GROUP)
            .withDescription("DB 동기화 job")
            .setJobData(postLikesCtx)
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger postLikesSyncDbTrigger(JobDetail postLikesSyncDbJobDetail) {
        return TriggerBuilder
            .newTrigger()
            .forJob(postLikesSyncDbJobDetail)
            .withIdentity(POST_LIKES_SYNC_DB_JOB_TRIGGER, POST_LIKES_SYNC_DB_JOB_GROUP)
            .withDescription("DB 동기화 Trigger")
            .startNow()
            .withSchedule(
                SimpleScheduleBuilder
                    .simpleSchedule()
                    .withIntervalInSeconds(POST_LIKES_SYNC_DB_JOB_INTERVAL_SECONDS)
                    .repeatForever())
            .build();
    }
}
