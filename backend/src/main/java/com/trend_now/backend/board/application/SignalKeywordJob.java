package com.trend_now.backend.board.application;

import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.Top10;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

@Slf4j
public class SignalKeywordJob implements Job {

    private static final String KEYWORD_JOB_ERROR_MESSAGE = "실시간 검색어 순위 리스트 스케줄러가 정상적으로 동작하지 않았습니다.";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        ApplicationContext applicationContext = (ApplicationContext) jobDataMap.get(
                "applicationContext");

        SignalKeywordService signalKeywordService = applicationContext.getBean(
                SignalKeywordService.class);
        BoardService boardService = applicationContext.getBean(BoardService.class);
        BoardRedisService boardRedisService = applicationContext.getBean(BoardRedisService.class);

        try {
            SignalKeywordDto signalKeywordDto = signalKeywordService.fetchRealTimeKeyword().block();
            boardRedisService.cleanUpExpiredKeys();
            for(int i = 0; i < signalKeywordDto.getTop10().size(); i++) {
                Top10 top10 = signalKeywordDto.getTop10().get(i);
                BoardSaveDto boardSaveDto = BoardSaveDto.from(top10);

                boardRedisService.saveBoardRedis(boardSaveDto, i + 1);
                boardService.saveBoardIfNotExists(boardSaveDto);

                boolean isRealTimeBoard = boardRedisService.isRealTimeBoard(boardSaveDto);
                boardService.updateBoardIsDeleted(boardSaveDto, isRealTimeBoard);
            }
            boardRedisService.setRankValidListTime();
        } catch (Exception e) {
            throw new JobExecutionException(KEYWORD_JOB_ERROR_MESSAGE, e);
        }
    }
}
