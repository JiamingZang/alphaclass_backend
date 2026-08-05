package com.imct.alphaclass.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.utils.MapUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 有道翻译服务：中英互译。
 * <p>
 * 实现分为两部分：调用有道开放 API 获取基础翻译（需按文档生成 v3 签名），
 * 再抓取有道网页补充音标与例句（API 响应中不包含这些信息）。
 */
@Service
public class TranslationService {

    @Value("${ai.youdao.app-key}")
    private String appKey;

    @Value("${ai.youdao.app-secret}")
    private String appSecret;

    /** 中文 → 英文翻译 */
    public YoudaoTranslationResult translateCN(String q) {
        Map<String, String> dic = getRequestMap(q, "zh-CHS", "en");//所有上传的参数将写入该字典中
        String url = "https://openapi.youdao.com/api";
        return get(url, dic);
    }

    /** 英文 → 中文翻译（含音标与例句） */
    public YoudaoTranslationResult translateEN(String q) {
        Map<String, String> dic = getRequestMap(q, "en", "zh-CHS");//所有上传的参数将写入该字典中
        String url = "https://openapi.youdao.com/api";
        YoudaoTranslationResult res = get(url, dic);
        res.exampleSentences = getExampleSentences(q);
        return res;
    }

    /** 组装有道 API 请求参数（v3 签名：appKey + 截断文本 + salt + curtime + appSecret 的 SHA-256） */
    public Map<String, String> getRequestMap(String q, String from, String to) {
        Map<String, String> params = new HashMap<String, String>();
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", from);
        params.put("to", to);
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = appKey + truncate(q) + salt + curtime + appSecret;
        String sign = getDigest(signStr);
        params.put("appKey", appKey);
        params.put("q", q);
        params.put("salt", salt);
        params.put("sign", sign);
        return params;
    }

    /** 生成 SHA-256 摘要（16 进制大写） */
    public String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /** 有道签名规则：超过 20 字符的文本截取首尾 10 字符，中间拼接长度 */
    public String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }

    /** 从有道网页抓取英文例句（英 → 中成对） */
    public List<ExampleSentencesResult> getExampleSentences(String enq) {
        List<ExampleSentencesResult> res = new ArrayList<ExampleSentencesResult>();
        String wordURL = "https://www.youdao.com/result?word=" + MapUtils.urlEncode(enq) + "&lang=en";
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(wordURL)
            .method("GET", null)
            .build();
        try {
            Response response = client.newCall(request).execute();
            Document document = Jsoup.parse(response.body().string());
            document.select("script").remove();
            document.select("link").remove();
            document.select("style").remove();
            Elements enHtmlNodes = document.getElementsByClass("sen-eng");
            Elements cnHtmlNodes = document.getElementsByClass("sen-ch");
            
            if (enHtmlNodes != null && cnHtmlNodes != null) {
                for (int i = 0; i < enHtmlNodes.size(); i++) {
                    ExampleSentencesResult result = 
                    new ExampleSentencesResult(enHtmlNodes.get(i).text(), cnHtmlNodes.get(i).text());
                    res.add(result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 调用有道翻译 API 并补充音标与例句：
     * 基础翻译来自 openapi 响应，音标从响应 webdict 链接抓取，例句从 mobile 单词典接口抓取。
     */
    public YoudaoTranslationResult get(String url, Map<String, String> map) {
        StringBuilder builder = new StringBuilder();

        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (i > 0) {
                builder.append("&");
            }
            builder.append(entry.getKey() + "=" + entry.getValue());
            i++;
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(url + "?" + builder.toString())
            .method("GET", null)
            .build();
        try {
            //获取基础结果
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            WebDictResult res1 = JSON.parseObject(res, new TypeReference<WebDictResult>() {});
            //获取webdict结果获取音标
            String webDictUrl = res1.webdict.get("url");
            request = new Request.Builder().url(webDictUrl).method("GET", null).build();
            String res2 = client.newCall(request).execute().body().string();
            Matcher matcher = Pattern
                            .compile("<span class=\"phonetic\">\\[(.*?)\\]</span>")
                            .matcher(res2);
            String find = matcher.find() ? matcher.group(1) : "";
            //获取例句结果
            String exampleSentenceUrl = "https://mobile.youdao.com/singledict?q="
                                    + res1.query
                                    + "&dict=blng_sents_part&le=eng&more=false";
            request = new Request.Builder().url(exampleSentenceUrl).method("GET", null).build();
            String exampleSentenceRes = client.newCall(request).execute().body().string();
            Document document = Jsoup.parse(exampleSentenceRes);
            
            Elements cols = document.getElementsByClass("col2");
            List<ExampleSentencesResult> exampleSentencesResults = new ArrayList<>();
            for (Element ele : cols) {
                exampleSentencesResults.add(
                    new ExampleSentencesResult(
                        ele.getElementsByTag("p").first().text(),
                        ele.getElementsByClass("grey").text()
                    )
                );
            }
            YoudaoTranslationResult res3 = JSON.parseObject(res, new TypeReference<YoudaoTranslationResult>() {});
            BasicResult basic = new BasicResult();
            basic.phonetic = "[" + find + "]";
            res3.basic = basic;
            res3.exampleSentences = exampleSentencesResults;
            return res3;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 英 → 中接口响应（keyword + 音标 + 例句） */
    public static class EN2CNResult {
        public String keyword;
        public String phonetic;
        public List<ExampleSentencesResult> exampleSentences;

        public EN2CNResult(String keyword, String phonetic, List<ExampleSentencesResult> exampleSentences) {
            this.keyword = keyword;
            this.phonetic = phonetic;
            this.exampleSentences = exampleSentences;
        }
    }

    /** 中 → 英接口响应（keyword + 释义列表） */
    public static class CN2ENResult {
        public String keyword;
        public List<String> explains;

        public CN2ENResult(String keyword, List<String> explains) {
            this.keyword = keyword;
            this.explains = explains;
        }
    }

    /** 有道 API 响应中的网络释义条目 */
    public static class WebResult {
        public List<String> value;
        public String key;
    }

    /** 有道 API 响应中的基础信息（音标、释义） */
    public static class BasicResult {
        public String phonetic;
        public List<String> explains;
    }

    /** 有道 API 翻译结果（扩展了网页补充的 exampleSentences） */
    public static class YoudaoTranslationResult {
        public String errorCode;
        public String query;
        public List<String> translation;
        public BasicResult basic;
        public List<WebResult> web;
        public String l;//源语言和目标语言
        public String tSpeakUrl;
        public String speakUrl;
        public List<String> returnPhrase;//单词校验结果
        public List<ExampleSentencesResult> exampleSentences;
    }

    /** 网页例句（英中成对） */
    public static class ExampleSentencesResult {
        public String enExampleSentence;
        public String cnExampleSentence;

        public ExampleSentencesResult(String enExampleSentence, String cnExampleSentence) {
            this.enExampleSentence = enExampleSentence;
            this.cnExampleSentence = cnExampleSentence;
        }
    }

    /** 有道 API 原始响应结构（webdict 链接用于抓音标） */
    public static class WebDictResult {
        public String tSpeakUrl;
        public String requestId;
        public String query;
        public List<String> translation;
        public String errorCode;
        public String l;//源语言和目标语言
        public Boolean isWord;
        public String speakUrl;
        public Map<String, String> mTerminalDict;
        public Map<String, String> webdict;
        public Map<String, String> dict;
    }
}
