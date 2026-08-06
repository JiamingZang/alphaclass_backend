package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.controller.VideoGenerationController;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.service.VideoGenerationService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 视频生成接口契约快照：历史列表（200）/ 文生视频缺参数（400）。
 */
@WebMvcTest(VideoGenerationController.class)
class VideoGenerationControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private VideoGenerationService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getHistory() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));
        List<GenVideoResult> history = new ArrayList<>();
        GenVideoResult v = new GenVideoResult();
        v.setId(700);
        v.setTask_status("SUCCESS");
        v.setUrl("https://cdn.example.com/700.mp4");
        v.setThumbnail_url("https://cdn.example.com/700.jpg");
        history.add(v);
        when(service.getHistory(1)).thenReturn(history);

        verifySnapshot(get("/services/generate-video/history")
                .header("token", buildToken(userService)),
                200, "getHistory");
    }

    @Test
    void textToVideo_missingParams_returns400() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));

        verifySnapshot(post("/services/generate-video/text-to-video")
                .header("token", buildToken(userService))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
                400, "textToVideo_missingParams");
    }
}
