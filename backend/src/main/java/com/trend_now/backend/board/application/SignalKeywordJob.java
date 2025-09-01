package com.trend_now.backend.board.application;

import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.keyword_summarize.GeminiService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

@Slf4j
public class SignalKeywordJob implements Job {

    private static final String KEYWORD_JOB_ERROR_MESSAGE = "실시간 검색어 순위 리스트 스케줄러가 정상적으로 동작하지 않았습니다.";
    private static final String SIGNAL_KEYWORD_LIST_EVENT_MESSAGE = "실시간 검색어 순위 이벤트 호출";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        ApplicationContext applicationContext = (ApplicationContext) jobDataMap.get(
                "applicationContext");

        SignalKeywordService signalKeywordService = applicationContext.getBean(
                SignalKeywordService.class);
        BoardService boardService = applicationContext.getBean(BoardService.class);
        BoardRedisService boardRedisService = applicationContext.getBean(BoardRedisService.class);
        RedisPublisher redisPublisher = applicationContext.getBean(RedisPublisher.class);
        BoardCache boardCache = applicationContext.getBean(BoardCache.class);
        GeminiService geminiService = applicationContext.getBean(GeminiService.class);

        try {
            SignalKeywordDto signalKeywordDto = signalKeywordService.fetchRealTimeKeyword().block();
            boardRedisService.cleanUpExpiredKeys();
            Top10WithChange top10WithChange = signalKeywordService.calculateRankChange(
                    signalKeywordDto);
            for (int i = 0; i < signalKeywordDto.getTop10().size(); i++) {

                /**
                 *  Signal에서 받아온 데이터 Top10 객체를 통해 BoardSaveDto 객체 생성
                 *  - top10.getName() 의 데이터와 일치하는 게시판이 존재하면 해당 게시판의 식별자를 가져오고,
                 *      없으면 저장 후 가져온다.
                 *  - 가져온 게시판 식별자, 게시판 이름, 게시판 종류를 가지고 BoardSaveDto 객체 생성
                 *  - 해당 BoardSaveDto 객체를 통해 redis에 게시판 식별자, 게시판 이름, 게시판 종류, 순위 저장
                 */

                Top10 top10 = signalKeywordDto.getTop10().get(i);
                BoardSaveDto boardSaveDto = BoardSaveDto.from(top10);

                // 새로 등재된 키워드라면 AI로 이슈 요약
                String boardSummary = "";
                if (top10.getState().equals("n")) {
                    boardSummary = geminiService.summarizeKeyword(boardSaveDto.getBoardName());
                }

                Long boardId = boardService.saveBoardIfNotExists(boardSaveDto, boardSummary);
                boardSaveDto.setBoardId(boardId);
                boardRedisService.saveBoardRedis(boardSaveDto, i + 1);

                /* 동일 객체 참조로 내부 원본의 각 검색어의 게시판 ID를 포함하여 반환 */
                top10WithChange.getTop10WithDiff().get(i).setBoardId(boardId);

                boolean isRealTimeBoard = boardRedisService.isRealTimeBoard(boardSaveDto);
                boardService.updateBoardIsDeleted(boardSaveDto, isRealTimeBoard);
            }
            boardRedisService.setRankValidListTime();

            boardCache.setBoardInfo(boardRedisService.getBoardRank(0, -1));

            Set<String> allClientId = signalKeywordService.findAllClientId();
            for (String clientId : allClientId) {
                log.info("스케줄러에서 clientId: {}에게 이벤트 발행", clientId);
                SignalKeywordEventDto event = new SignalKeywordEventDto(clientId,
                        SIGNAL_KEYWORD_LIST_EVENT_MESSAGE, top10WithChange);
                redisPublisher.publishSignalKeywordEvent(event);
            }
        } catch (Exception e) {
            throw new JobExecutionException(KEYWORD_JOB_ERROR_MESSAGE, e);
        }
    }
}
