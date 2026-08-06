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

import com.imct.alphaclass.controller.TextToImageController;
import com.imct.alphaclass.service.TextToImageService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 文生图接口契约快照：历史列表（200）/ 缺少 prompt（400）。
 */
@WebMvcTest(TextToImageController.class)
class TextToImageControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private TextToImageService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getHistory() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));
        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> h = new HashMap<>();
        h.put("id", "500");
        h.put("prompt", "一只猫");
        h.put("status", "SUCCESS");
        history.add(h);
        when(service.getHistory(1)).thenReturn(history);

        verifySnapshot(get("/services/text-to-image/history")
                .header("token", buildToken(userService)),
                200, "getHistory");
    }

    @Test
    void generateImage_missingPrompt_returns400() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));

        verifySnapshot(post("/services/text-to-image/generate-image")
                .header("token", buildToken(userService))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
                400, "generateImage_missingPrompt");
    }
}
