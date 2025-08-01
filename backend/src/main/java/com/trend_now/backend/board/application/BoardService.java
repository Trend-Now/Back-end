package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.RealtimeBoardListDto;
import com.trend_now.backend.board.dto.FixedBoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.cache.BoardCache;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCache boardCache;

    private final static String BOARD_NOT_FOUND_MESSAGE = "해당 게시판이 존재하지 않습니다: ";

    @Transactional
    public Long saveBoardIfNotExists(BoardSaveDto boardSaveDto) {
        Boards board = boardRepository.findByName(boardSaveDto.getBoardName())
            .orElseGet(() -> boardRepository.save(
                    Boards.builder()
                        .name(boardSaveDto.getBoardName())
                        .boardCategory(boardSaveDto.getBoardCategory())
                        .build()
                )
            );
        return board.getId();
    }


    @Transactional
    public void updateBoardIsDeleted(BoardSaveDto boardSaveDto, boolean isInRedis) {
        // 요구사항을 기반으로 Redis에 있는 게시판 데이터는 DB에도 존재해야 한다.
        Boards findBoards = boardRepository.findByName(boardSaveDto.getBoardName())
            .orElseThrow(() -> new NotFoundException(BOARD_NOT_FOUND_MESSAGE + boardSaveDto.getBoardName()));

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

    public List<RealtimeBoardListDto> getFixedBoardList() {
        List<Boards> boardList = boardRepository.findByBoardCategory(BoardCategory.FIXED);
        return boardList.stream()
            .map(board -> RealtimeBoardListDto.builder()
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
