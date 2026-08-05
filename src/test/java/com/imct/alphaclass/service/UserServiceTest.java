package com.imct.alphaclass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;

/**
 * UserService 行为基线测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO dao;
    @Mock
    private AccessService access;

    @InjectMocks
    private UserService service;

    private User buildUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("alice");
        user.setPassword("secret");
        user.setRole("teacher");
        user.setName("Alice");
        return user;
    }

    @BeforeEach
    void setUp() {
        lenient().when(access.userUrl(anyString())).thenAnswer(invocation ->
                "http://localhost:8080/v2/users/" + invocation.getArgument(0));
        lenient().when(access.userCoursesUrl(anyString())).thenAnswer(invocation ->
                "http://localhost:8080/v2/users/" + invocation.getArgument(0) + "/courses");
    }

    @Test
    void findAll_returnsUsersWithUrlFields() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("username", "alice");
        row.put("role", "teacher");
        row.put("name", "Alice");
        rows.add(row);
        when(dao.findAll()).thenReturn(rows);

        List<Map<String, Object>> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).get("id"));
        assertEquals("http://localhost:8080/v2/users/alice", result.get(0).get("url"));
        assertEquals("http://localhost:8080/v2/users/alice/courses", result.get(0).get("courses_url"));
    }

    @Test
    void register_newUsername_registersAndReturnsUser() {
        when(dao.getByUsername("bob")).thenReturn(null);
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2);
            return null;
        }).when(dao).register(any(User.class));

        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword("pwd");
        bob.setRole("student");
        bob.setName("Bob");

        Map<String, Object> result = service.register(bob);

        assertNotNull(result);
        assertEquals("2", result.get("id"));
        assertEquals("bob", result.get("username"));
        assertEquals("http://localhost:8080/v2/users/bob", result.get("url"));
        assertEquals("http://localhost:8080/v2/users/bob/courses", result.get("courses_url"));
        // 入库密码必须为哈希值，明文不得落库
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(dao).register(captor.capture());
        assertEquals(UserService.hashPassword("bob", "pwd"), captor.getValue().getPassword());
        assertNotEquals("pwd", captor.getValue().getPassword());
    }

    @Test
    void register_duplicateUsername_returnsNull() {
        when(dao.getByUsername("alice")).thenReturn(buildUser());
        User dup = buildUser(); // 参数完整，仅用户名已存在

        assertNull(service.register(dup));
        verify(dao, never()).register(any());
    }

    @Test
    void register_invalidRole_throws400() {
        User hacker = buildUser();
        hacker.setRole("admin");

        ServiceException e = assertThrows(ServiceException.class, () -> service.register(hacker));
        assertEquals(Constants.CODE_400, e.getCode());
        verify(dao, never()).register(any());
    }

    @Test
    void register_blankPassword_throws400() {
        User blank = buildUser();
        blank.setPassword(" ");

        assertThrows(ServiceException.class, () -> service.register(blank));
        verify(dao, never()).register(any());
    }

    @Test
    void register_blankUsername_throws400() {
        User blank = buildUser();
        blank.setUsername("");

        assertThrows(ServiceException.class, () -> service.register(blank));
        verify(dao, never()).register(any());
    }

    @Test
    void login_success_returnsUserWithSignKey() {
        // 模拟库中存量：密码已是哈希值
        when(dao.login(any(User.class))).thenAnswer(invocation -> {
            User stored = buildUser();
            stored.setPassword(UserService.hashPassword("alice", "secret"));
            return stored;
        });

        User login = new User();
        login.setUsername("alice");
        login.setPassword("secret");
        login.setRole("teacher");

        Map<String, Object> result = service.login(login);

        assertNotNull(result);
        assertEquals("1", result.get("id"));
        assertEquals(UserService.hashPassword("alice", "secret"), result.get("sign"));
        assertNull(result.get("password"));
        assertEquals("http://localhost:8080/v2/users/alice", result.get("url"));
        // 查询前密码必须已哈希，明文不参与比对
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(dao).login(captor.capture());
        assertEquals(UserService.hashPassword("alice", "secret"), captor.getValue().getPassword());
        assertNotEquals("secret", captor.getValue().getPassword());
    }

    @Test
    void login_fail_returnsNull() {
        when(dao.login(any(User.class))).thenReturn(null);

        User login = new User();
        login.setUsername("alice");
        login.setPassword("wrong");
        login.setRole("teacher");

        assertNull(service.login(login));
    }

    @Test
    void changePassword_success_returnsUserWithoutPassword() {
        when(dao.updatePasswordByUsername(
                eq(UserService.hashPassword("alice", "newpwd")),
                eq("alice"),
                eq(UserService.hashPassword("alice", "oldpwd")))).thenReturn(true);
        // 真实 DAO 的 getByUsername 不查询 password 字段，此处 mock 不带密码的用户
        User withoutPassword = buildUser();
        withoutPassword.setPassword(null);
        when(dao.getByUsername("alice")).thenReturn(withoutPassword);

        Map<String, String> params = new HashMap<>();
        params.put("password", "oldpwd");
        params.put("new_password", "newpwd");

        Map<String, Object> result = service.changePassword("alice", params);

        assertNotNull(result);
        assertNull(result.get("password"));
        assertNull(result.get("courses_url"));
    }

    @Test
    void changePassword_wrongOldPassword_returnsNull() {
        when(dao.updatePasswordByUsername(anyString(), anyString(), anyString())).thenReturn(false);

        Map<String, String> params = new HashMap<>();
        params.put("password", "wrong");
        params.put("new_password", "newpwd");

        assertNull(service.changePassword("alice", params));
    }

    @Test
    void changeProfile_success_returnsUpdatedUser() {
        when(dao.updateNameByUsername("Alice2", "alice")).thenReturn(true);
        when(dao.getByUsername("alice")).thenReturn(buildUser());

        Map<String, String> params = new HashMap<>();
        params.put("name", "Alice2");

        Map<String, Object> result = service.changeProfile("alice", params);

        assertNotNull(result);
        assertEquals("Alice", result.get("name"));
        assertNull(result.get("courses_url"));
    }

    @Test
    void changeProfile_missingName_returnsNull() {
        assertNull(service.changeProfile("alice", new HashMap<>()));
        verify(dao, never()).updateNameByUsername(anyString(), anyString());
    }
}
