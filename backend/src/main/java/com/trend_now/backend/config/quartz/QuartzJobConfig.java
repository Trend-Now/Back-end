package com.trend_now.backend.config.quartz;

import com.trend_now.backend.board.application.SignalKeywordJob;
import com.trend_now.backend.post.application.PostLikesSyncDbJob;
import com.trend_now.backend.post.application.PostViewSyncDbJob;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class QuartzJobConfig {

    private static final String SIGNAL_KEYWORD_GROUP = "SignalGroup";
    private static final String SIGNAL_KEYWORD_JOB = "SignalKeywordJob";
    private static final String SIGNAL_KEYWORD_TRIGGER = "SignalKeywordTrigger";
    // signal.bz의 실시간 검색어 목록 갱신 주기가 30분이므로 요청 스케줄러 반복 시간도 30분(1800초)으로 설정
    private static final int SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS = 1801;
    private static final String SIGNAL_KEYWORD_SCHEDULER_CRON_EXPRESSION = "0 0,30 * * * ?";

    private static final String POST_LIKES_SYNC_DB_JOB = "PostLikesSyncDbJob";
    private static final String POST_LIKES_SYNC_DB_JOB_GROUP = "PostLikesSyncDbJobGroup";
    private static final String POST_LIKES_SYNC_DB_JOB_TRIGGER = "PostLikesSyncDbJobTrigger";
    private static final int POST_LIKES_SYNC_DB_JOB_INTERVAL_SECONDS = 301;

    private static final String POST_VIEW_SYNC_DB_JOB = "PostViewSyncDbJob";
    private static final String POST_VIEW_SYNC_DB_JOB_GROUP = "PostViewSyncDbJobGroup";
    private static final String POST_VIEW_SYNC_DB_JOB_TRIGGER = "PostViewSyncDbJobTrigger";
    private static final int POST_VIEW_SYNC_DB_JOB_INTERVAL_SECONDS = 301;

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
                        CronScheduleBuilder
                                .cronSchedule(SIGNAL_KEYWORD_SCHEDULER_CRON_EXPRESSION)
                )
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

    @Bean
    public JobDetail postViewSyncDbJobDetail() {
        JobDataMap postViewCtx = new JobDataMap();
        postViewCtx.put("applicationContext", applicationContext);

        return JobBuilder
                .newJob(PostViewSyncDbJob.class)
                .withIdentity(POST_VIEW_SYNC_DB_JOB, POST_VIEW_SYNC_DB_JOB_GROUP)
                .withDescription("게시글 조회수 DB 동기화 job")
                .setJobData(postViewCtx)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger postViewSyncDbTrigger(JobDetail postViewSyncDbJobDetail) {
        return TriggerBuilder
                .newTrigger()
                .forJob(postViewSyncDbJobDetail)
                .withIdentity(POST_VIEW_SYNC_DB_JOB_TRIGGER, POST_VIEW_SYNC_DB_JOB_GROUP)
                .withDescription("게시글 조회수 DB 동기화 Trigger")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withIntervalInSeconds(POST_VIEW_SYNC_DB_JOB_INTERVAL_SECONDS)
                                .repeatForever())
                .build();
    }
}
