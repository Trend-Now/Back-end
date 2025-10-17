package com.trend_now.backend.board.application;

import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardIsDeletedDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.FixedBoardSaveDto;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.exception.customException.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final static String BOARD_NOT_FOUND_MESSAGE = "해당 게시판이 존재하지 않습니다: ";
    public static final String BOARD_KEY_DELIMITER = ":";

    private final BoardRepository boardRepository;
    private final BoardCache boardCache;
    private final BoardRedisService boardRedisService;

    /**
     * 게시판이 존재하지 않으면 저장, 삭제된 게시판이면 isDeleted 상태 변경 후 반환
     */
    @Transactional
    public Boards saveOrUpdateBoard(BoardSaveDto boardSaveDto) {
        Optional<Boards> optionalBoards = boardRepository.findByName(boardSaveDto.getBoardName());

        if (optionalBoards.isPresent()) {
            // 삭제된 게시판이 다시 실시간 검색어에 등재된 경우, isDeleted 상태 변경
            Boards board = optionalBoards.get();
            if (board.isDeleted()) {
                // 즉, isDeleted 필드가 true에서 false로 변경되는 로직
                // updateBoardIsDeleted()로 삭제된 게시판으로 처리되었다가 시간이 다시 부여되는 경우에 실행됨
                board.changeDeleted();
            }
            return board;
        } else {
            // 기존 게시판들과 유사도 검증 (비슷한 이름의 게시판이 있으면 해당 게시판의 이름 반환, 없으면 입력받은 이름 그대로 반환)
            String similarBoard = boardCache.findKeywordSimilarity(boardSaveDto.getBoardName());

            // 비슷한 이름의 게시판이 존재하면 이름 업데이트
            if (!similarBoard.equals(boardSaveDto.getBoardName())) {
                Boards board = boardRepository.findByName(similarBoard)
                    .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE));
                // 새로 들어온 키워드가 기존 게시판 대신 저장되므로, 기존 게시판은 Redis에서 삭제한다.
                boardRedisService.deleteBoardRankKey(board.getId(), similarBoard);
                boardRedisService.deleteBoardValueKey(board.getId(), similarBoard);
                board.updateName(boardSaveDto.getBoardName());
                return board;
            }

            // 비슷한 게시판이 존재하지 않으면 새로운 게시판 저장
            return boardRepository.save(
                    Boards.builder()
                            .name(boardSaveDto.getBoardName())
                            .boardCategory(boardSaveDto.getBoardCategory())
                            .build()
            );
        }
    }


    /**
     * SignalKeywordJob의 try 구문 안에서는 실시간 검색어 순위에 존재하는 값에 대해서만 isDeleted 상태를 변경하는 문제가 존재 아래 메서드를 통해
     * 만료된 실시간 게시판의 soft delete로 DB의 isDeleted 상태값을 변경
     */
    @Transactional
    public void updateBoardIsDeleted() {
        // TTL이 만료된 키를 ZSET에서 삭제하기 전에 가져옴으로써 만료된 실시간 게시판까지 모두 가져옴
        Set<String> boardRank = boardRedisService.getBoardRank(0, -1);
        if (boardRank == null || boardRank.isEmpty()) {
            return;
        }

        for (String boardRankMember : boardRank) {
            Long boardId;
            try {
                boardId = Long.parseLong(boardRankMember.split(BOARD_KEY_DELIMITER)[1]);
            } catch (NumberFormatException e) {
                log.warn("잘못된 boardRankMember: {}", boardRankMember);
                continue;
            }

            Boards findBoards = boardRepository.findById(boardId)
                    .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE));
            BoardIsDeletedDto boardIsDeletedDto = BoardIsDeletedDto.of(boardId,
                    findBoards.getName());
            boolean isRealTimeBoard = boardRedisService.isRealTimeBoard(boardIsDeletedDto);

            // 현재 실시간 게시판이 아니면서 만료된 게시판인 경우에 isDeleted를 false에서 true로 변경
            if (!isRealTimeBoard && !findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        }
    }

    @Transactional
    public void addFixedBoard(FixedBoardSaveDto fixedBoardSaveDto) {
        boardRepository.save(
                Boards.builder()
                        .name(fixedBoardSaveDto.getBoardName())
                        .boardCategory(BoardCategory.FIXED)
                        .build());
        boardCache.initFixedBoard();
    }

    public List<RealtimeBoardDto> getFixedBoardList() {
        List<Boards> boardList = boardRepository.findByBoardCategory(BoardCategory.FIXED);
        return boardList.stream()
                .map(board -> RealtimeBoardDto.builder()
                        .boardId(board.getId())
                        .boardName(board.getName())
                        .updatedAt(board.getUpdatedAt())
                        .createdAt(board.getCreatedAt())
                        .build())
                .toList();
    }

    public String getBoardNameById(Long boardId) {
        Boards findBoards = boardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE + boardId));
        return findBoards.getName();
    }
}
