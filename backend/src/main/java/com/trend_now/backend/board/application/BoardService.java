package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RealtimeBoardDto;
import com.trend_now.backend.board.dto.FixedBoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.exception.customException.NotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final static String BOARD_NOT_FOUND_MESSAGE = "해당 게시판이 존재하지 않습니다: ";

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
            board.changeDeleted();
            return board;
        } else {
            // 기존 게시판들과 유사도 검증 (비슷한 이름의 게시판이 있으면 해당 게시판의 이름 반환, 없으면 입력받은 이름 그대로 반환)
            String similarBoard = boardCache.findKeywordSimilarity(boardSaveDto.getBoardName());

            // 비슷한 이름의 게시판이 존재하면 이름 업데이트
            if (!similarBoard.equals(boardSaveDto.getBoardName())) {
                // 새로 들어온 키워드가 기존 게시판 대신 저장되므로, 기존 게시판은 Redis에서 삭제한다.
                boardRedisService.deleteBoardRankKey(boardSaveDto.getBoardId(), similarBoard);
                boardRedisService.deleteBoardValueKey(boardSaveDto.getBoardId(), similarBoard);
                Boards board = boardRepository.findByName(similarBoard)
                    .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE));
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


    @Transactional
    public void updateBoardIsDeleted(BoardSaveDto boardSaveDto, boolean isInRedis) {
        // 요구사항을 기반으로 Redis에 있는 게시판 데이터는 DB에도 존재해야 한다.
        Boards findBoards = boardRepository.findByName(boardSaveDto.getBoardName())
            .orElseThrow(
                () -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE + boardSaveDto.getBoardName()));

        if (isInRedis) {
            if (findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        } else {
            if (!findBoards.isDeleted()) {
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
