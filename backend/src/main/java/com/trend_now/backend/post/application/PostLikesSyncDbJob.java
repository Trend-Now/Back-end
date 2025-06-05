package com.trend_now.backend.post.application;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class PostLikesSyncDbJob implements Job {

    private static final String APPLICATION_CONTEXT = "applicationContext";
    private static final String SYNC_JOB_ERROR_MESSAGE = "DB 동기화 Job 실행 중 오류 발생";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        ApplicationContext applicationContext = (ApplicationContext) jobDataMap.get(
                APPLICATION_CONTEXT);

        PostLikesService postLikesService = applicationContext.getBean(PostLikesService.class);

        try {
            postLikesService.syncLikesToDatabase();
        } catch (Exception e) {
            throw new JobExecutionException(SYNC_JOB_ERROR_MESSAGE, e);
        }
    }
}

