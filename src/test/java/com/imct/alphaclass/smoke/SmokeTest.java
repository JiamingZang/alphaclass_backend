package com.imct.alphaclass.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 冒烟集成测试：启动完整 Spring 容器并连接真实数据库，验证核心链路
 * users → courses → keywords → medias 的响应结构契约。
 *
 * <p>对应原手动冒烟流程（curl 启动的应用），固化为可重复执行的自动化测试。
 * 数据动态发现（取库中第一个用户/课程/关键词），不写死用户名，
 * 因此只要库中有任意一条完整链路数据即可运行。
 *
 * <p>默认跳过（不依赖真实库，避免拖慢/失败常规构建）；显式启用：
 * <pre>
 *   mvn test -Dsmoke.test=true
 * </pre>
 * 数据库不可达时自动跳过（assumeTrue），不会导致构建失败。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.datasource.hikari.connection-timeout=5000")
@EnabledIfSystemProperty(named = "smoke.test", matches = "true")
class SmokeTest {

    @Resource
    private TestRestTemplate rest;

    @Resource
    private DataSource dataSource;

    /** 数据库连通性探测结果缓存，避免每个测试都重复等待连接超时 */
    private static Boolean dbReachable = null;

    @BeforeEach
    void requireDatabase() {
        if (dbReachable == null) {
            try (Connection conn = dataSource.getConnection()) {
                dbReachable = true;
            } catch (Exception e) {
                dbReachable = false;
            }
        }
        assumeTrue(dbReachable, "数据库不可达，跳过冒烟测试（需要可访问真实库的网络环境）");
    }

    /** 用户列表接口可用 */
    @Test
    void usersListReturns200() {
        ResponseEntity<String> resp = rest.getForEntity("/users", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(JSON.parseArray(resp.getBody()), "响应应为 JSON 数组");
    }

    /**
     * 课程链路：取库中第一条完整链路（第一个有课程的用户），
     * 验证其课程列表的结构契约（id 字符串化、user 嵌套、url 填充）。
     */
    @Test
    void courseLinkageStructure() {
        String[] link = firstLink();
        assumeTrue(link != null, "库中无用户+课程链路，跳过课程结构验证");
        String username = link[0];

        ResponseEntity<String> resp = rest.getForEntity("/users/{username}/courses", String.class, username);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONArray courses = JSON.parseArray(resp.getBody());
        assumeTrue(courses != null && !courses.isEmpty(), "该用户无课程，跳过课程结构验证");
        JSONObject course = courses.getJSONObject(0);
        assertEquals(String.class, course.get("id").getClass(), "course.id 应为字符串");
        assertNotNull(course.getJSONObject("user"), "course 应嵌套 user");
        assertNotNull(course.getString("keywords_url"), "course 应包含 keywords_url");
        assertNotNull(course.getString("anchors_url"), "course 应包含 anchors_url");
        assertNotNull(course.getString("created_at"), "course 应包含格式化后的 created_at");
    }

    /**
     * 关键词/看点链路：沿第一个用户 → 第一个课程 → 第一个关键词，
     * 验证 keyword 嵌套 medias、media 嵌套 color/anchor/asset 的结构契约。
     */
    @Test
    void keywordAndMediaStructure() {
        String[] link = firstLink();
        assumeTrue(link != null, "库中无用户+课程链路，跳过关键词链路验证");
        String username = link[0];
        String courseName = link[1];

        ResponseEntity<String> resp = rest.exchange("/courses/{owner}/{course}/keywords", HttpMethod.GET, null,
                String.class, username, courseName);
        assertEquals(HttpStatus.OK, resp.getStatusCode());

        JSONArray keywords = JSON.parseArray(resp.getBody());
        assumeTrue(keywords != null && !keywords.isEmpty(), "该课程无关键词，跳过看点结构验证");
        JSONObject keyword = keywords.getJSONObject(0);
        assertEquals(String.class, keyword.get("id").getClass(), "keyword.id 应为字符串");
        assertNotNull(keyword.get("medias"), "keyword 应嵌套 medias");

        JSONArray medias = keyword.getJSONArray("medias");
        assumeTrue(medias != null && !medias.isEmpty(), "该关键词无看点，跳过看点结构验证");
        JSONObject media = medias.getJSONObject(0);
        assertEquals(String.class, media.get("id").getClass(), "media.id 应为字符串");
        assertNotNull(media.getJSONObject("color"), "media 应嵌套 color");
        assertNotNull(media.getJSONObject("anchor"), "media 应嵌套 anchor");
        assertTrue(media.containsKey("asset"), "media 应包含 asset 字段（可能为 null）");
    }

    /** 不存在的关键词返回业务 404（ServiceException → {message}） */
    @Test
    void missingKeywordReturns404() {
        String[] link = firstLink();
        assumeTrue(link != null, "库中无用户+课程链路，跳过 404 语义验证");

        ResponseEntity<String> resp = rest.exchange("/courses/{owner}/{course}/not-exist-keyword",
                HttpMethod.GET, null, String.class, link[0], link[1]);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        JSONObject body = JSON.parseObject(resp.getBody());
        assertNotNull(body.getString("message"), "404 响应应包含 message 说明");
    }

    /** 写操作无 token 返回 401（拦截器鉴权语义） */
    @Test
    void writeWithoutTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange("/user/assets", HttpMethod.POST,
                new HttpEntity<String>("{}", headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNotNull(JSON.parseObject(resp.getBody()).getString("message"));
    }

    /**
     * 取库中第一条完整链路：遍历用户列表（最多前 10 个），
     * 返回第一个有课程的用户及其第一个课程名 [username, courseName]；
     * 库中无完整链路时返回 null（由调用方 assumeTrue 跳过）。
     */
    private String[] firstLink() {
        ResponseEntity<String> resp = rest.getForEntity("/users", String.class);
        JSONArray users = JSON.parseArray(resp.getBody());
        if (users == null) {
            return null;
        }
        for (int i = 0; i < users.size() && i < 10; i++) {
            String username = users.getJSONObject(i).getString("username");
            String courseName = firstCourseName(username);
            if (courseName != null) {
                return new String[] { username, courseName };
            }
        }
        return null;
    }

    /** 取该用户第一个课程名；无课程时返回 null */
    private String firstCourseName(String username) {
        ResponseEntity<String> resp = rest.getForEntity("/users/{username}/courses", String.class, username);
        JSONArray courses = JSON.parseArray(resp.getBody());
        if (courses == null || courses.isEmpty()) {
            return null;
        }
        return courses.getJSONObject(0).getString("name");
    }
}
