package com.trend_now.backend.board.util;

import static org.junit.jupiter.api.Assertions.*;

import com.trend_now.backend.search.util.SearchKeywordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class SearchKeywordUtilTest {

    private static final Logger log = LoggerFactory.getLogger(SearchKeywordUtilTest.class);
    @Autowired
    SearchKeywordUtil searchKeywordUtil;

    @Test
    void 텍스트_분해() {
        // given
        String text = "안녕하세요.반갑습니다.";
        String text2 = "apple";

        // when
        String disassembleText1 = searchKeywordUtil.disassembleText(text);
        String disassembleText2 = searchKeywordUtil.disassembleText(text2);

        // then
        assertEquals("ㅇㅏㄴㄴㅕㅇㅎㅏㅅㅔㅇㅛ.ㅂㅏㄴㄱㅏㅂㅅㅡㅂㄴㅣㄷㅏ.", disassembleText1);
        assertEquals( "apple", disassembleText2);

    }

}