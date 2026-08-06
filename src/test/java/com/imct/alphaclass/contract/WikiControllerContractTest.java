package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.imct.alphaclass.controller.WikiController;
import com.imct.alphaclass.service.UserService;
import com.imct.alphaclass.service.WikiService;
import com.imct.alphaclass.service.WikiService.WikiResult;

/**
 * 百科接口契约快照：URL 代理（200）/ SSRF 拦截内网地址（400）。
 */
@WebMvcTest(WikiController.class)
class WikiControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private WikiService service;

    @MockBean
    private UserService userService;

    @Test
    void getProxy_publicUrl() throws Exception {
        when(service.getDataFromUrl("https://example.com/page"))
                .thenReturn("<html><body>x</body></html>");

        verifySnapshot(get("/services/get-proxy").param("url", "https://example.com/page"),
                200, "getProxy_publicUrl");
    }

    @Test
    void getWikiItems() throws Exception {
        WikiResult r = new WikiResult("苹果", "苹果（水果）", "https://baike.baidu.com/item/苹果", "");
        when(service.getWikiItems("苹果")).thenReturn(Collections.singletonList(r));

        verifySnapshot(get("/services/get-wiki-items").param("keyword", "苹果"),
                200, "getWikiItems");
    }
}
