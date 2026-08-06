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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.service.UserService;

/**
 * PasswordMigrationRunner 行为基线测试：只迁移明文，跳过已哈希，幂等。
 */
@ExtendWith(MockitoExtension.class)
class PasswordMigrationRunnerTest {

    @Mock
    private UserDAO userDAO;

    private PasswordMigrationRunner runner;

    @BeforeEach
    void setUp() {
        runner = new PasswordMigrationRunner(userDAO);
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
        when(userDAO.findAllWithPassword()).thenReturn(
                Arrays.asList(buildUser(1, "alice", "secret")));

        runner.run(null);

        verify(userDAO).updatePasswordById(UserService.hashPassword("alice", "secret"), 1);
        verify(userDAO, times(1)).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_hashedPassword_isSkipped() {
        String hashed = UserService.hashPassword("bob", "pwd");
        when(userDAO.findAllWithPassword()).thenReturn(
                Arrays.asList(buildUser(2, "bob", hashed)));

        runner.run(null);

        verify(userDAO, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    void run_mixedUsers_onlyMigratesPlaintext() {
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
        when(userDAO.findAllWithPassword()).thenReturn(new ArrayList<>());

        runner.run(null);

        verify(userDAO, never()).updatePasswordById(anyString(), anyInt());
    }
}
