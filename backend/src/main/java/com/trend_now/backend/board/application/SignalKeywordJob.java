package com.trend_now.backend.board.application;

import com.trend_now.backend.board.application.board_summary.BoardSummaryTriggerService;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.dto.Top10WithDiff;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        log.info("실시간 검색어 순위 리스트 스케줄러 실행 시작 -  현재 시각: {}", Instant.now());
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        ApplicationContext applicationContext = (ApplicationContext) jobDataMap.get(
                "applicationContext");

        SignalKeywordService signalKeywordService = applicationContext.getBean(
                SignalKeywordService.class);
        BoardService boardService = applicationContext.getBean(BoardService.class);
        BoardRedisService boardRedisService = applicationContext.getBean(BoardRedisService.class);
        RedisPublisher redisPublisher = applicationContext.getBean(RedisPublisher.class);
        BoardCache boardCache = applicationContext.getBean(BoardCache.class);
        BoardSummaryTriggerService boardSummaryTriggerService = applicationContext.getBean(
            BoardSummaryTriggerService.class);

        // 스케줄러 실행 시 생기는 시간 차이를 줄이기 위해 Instant.now()를 최초 1회만 호출
        Instant now = Instant.now();
        try {
            SignalKeywordDto signalKeywordDto = signalKeywordService.fetchRealTimeKeyword().block();
            boardRedisService.cleanUpExpiredKeys();
            List<Top10WithDiff> top10WithDiffList = new ArrayList<>();
            for (int i = 0; i < signalKeywordDto.getTop10().size(); i++) {

                /**
                 *  Signal에서 받아온 데이터 Top10 객체를 통해 BoardSaveDto 객체 생성
                 *  - top10.getName() 의 데이터와 일치하는 게시판이 존재하면 해당 게시판의 식별자를 가져오고,
                 *      없으면 저장 후 가져온다.
                 *  - 가져온 게시판 식별자, 게시판 이름, 게시판 종류를 가지고 BoardSaveDto 객체 생성
                 *  - 해당 BoardSaveDto 객체를 통해 redis에 게시판 식별자, 게시판 이름, 게시판 종류, 순위 저장
                 */

                // signal.bz의 top10 객체를 BoardSaveDto 객체로 변환
                Top10 top10 = signalKeywordDto.getTop10().get(i);
                BoardSaveDto boardSaveDto = BoardSaveDto.from(top10);

                // top10 키워드로 게시판 저장 또는 업데이트
                Boards boards = boardService.saveOrUpdateBoard(boardSaveDto);
                boardSaveDto.setBoardId(boards.getId());

                // 저장된 게시판의 AI 요약 저장 또는 업데이트
                boardSummaryTriggerService.triggerSummaryUpdate(boards.getId(), boards.getName());

                // Redis의 realtime_keywords에 게시판 정보와 순위, 순위 변화량 저장
                Top10WithDiff top10WithDiff = signalKeywordService.addRealtimeKeyword(
                    boardSaveDto, i);
                top10WithDiffList.add(top10WithDiff);

                // i는 10번을 순회하면서 첫 번째는 0.1, 두 번째는 0.09, ... , 열 번째는 0.01의 값을 위 시간에서 더한다
                double score = calculateScore(now, i);
                boardRedisService.saveBoardRedis(boardSaveDto, score);
            }
            // Redis(realtime_keywords)에 저장된 이전 실시간 검색어 순위 리스트 삭제
            signalKeywordService.deleteOldRealtimeKeywords();
            // Redis(board_rank_valid)의 유효시간 갱신
            boardRedisService.setRankValidListTime();
            signalKeywordService.updateLastUpdatedTime(signalKeywordDto.getNow());

            // 인메모리 캐시에 게시판 정보 갱신
            boardCache.setBoardInfo(boardRedisService.getBoardRank(0, -1));

            Top10WithChange top10WithChange = new Top10WithChange(signalKeywordDto.getNow(), top10WithDiffList);

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

    private double calculateScore(Instant now, int index) {
        // score 값을 현재 Instant 시간에서 2시간이 지난 값으로 설정하여 saveBoardRedis에 전달
        Instant twoHoursLater = now.plus(2, ChronoUnit.HOURS);

        long epochMilli = twoHoursLater.toEpochMilli(); // UTC 기준 2시간이 지난 시간을 밀리초로 변환
        double rank = (10 - index) * 0.01;

        return (epochMilli + rank) * -1; // 내림차순 정렬을 위해 음수로 변환
    }
}
