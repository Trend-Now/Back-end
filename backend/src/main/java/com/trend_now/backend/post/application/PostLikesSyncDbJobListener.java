package com.trend_now.backend.post.application;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

@Slf4j
public class PostLikesSyncDbJobListener implements JobListener {

    private static final String JOB_NAME = "PostLikesSyncDbJob";
    private static final String JOB_TO_BE_EXECUTED = "[-] PostLikesSyncDb Job이 실행 되기 전 수행됩니다";
    private static final String JOB_EXECUTION_VETOED = "[-] PostLikesSyncDb Job이 실행 취소된 시점 수행됩니다";
    private static final String JOB_WAS_EXECUTED = "[-] PostLikesSyncDb Job이 실행 완료된 시점 수행됩니다";

    @Override
    public String getName() {
        return JOB_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {
        log.info(JOB_TO_BE_EXECUTED);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {
        log.info(JOB_EXECUTION_VETOED);
    }

    @Override
    public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
        log.info(JOB_WAS_EXECUTED);
    }
}
