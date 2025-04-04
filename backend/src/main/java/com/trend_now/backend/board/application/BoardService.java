package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public Long saveBoardIfNotExists(BoardSaveDto boardSaveDto) {
        Boards board = boardRepository.findByName(boardSaveDto.getName())
                .orElseGet(() -> boardRepository.save(
                        Boards.builder()
                                .name(boardSaveDto.getName())
                                .boardCategory(boardSaveDto.getBoardCategory())
                                .build()
                )
        );
        return board.getId();
    }


    @Transactional
    public void updateBoardIsDeleted(BoardSaveDto boardSaveDto, boolean isInRedis) {
        // 요구사항을 기반으로 Redis에 있는 게시판 데이터는 DB에도 존재해야 한다.
        Boards findBoards = boardRepository.findByName(boardSaveDto.getName())
                .orElseThrow(
                        () -> new IllegalStateException("해당 게시판이 존재하지 않습니다: " + boardSaveDto.getName())
                );

        if(isInRedis) {
            if(findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        } else {
            if(!findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        }
    }
}
