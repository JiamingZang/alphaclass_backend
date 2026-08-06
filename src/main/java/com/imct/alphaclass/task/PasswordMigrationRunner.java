package com.imct.alphaclass.task;

import java.util.List;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationRunner implements ApplicationRunner {

    /** SHA-256 十六进制摘要的固定形态 */
    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    private final UserDAO userDAO;

    @Override
    public void run(ApplicationArguments args) {
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

    /** 64 位小写 hex 视为已哈希；null/明文/其他格式视为待迁移 */
    private boolean isHashed(String password) {
        return password != null && SHA256_HEX.matcher(password).matches();
    }
}
