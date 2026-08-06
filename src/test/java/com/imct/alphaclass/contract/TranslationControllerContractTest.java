package com.imct.alphaclass.contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.controller.TranslationController;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.service.TranslationService;
import com.imct.alphaclass.service.TranslationService.BasicResult;
import com.imct.alphaclass.service.TranslationService.YoudaoTranslationResult;
import com.imct.alphaclass.service.UserService;

/**
 * 翻译接口契约快照：中译英（200）/ 第三方服务不可用（503）。
 */
@WebMvcTest(TranslationController.class)
class TranslationControllerContractTest extends AbstractControllerContractTest {

    @MockBean
    private TranslationService service;

    @MockBean
    private UserService userService;

    @Test
    void zhToEn() throws Exception {
        YoudaoTranslationResult t = new YoudaoTranslationResult();
        t.translation = Arrays.asList("hello");
        t.basic = new BasicResult();
        t.basic.phonetic = "[həlo]";
        when(service.translateCN("苹果")).thenReturn(t);

        verifySnapshot(get("/services/zh-to-en").param("word", "苹果"), 200, "zhToEn");
    }

    @Test
    void enToZh_serviceDown_returns503() throws Exception {
        when(service.translateEN("apple"))
                .thenThrow(new ServiceException(Constants.CODE_503, "翻译服务不可用"));

        verifySnapshot(get("/services/en-to-zh").param("word", "apple"), 503, "enToZh_serviceDown");
    }
}
