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
        Boards findBoards = boardRepository.findByName(boardSaveDto.getName())
                .orElseGet(() -> null);

        if(findBoards == null) return;

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
