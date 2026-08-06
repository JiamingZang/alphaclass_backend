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

import com.imct.alphaclass.controller.AssetController;
import com.imct.alphaclass.service.AssetService;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.utils.TokenUtils;

/**
 * 资产接口契约快照：当前用户资产列表（200）/ 无 token 详情（401）。
 */
@WebMvcTest(AssetController.class)
class AssetControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private AssetService service;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenUtils tokenUtils;

    @Test
    void getAllByUser() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(buildUser(1, "alice"));
        List<Map<String, Object>> assets = new ArrayList<>();
        Map<String, Object> a = new HashMap<>();
        a.put("id", "400");
        a.put("name", "asset400");
        a.put("type", "image");
        assets.add(a);
        when(service.getAllByUser("alice", 1, 5, null)).thenReturn(assets);

        verifySnapshot(get("/user/assets").header("token", buildToken(userService)),
                200, "getAllByUser");
    }

    @Test
    void getById_noToken_returns401() throws Exception {
        when(tokenUtils.getCurrentUser()).thenReturn(null);

        verifySnapshot(get("/user/assets/1"), 401, "getById_noToken");
    }
}
