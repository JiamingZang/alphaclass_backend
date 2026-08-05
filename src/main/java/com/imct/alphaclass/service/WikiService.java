package com.imct.alphaclass.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.exception.ServiceException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 百科查询服务：基于百度百科网页抓取。
 * <p>
 * 处理三种情况：无歧义词直接取 meta description；常见解释的多义词（如“苹果”）
 * 返回候选列表；无常见解释的多义词（如“递归”）从义项列表抓取。
 */
@Service
public class WikiService {

    /** URL 代理抓取：清洗掉 script/link/style 后返回 HTML（供前端绕过跨域限制） */
    public String getDataFromUrl(String urlParam) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        Request request = new Request.Builder()
            .url(urlParam)
            .method("GET", null)
            .addHeader("Accept", "text/plain")
            .build();
        try {
            Response response = client.newCall(request).execute();
            Document document = Jsoup.parse(response.body().string());
            document.select("script").remove();
            document.select("link").remove();
            document.select("style").remove();
            return document.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    /** 按关键词搜索百科条目，返回条目候选列表（无结果时返回空列表） */
    public List<WikiResult> getWikiItems(String keyword) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();

        String uri = "http://baike.baidu.com/search/word?word=" + keyword;
        Request request = new Request.Builder()
            .url(uri)
            .method("GET", null)
            .addHeader("Accept", "text/plain")
            .build();
        try {
            Response response = client.newCall(request).execute();
            Document document = Jsoup.parse(response.body().string());
            document.select("script").remove();
            document.select("link").remove();
            document.select("style").remove();

            List<WikiResult> results = new ArrayList<WikiResult>();

             //对于无歧义词或有常见解释的多义词（如苹果）可以直接获取百科解释desrciption
            Element kwbox = document.select("meta[name=description]").first();
            if (kwbox == null) {
                return results;
            }
            String description = kwbox.attr("content");

            // 该关键词无解释
            if (description.equals("百度百科是一部内容开放、自由的网络百科全书，旨在创造一个涵盖所有领域知识，服务所有互联网用户的中文知识性百科全书。在这里你可以参与词条编辑，分享贡献你的知识。"))
            {
                return results;
            }

            // 判断是否为有常见解释的多义词，如“苹果”
            String href = "";
            Elements is_polysemant = document.getElementsByClass("polysemantList-wrapper cmn-clearfix");
            if (!is_polysemant.isEmpty()) {
                Elements polysemant_list = is_polysemant.select("li");
                for (Element item : polysemant_list) {
                    Element aNode = item.select("a").first();
                    if (aNode != null) {
                        href = "https://baike.baidu.com" + item.select("a").attr("href");
                        results.add(new WikiResult(keyword, item.select("a").attr("title"), href, ""));
                    } else {
                        Element spanNode = item.select("span").first();
                        results.add(new WikiResult(keyword, spanNode.text(), "", description));
                    }
                }
                return results;
            }

            // 判断是否为无常见解释的多义词，如“递归”
            Elements is_polysemant2 = document.getElementsByClass("lemmaWgt-subLemmaListTitle");
            if (!is_polysemant2.isEmpty()) {
                Element poly_node2 = document.getElementsByClass("custom_dot para-list list-paddingleft-1").first();
                Elements nodes = poly_node2.select("a");
                for (Element node : nodes) {
                    href = "https://baike.baidu.com" + node.attr("href");
                    // 将该义项加入候选列表
                    results.add(new WikiResult(keyword, node.text(), href, ""));
                }
                return results;
            }

            // 该词为无歧义词
            results.add(new WikiResult(keyword, keyword, "", description));
            return results;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceException(Constants.CODE_400, e.toString());
        }
    }

    /** 抓取百科条目详情页的标题与长描述 */
    public Map<String, Object> getLongDescription(String uri) {
        OkHttpClient client = new OkHttpClient().newBuilder()
            .build();
        String description = "";
        Request request = new Request.Builder()
            .url(uri)
            .method("GET", null)
            .addHeader("Accept", "text/plain")
            .build();
        try {
            Response response = client.newCall(request).execute();
            Document document = Jsoup.parse(response.body().string());
            document.select("script").remove();
            document.select("link").remove();
            document.select("style").remove();

            Element kwbox = document.select("meta[name=description]").first();
            description = kwbox.attr("content");
            String keyword = document.getElementsByClass("lemmaWgt-lemmaTitle-title J-lemma-title").first().select("h1").first().text();
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("keyword", keyword);
            result.put("long_description", description);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(Constants.CODE_400, e.toString());
        }
    }

    /** 百科条目（关键词 + 短描述/长描述 + 详情页链接） */
    public static class WikiResult {
        private String keyword;
        private String short_description;
        private String url;
        private String long_description;

        public WikiResult(String keyword, String short_description, String url, String long_description) {
            this.keyword = keyword;
            this.short_description = short_description;
            this.url = url;
            this.long_description = long_description;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getShort_description() {
            return short_description;
        }

        public void setShort_description(String short_description) {
            this.short_description = short_description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLong_description() {
            return long_description;
        }

        public void setLong_description(String long_description) {
            this.long_description = long_description;
        }
    }
}
