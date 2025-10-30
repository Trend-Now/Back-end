package com.trend_now.backend.board.application;

import com.trend_now.backend.board.application.board_summary.BoardSummaryTriggerService;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.SignalKeywordDto;
import com.trend_now.backend.board.dto.SignalKeywordEventDto;
import com.trend_now.backend.board.dto.Top10;
import com.trend_now.backend.board.dto.Top10WithChange;
import com.trend_now.backend.board.repository.BoardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
        BoardRepository boardRepository = applicationContext.getBean(BoardRepository.class);
        BoardService boardService = applicationContext.getBean(BoardService.class);
        BoardRedisService boardRedisService = applicationContext.getBean(BoardRedisService.class);
        RedisPublisher redisPublisher = applicationContext.getBean(RedisPublisher.class);
        BoardCache boardCache = applicationContext.getBean(BoardCache.class);
        BoardSummaryTriggerService boardSummaryTriggerService = applicationContext.getBean(
            BoardSummaryTriggerService.class);

        // 스케줄러 실행 동안 생기는 시간 차이를 없애기 위해 Instant.now()를 최초 1회만 호출
        Instant now = Instant.now();
        try {
            SignalKeywordDto signalKeywordDto = signalKeywordService.fetchRealTimeKeyword().block();
            boardRedisService.cleanUpExpiredKeys();
            if (signalKeywordDto == null) {
                log.warn("시그널 비즈 API 호출 실패");
                throw new Exception(KEYWORD_JOB_ERROR_MESSAGE);
            }
            for (Top10 top10 : signalKeywordDto.getTop10()) {

                /**
                 *  본 메서드는 크게 4가지 경우로 나뉘어 동작한다
                 *  1. 완전히 새로운 게시판인 경우 -> DB에 게시판 생성하고 저장, Redis에 키워드 저장
                 *  2. 실시간 게시판에 이미 존재하는 키워드인 경우 -> Redis에 키워드 저장 및 순위 갱신
                 *  3. 똑같은 키워드가 실시간 게시판에 존재하지 않지만, 유사한 게시판은 존재하는 경우 -> DB 게시판 이름 업데이트, Redis에 키워드 저장 및 순위 갱신
                 *  4. 실시간 게시판에도 없고, 유사한 게시판도 없지만 DB에는 저장 돼 있는 경우 -> DB isDeleted 상태 변경, Redis에 키워드 저장
                 */

                // signal.bz의 top10 객체를 BoardSaveDto 객체로 변환
                BoardSaveDto boardSaveDto = BoardSaveDto.from(top10);

                // 기존 게시판들과 유사도 검증 (비슷한 이름의 게시판이 있으면 해당 게시판의 이름 반환, 없으면 입력받은 이름 그대로 반환)
                String oldBoardName = boardCache.findKeywordSimilarity(boardSaveDto.getBoardName());
                // 새로 들어온 키워드와 oldBoardName가 다르면 비슷한 이름의 게시판이 존재하는 것
                boolean hasSimilarBoard = !oldBoardName.equals(boardSaveDto.getBoardName());
                String searchBoardName = hasSimilarBoard ? oldBoardName : boardSaveDto.getBoardName();

                Optional<Boards> optionalBoards = boardRepository.findByName(searchBoardName);

                double score = calculateScore(now, top10.getRank());

                /* 완전히 새로운 게시판 */
                if (optionalBoards.isEmpty()) {
                    // 게시판 생성 후 저장
                    Boards board = boardService.saveBoard(boardSaveDto);
                    boardSaveDto.setBoardId(board.getId());
                    // 게시판 요약 생성
                    boardSummaryTriggerService.triggerSummaryUpdate(board.getId(), board.getName());
                    // realtime_keywords에 저장
                    signalKeywordService.addNewRealtimeKeyword(board.getId(), boardSaveDto.getBoardName(), top10.getRank());
                    // board_rank에 저장
                    boardRedisService.saveBoardRedis(boardSaveDto, score);
                    continue;
                }

                Boards board = optionalBoards.get();
                boardSaveDto.setBoardId(board.getId());
                boolean isRealTimeBoard = boardRedisService.isRealTimeBoard(board.getName(), board.getId());

                /* 정확히 같은 키워드가 실시간 이슈 목록에 있음 */
                if (isRealTimeBoard && !hasSimilarBoard) {
                    // 랭크 변동 추이 계산 후 realtime_keywords에 저장
                    signalKeywordService.addRealtimeKeywordWithRankTracking(board.getId(), board.getName(), boardSaveDto.getBoardName(), top10.getRank());
                    // board_rank에 새로운 score로 저장하고 TTL로 초기화
                    boardRedisService.saveBoardRedis(boardSaveDto, score);
                    continue;
                }

                /* 유사한 키워드가 실시간 이슈 목록에 있음 */
                if (isRealTimeBoard && hasSimilarBoard) {
                    boardService.updateBoardName(board, boardSaveDto.getBoardName());
                    // 새로운 키워드 랭크 변동 추이 계산 후 realtime_keywords에 저장
                    signalKeywordService.addRealtimeKeywordWithRankTracking(board.getId(), oldBoardName, boardSaveDto.getBoardName(), top10.getRank());
                    // 새로 키워드로 게시판 생성 후 저장
                    boardRedisService.saveBoardRedis(boardSaveDto, score);
                    // 기존 게시판은 board_rank에서 삭제
                    boardRedisService.deleteKeyInBoardRank(board.getId(), oldBoardName);
                    continue;
                }

                /* 실시간 게시판에 없음 (복원) */
                if (board.isDeleted()) {
                    board.changeDeleted();
                }
                // 삭제 되었다가 다시 생성된 게시판이므로 게시판 요약 재생성
                boardSummaryTriggerService.triggerSummaryUpdate(board.getId(), board.getName());
                signalKeywordService.addNewRealtimeKeyword(board.getId(), boardSaveDto.getBoardName(), top10.getRank());
                boardRedisService.saveBoardRedis(boardSaveDto, score);
            }
            // Redis(realtime_keywords)에 저장된 이전 실시간 검색어 순위 리스트 삭제
            signalKeywordService.deleteOldRealtimeKeywords();
            // Redis(board_rank_valid)의 유효시간 갱신
            boardRedisService.setRankValidListTime();
            signalKeywordService.updateLastUpdatedTime(signalKeywordDto.getNow());
            // 인메모리 캐시에 게시판 정보 갱신
            boardCache.setBoardInfo(boardRedisService.getBoardRank(0, -1));
            // 실시간 이슈 키워드 변경 SSE 이벤트 발행
            publishRealtimeKeywordsEvent(signalKeywordService, redisPublisher);
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

    private void publishRealtimeKeywordsEvent(SignalKeywordService signalKeywordService,
        RedisPublisher redisPublisher) {
        Top10WithChange top10WithChange = signalKeywordService.getRealTimeKeyword();

        Set<String> allClientId = signalKeywordService.findAllClientId();
        for (String clientId : allClientId) {
            log.info("스케줄러에서 clientId: {}에게 이벤트 발행", clientId);
            SignalKeywordEventDto event = new SignalKeywordEventDto(clientId,
                SIGNAL_KEYWORD_LIST_EVENT_MESSAGE, top10WithChange);
            redisPublisher.publishSignalKeywordEvent(event);
        }
    }
}
