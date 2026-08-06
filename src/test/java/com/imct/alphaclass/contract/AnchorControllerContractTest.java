package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.imct.alphaclass.controller.AnchorController;
import com.imct.alphaclass.service.AnchorService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 锚点接口契约快照：锚点列表（200）/ 删除不存在的锚点（404）。
 */
@WebMvcTest(AnchorController.class)
class AnchorControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private AnchorService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllAnchors() throws Exception {
        List<Map<String, Object>> anchors = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("id", "300");
        a.put("name", "anchor300");
        anchors.add(a);
        when(service.getAllAnchorsByCourse("alice", "math")).thenReturn(anchors);

        verifySnapshot(get("/courses/alice/math/anchors"), 200, "getAllAnchors");
    }

    @Test
    void deleteAnchor_notFound_returns404() throws Exception {
        when(tokenUtils.requireOwner("alice")).thenReturn(buildUser(1, "alice"));
        when(service.deleteAnchorById("alice", "math", 999)).thenReturn(false);

        verifySnapshot(delete("/courses/alice/math/anchors/999")
                .header("token", buildToken(userService)),
                404, "deleteAnchor_notFound");
    }
}
