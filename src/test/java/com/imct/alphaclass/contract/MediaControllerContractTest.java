package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.imct.alphaclass.controller.MediaController;
import com.imct.alphaclass.service.MediaService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 看点接口契约快照：关键词看点列表（200）/ 单看点不存在（404）。
 */
@WebMvcTest(MediaController.class)
class MediaControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private MediaService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllMedias() throws Exception {
        List<Map<String, Object>> medias = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("id", "200");
        m.put("name", "media200");
        m.put("type", "image");
        medias.add(m);
        when(service.getAllMediasByKeyword("alice", "math", "k1")).thenReturn(medias);

        verifySnapshot(get("/courses/alice/math/k1/medias"), 200, "getAllMedias");
    }

    @Test
    void getMediaById_notFound_returns404() throws Exception {
        when(service.getMediaById("math", "alice", "k1", 999)).thenReturn(null);

        verifySnapshot(get("/courses/alice/math/k1/medias/999"), 404, "getMediaById_notFound");
    }
}
