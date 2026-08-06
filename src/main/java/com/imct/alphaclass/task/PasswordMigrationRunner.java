package com.imct.alphaclass.task;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.service.UserService;

/**
 * 存量密码迁移：加盐哈希上线前注册的账号在库中仍为明文，登录永远比对不上。
 * 启动时把非 SHA-256 摘要格式的密码原地升级为 sha256(username + password)。
 * 幂等：已迁移的行（64 位小写 hex）自动跳过，重复启动无副作用。
 * <p>
 * 注意：若用户密码恰好是 64 位十六进制字符串，会被误判为已哈希而跳过（概率极低，
 * 可通过重设密码恢复），因此迁移前不要手工哈希已有数据。
 * <p>
 * 开关：默认关闭（{@code app.password-migration.enabled=false}）。迁移不可逆且会改写
 * 存量明文，必须与“线上切换到哈希登录”同批次进行，由运维显式设置
 * {@code PASSWORD_MIGRATION_ENABLED=true} 一次性执行，完成后可关闭。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationRunner implements ApplicationRunner {

    /** SHA-256 十六进制摘要的固定形态 */
    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    /** 哈希后密码固定 64 字符；列长阈值留出换算法余量（如 bcrypt 60 字符） */
    private static final int PASSWORD_COLUMN_LENGTH = 128;

    private final UserDAO userDAO;
    private final JdbcTemplate jdbcTemplate;

    /** 迁移开关：默认关闭，仅显式开启时执行（防止误碰生产库存量明文） */
    @Value("${app.password-migration.enabled:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("存量密码迁移已关闭（app.password-migration.enabled=false），跳过");
            return;
        }
        ensurePasswordColumnLength();
        List<User> users = userDAO.findAllWithPassword();
        int migrated = 0;
        for (User user : users) {
            if (!isHashed(user.getPassword())) {
                userDAO.updatePasswordById(
                        UserService.hashPassword(user.getUsername(), user.getPassword()), user.getId());
                migrated++;
            }
        }
        if (migrated > 0) {
            log.info("存量密码迁移完成：{} 个明文账号已升级为加盐哈希", migrated);
        }
    }

    /**
     * 密码升级为 64 字符 SHA-256 后，列长不足会写入失败（Data too long），
     * 启动时检查 information_schema，不足则 ALTER 扩列。幂等：列长已达标直接跳过，
     * 保留原列 NULL/NOT NULL 语义；无 ALTER 权限时启动失败并给出明确报错。
     * 仅在开关开启时调用（本方法修改表结构，同样不可随意执行）。
     */
    private void ensurePasswordColumnLength() {
        Map<String, Object> column = jdbcTemplate.queryForMap(
                "select character_maximum_length, is_nullable from information_schema.columns "
                        + "where table_schema = database() and table_name = 'user' and column_name = 'password'");
        long current = ((Number) column.get("character_maximum_length")).longValue();
        if (current >= PASSWORD_COLUMN_LENGTH) {
            return;
        }
        String nullable = "YES".equalsIgnoreCase(String.valueOf(column.get("is_nullable"))) ? "NULL" : "NOT NULL";
        jdbcTemplate.execute("alter table user modify column password varchar(" + PASSWORD_COLUMN_LENGTH + ") "
                + nullable);
        log.warn("user.password 列长度 {} < {}，已自动扩列以容纳加盐哈希", current, PASSWORD_COLUMN_LENGTH);
    }

    /** 64 位小写 hex 视为已哈希；null/明文/其他格式视为待迁移 */
    private boolean isHashed(String password) {
        return password != null && SHA256_HEX.matcher(password).matches();
    }
}
