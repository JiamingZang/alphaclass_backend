package com.imct.alphaclass.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.test.web.servlet.MvcResult;

/**
 * 接口契约快照：将 Controller 的完整 JSON 响应固化为 golden file
 * （src/test/resources/contracts/&lt;测试类&gt;/&lt;用例名&gt;.json），
 * 字段增删、顺序或格式变化都会产生显式 diff——这是接口格式与"原项目"保持一致的最硬约束。
 * <ul>
 * <li>快照缺失：自动生成并让用例失败，提示人工核对后重跑（防止未经确认的基线）</li>
 * <li>刷新基线：-Dcontract.snapshot.update=true 时覆盖生成并直接通过（仅用于有意变更格式时）</li>
 * <li>对比忽略 \r，兼容 Windows/Unix 换行差异</li>
 * </ul>
 */
public final class ContractSnapshot {

    private ContractSnapshot() {
    }

    private static final String ROOT = "src/test/resources/contracts";

    /** 校验响应体与契约快照一致；快照缺失时生成并失败（update 模式直接采纳） */
    public static void verify(Class<?> testClass, String name, MvcResult mvcResult) throws Exception {
        String actual = mvcResult.getResponse().getContentAsString();
        Path file = Paths.get(ROOT, testClass.getSimpleName(), name + ".json");
        boolean update = Boolean.getBoolean("contract.snapshot.update");
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, actual.getBytes(StandardCharsets.UTF_8));
            if (update) {
                return;
            }
            fail("契约快照不存在，已生成：" + file
                    + "。请人工核对响应是否符合预期后重跑；批量采纳请加 -Dcontract.snapshot.update=true");
        }
        String expected = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        if (update) {
            Files.write(file, actual.getBytes(StandardCharsets.UTF_8));
            return;
        }
        assertEquals(normalize(expected), normalize(actual), "接口响应与契约快照不一致：" + file);
    }

    private static String normalize(String content) {
        return content.replace("\r", "").trim();
    }
}
