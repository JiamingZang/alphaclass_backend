package com.imct.alphaclass.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.imct.alphaclass.common.JSONResult;
import com.imct.alphaclass.service.TranslationService;
import com.imct.alphaclass.service.TranslationService.CN2ENResult;
import com.imct.alphaclass.service.TranslationService.EN2CNResult;
import com.imct.alphaclass.service.TranslationService.YoudaoTranslationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 翻译接口：中英互译，业务逻辑见 {@link TranslationService}（有道 API + 网页例句抓取）。
 */
@RestController
@Tag(name = "翻译", description = "有道中英互译；服务不可用时返回 503")
public class TranslationController {

    private final TranslationService service;

    public TranslationController(TranslationService service) {
        this.service = service;
    }

    /** 中文 → 英文：返回关键词与释义列表 */
    @RequestMapping(value = "/services/zh-to-en", method = RequestMethod.GET)
    @Operation(summary = "中译英", description = "query: word；返回 {keyword, explains[]}")
    public JSONResult translateZhToEN(@RequestParam(name = "word", required = true) String word) {
        YoudaoTranslationResult translation = service.translateCN(word);
        CN2ENResult res = new CN2ENResult(word, translation.translation);
        return JSONResult.successWithData(res);
    }

    /** 英文 → 中文：返回关键词、音标与例句（音标缺失时置空串，不 500） */
    @RequestMapping(value = "/services/en-to-zh", method = RequestMethod.GET)
    @Operation(summary = "英译中", description = "query: word；返回 {keyword, phonetic, explains[]}（phonetic 缺失时为空串）")
    public JSONResult translateENToZh(@RequestParam(name = "word", required = true) String word) {
        YoudaoTranslationResult translation = service.translateEN(word);
        String phonetic = translation.basic == null ? "" : translation.basic.phonetic;
        EN2CNResult res = new EN2CNResult(word, phonetic, translation.exampleSentences);
        return JSONResult.successWithData(res);
    }
}
