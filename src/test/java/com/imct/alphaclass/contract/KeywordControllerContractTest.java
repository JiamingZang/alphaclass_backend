package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.imct.alphaclass.controller.KeywordController;
import com.imct.alphaclass.service.KeywordService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 关键词接口契约快照：列表（200）/ 非课程创建者添加（401）。
 */
@WebMvcTest(KeywordController.class)
class KeywordControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private KeywordService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllKeywords() throws Exception {
        List<Map<String, Object>> keywords = new ArrayList<>();
        Map<String, Object> k = new HashMap<>();
        k.put("id", "100");
        k.put("keyword", "k1");
        keywords.add(k);
        when(service.getAllKeywordsByCourse("alice", "math")).thenReturn(keywords);

        verifySnapshot(get("/courses/alice/math/keywords"), 200, "getAllKeywords");
    }

    @Test
    void addKeyword_notOwner_returns401() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(null);

        verifySnapshot(post("/courses/alice/math/keywords")
                .header("token", buildToken(userService))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keyword\":\"k2\"}"),
                401, "addKeyword_notOwner");
    }
}
