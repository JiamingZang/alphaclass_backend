package com.imct.alphaclass.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.imct.alphaclass.common.JSONResult;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.apache.commons.text.StringEscapeUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
public class TranslationController {

    @RequestMapping(value = "/services/zh-to-en", method = RequestMethod.GET)
    public JSONResult translateZhToEN(@RequestParam(name = "word",required = true) String word) throws IOException{
        YoudaoTranslationResult translation = translateCN(word);
        CN2ENResult res = new CN2ENResult(word, translation.basic.explains);
        return JSONResult.successWithData(res);
    }
    
    @RequestMapping(value = "/services/en-to-zh", method = RequestMethod.GET)
    public JSONResult translateENToZh(@RequestParam(name = "word",required = true) String word) throws IOException{
        YoudaoTranslationResult translation = translateEN(word);
        EN2CNResult res = new EN2CNResult(word, translation.basic.phonetic, translation.exampleSentences);
        return JSONResult.successWithData(res);
    }

    public YoudaoTranslationResult translateCN(String q){
        Map<String, String> dic = getRequestMap(q, "zh-CHS", "en");//所有上传的参数将写入该字典中
        String url = "https://openapi.youdao.com/api";
        return get(url, dic);

    }

    public YoudaoTranslationResult translateEN(String q){
        Map<String, String> dic = getRequestMap(q, "en", "zh-CHS");//所有上传的参数将写入该字典中
        String url = "https://openapi.youdao.com/api";
        YoudaoTranslationResult res = get(url, dic);
        res.exampleSentences = getExampleSentences(q);
        return res;
        
    }

    public class EN2CNResult{
        public String keyword;
        public String phonetic;
        public List<ExampleSentencesResult> exampleSentences;
        public EN2CNResult(String keyword, String phonetic, List<ExampleSentencesResult> exampleSentences) {
            this.keyword = keyword;
            this.phonetic = phonetic;
            this.exampleSentences = exampleSentences;
        }
    }

    public class CN2ENResult {
        public String keyword;
        public List<String> explains;
        public CN2ENResult(String keyword, List<String> explains) {
            this.keyword = keyword;
            this.explains = explains;
        }
        
    }

    public class WebResult {
        public List<String> value;
        public String key;
    }

    public class BasicResult {
        public String phonetic;
        public List<String> explains;
    }

    public class YoudaoTranslationResult {
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

    public class ExampleSentencesResult {
        public String enExampleSentence;
        public String cnExampleSentence;
        public ExampleSentencesResult(String enExampleSentence, String cnExampleSentence) {
            this.enExampleSentence = enExampleSentence;
            this.cnExampleSentence = cnExampleSentence;
        }
        
    }

    private static final String APP_KEY = "REPLACED_YOUDAO_APP_KEY";

    private static final String APP_SECRET = "REPLACED_YOUDAO_APP_SECRET";

    //获取请求参数的map
    public Map<String,String> getRequestMap(String q,String from,String to) {
        Map<String,String> params = new HashMap<String,String>();
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", from);
        params.put("to", to);
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = APP_KEY + truncate(q) + salt + curtime + APP_SECRET;
        String sign = getDigest(signStr);
        params.put("appKey", APP_KEY);
        params.put("q", q);
        params.put("salt", salt);
        params.put("sign", sign);
        return params;
    }

    //抄的，没什么用
    public YoudaoTranslationResult requestForHttp(String url,Map<String,String> params) throws IOException {

        /** 创建HttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

        /** httpPost */
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String,String>> it = params.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,String> en = it.next();
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key,value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList,"UTF-8"));
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        
        Header[] contentType = httpResponse.getHeaders("Content-Type");
        String json = "";
        if("audio/mp3".equals(contentType[0].getValue())){
            //如果响应是wav
            HttpEntity httpEntity = httpResponse.getEntity();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(baos);
            byte[] result = baos.toByteArray();
            EntityUtils.consume(httpEntity);
            if(result != null){//合成成功
                String file = "合成的音频存储路径"+System.currentTimeMillis() + ".mp3";
                byte2File(result,file);
            }
        }else{
            /** 响应不是音频流，直接显示结果 */
            HttpEntity httpEntity = httpResponse.getEntity();
            json = EntityUtils.toString(httpEntity,"UTF-8");
            EntityUtils.consume(httpEntity);
        }
        return JSON.parseObject(StringEscapeUtils.unescapeJson(json),new TypeReference<YoudaoTranslationResult>(){});

        
    }

    /**
     * 生成加密字段
     */
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

    /**
    *
    * @param result 音频字节流
    * @param file 存储路径
    */
    private void byte2File(byte[] result, String file) {
        File audioFile = new File(file);
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(audioFile);
            fos.write(result);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }

    public List<ExampleSentencesResult> getExampleSentences(String enq){
        List<ExampleSentencesResult> res = new ArrayList<ExampleSentencesResult>();
        String wordURL = "https://www.youdao.com/result?word=" + enq + "&lang=en";
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
            
            if (enHtmlNodes!=null && cnHtmlNodes!=null) {
                for (int i = 0; i < enHtmlNodes.size(); i++) {
                    ExampleSentencesResult result = 
                    new ExampleSentencesResult(enHtmlNodes.get(i).text(),cnHtmlNodes.get(i).text());
                    res.add(result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public YoudaoTranslationResult get(String url, Map<String,String> map){
        StringBuilder builder = new StringBuilder();

        int i = 0;
        for ( Map.Entry<String,String> entry : map.entrySet()) {
            if (i>0) {
                builder.append("&");
            }
            builder.append(entry.getKey()+"="+entry.getValue());
            i++;
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(url+"?"+builder.toString())
            .method("GET", null)
            .build();
        try {
            Response response = client.newCall(request).execute();
            String res = response.body().string();
            return JSON.parseObject(res,new TypeReference<YoudaoTranslationResult>(){});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }
}