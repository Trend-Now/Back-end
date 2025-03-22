package com.trend_now.backend.config;

import com.trend_now.backend.board.application.SignalKeywordJob;
import com.trend_now.backend.board.application.SignalKeywordJobListener;
import jakarta.annotation.PostConstruct;
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
@ConditionalOnProperty(name = "signalKeywordScheduler.enabled", havingValue = "true", matchIfMissing = false)
public class SignalKeywordSchedulerConfig {

    private static final String SIGNAL_KEYWORD_GROUP = "SignalGroup";
    private static final String SIGNAL_KEYWORD_JOB = "SignalKeywordJob";
    private static final String SIGNAL_KEYWORD_TRIGGER = "SignalKeywordTrigger";
    private static final int SIGNAL_KEYWORD_SCHEDULER_INTERVAL_SECONDS = 301;

    private Scheduler scheduler;
    private final ApplicationContext applicationContext;

    public SignalKeywordSchedulerConfig(Scheduler scheduler,
            ApplicationContext applicationContext) {
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
    }

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

        scheduler = new StdSchedulerFactory().getScheduler();
        SignalKeywordJobListener signalKeywordJobListener = new SignalKeywordJobListener();
        scheduler.getListenerManager().addJobListener(signalKeywordJobListener);
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }
}
