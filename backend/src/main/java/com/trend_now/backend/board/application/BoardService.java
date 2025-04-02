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
        List<Boards> findBoards = boardRepository.findByName(boardSaveDto.getName());

        if (findBoards.isEmpty()) {
            Boards boards = Boards.builder()
                    .name(boardSaveDto.getName())
                    .boardCategory(boardSaveDto.getBoardCategory())
                    .build();

            boardRepository.save(boards);
            return boards.getId();
        }

        return -1L;
    }

    @Transactional
    public void updateBoardIsDeleted(BoardSaveDto boardSaveDto, boolean isInRedis) {
        List<Boards> findBoards = boardRepository.findByName(boardSaveDto.getName());
        if (findBoards.isEmpty()) {
            return;
        }

        Boards board = findBoards.getFirst();
        if(isInRedis) {
            if(board.isDeleted()) {
                board.changeDeleted();
            }
        } else {
            if(!board.isDeleted()) {
                board.changeDeleted();
            }
        }
    }
}
