package com.imct.alphaclass.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.service.UserService;

/**
 * PasswordMigrationRunner 行为基线测试：只迁移明文，跳过已哈希，幂等；列长不足自动扩列。
 */
@ExtendWith(MockitoExtension.class)
class PasswordMigrationRunnerTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PasswordMigrationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new PasswordMigrationRunner(userDAO, jdbcTemplate);
        // 迁移开关默认关闭，各用例显式开启以聚焦迁移行为本身
        ReflectionTestUtils.setField(runner, "enabled", true);
    }

    /** information_schema 行：列长 128 视为已达标（默认不触发 ALTER） */
    private void stubColumnLength(long length, boolean nullable) {
        Map<String, Object> row = new HashMap<>();
        row.put("character_maximum_length", length);
        row.put("is_nullable", nullable ? "YES" : "NO");
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(row);
    }

    /** 默认列长 128：各迁移用例聚焦迁移行为本身，不关心扩列 */
    private void stubDefaultColumn() {
        stubColumnLength(128, true);
    }

    private User buildUser(int id, String username, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }

    @Test
    void run_plaintextPassword_isHashedAndUpdated() {
        stubDefaultColumn();
        when(userDAO.findAllWithPassword()).thenReturn(
                Arrays.asList(buildUser(1, "alice", "secret")));

        runner.run(null);

        verify(userDAO).updatePasswordById(UserService.hashPassword("alice", "secret"), 1);
        verify(userDAO, times(1)).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_hashedPassword_isSkipped() {
        stubDefaultColumn();
        String hashed = UserService.hashPassword("bob", "pwd");
        when(userDAO.findAllWithPassword()).thenReturn(
                Arrays.asList(buildUser(2, "bob", hashed)));

        runner.run(null);

        verify(userDAO, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_mixedUsers_onlyMigratesPlaintext() {
        stubDefaultColumn();
        String hashed = UserService.hashPassword("bob", "pwd");
        when(userDAO.findAllWithPassword()).thenReturn(Arrays.asList(
                buildUser(1, "alice", "secret"),
                buildUser(2, "bob", hashed),
                buildUser(3, "carol", null)));

        runner.run(null);

        verify(userDAO).updatePasswordById(UserService.hashPassword("alice", "secret"), 1);
        verify(userDAO).updatePasswordById(UserService.hashPassword("carol", null), 3);
        verify(userDAO, times(2)).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_emptyTable_noop() {
        stubDefaultColumn();
        when(userDAO.findAllWithPassword()).thenReturn(new ArrayList<>());

        runner.run(null);

        verify(userDAO, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_shortNullableColumn_altersAndMigrates() {
        stubColumnLength(32, true);
        when(userDAO.findAllWithPassword()).thenReturn(
                Arrays.asList(buildUser(1, "alice", "secret")));

        runner.run(null);

        verify(jdbcTemplate).execute("alter table user modify column password varchar(128) NULL");
        verify(userDAO).updatePasswordById(UserService.hashPassword("alice", "secret"), 1);
    }

    @Test
    void run_shortNotNullColumn_preservesNotNullSemantics() {
        stubColumnLength(32, false);
        when(userDAO.findAllWithPassword()).thenReturn(new ArrayList<>());

        runner.run(null);

        verify(jdbcTemplate).execute("alter table user modify column password varchar(128) NOT NULL");
    }

    @Test
    void run_sufficientColumn_skipsAlter() {
        stubColumnLength(255, true);
        when(userDAO.findAllWithPassword()).thenReturn(new ArrayList<>());

        runner.run(null);

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void run_disabledByDefault_skipsEverything() {
        // 不开启开关（默认 false）：不查库、不改表、不迁移
        PasswordMigrationRunner disabled = new PasswordMigrationRunner(userDAO, jdbcTemplate);

        disabled.run(null);

        verify(userDAO, never()).findAllWithPassword();
        verify(jdbcTemplate, never()).queryForMap(anyString());
        verify(jdbcTemplate, never()).execute(anyString());
    }
}
