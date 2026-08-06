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

import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.controller.ModelGenerationController;
import com.imct.alphaclass.service.ModelGenerationService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 3D 模型生成接口契约快照：历史列表（200）/ 更新回调无 token（401）。
 */
@WebMvcTest(ModelGenerationController.class)
class ModelGenerationControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private ModelGenerationService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getHistory() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));
        List<GenModelResult> history = new ArrayList<>();
        GenModelResult m = new GenModelResult();
        m.setId(600);
        m.setTask_status("DONE");
        m.setUrl("https://cdn.example.com/600.fbx");
        m.setThumbnail_url("https://cdn.example.com/600.jpg");
        history.add(m);
        when(service.getHistory(1)).thenReturn(history);

        verifySnapshot(get("/services/generate-model/history")
                .header("token", buildToken(userService)),
                200, "getHistory");
    }

    @Test
    void updateResult_noToken_returns401() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(null);

        verifySnapshot(post("/services/generate-model/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
                401, "updateResult_noToken");
    }
}
