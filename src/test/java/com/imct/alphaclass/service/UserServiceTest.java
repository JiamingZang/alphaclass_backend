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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.UserDAO;

/**
 * UserService 行为基线测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDAO dao;

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
        ReflectionTestUtils.setField(service, "baseUrl", "https://SERVER_IP_PLACEHOLDER/v2");
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
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/users/alice", result.get(0).get("url"));
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/users/alice/courses", result.get(0).get("courses_url"));
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
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/users/bob", result.get("url"));
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/users/bob/courses", result.get("courses_url"));
    }

    @Test
    void register_duplicateUsername_returnsNull() {
        when(dao.getByUsername("alice")).thenReturn(buildUser());

        User dup = new User();
        dup.setUsername("alice");

        assertNull(service.register(dup));
        verify(dao, never()).register(any());
    }

    @Test
    void login_success_returnsUserWithPassword() {
        when(dao.login(any(User.class))).thenReturn(buildUser());

        User login = new User();
        login.setUsername("alice");
        login.setPassword("secret");
        login.setRole("teacher");

        Map<String, Object> result = service.login(login);

        assertNotNull(result);
        assertEquals("1", result.get("id"));
        assertEquals("secret", result.get("password"));
        assertEquals("https://SERVER_IP_PLACEHOLDER/v2/users/alice", result.get("url"));
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
    void change_password_success_returnsUserWithNewPassword() {
        when(dao.updatePasswordByUsername(eq("newpwd"), eq("alice"), eq("oldpwd"))).thenReturn(true);
        when(dao.getByUsername("alice")).thenReturn(buildUser());

        Map<String, String> params = new HashMap<>();
        params.put("password", "oldpwd");
        params.put("new_password", "newpwd");

        Map<String, Object> result = service.change_password("alice", params);

        assertNotNull(result);
        assertEquals("newpwd", result.get("password"));
        assertNull(result.get("courses_url"));
    }

    @Test
    void change_password_wrongOldPassword_returnsNull() {
        when(dao.updatePasswordByUsername(anyString(), anyString(), anyString())).thenReturn(false);

        Map<String, String> params = new HashMap<>();
        params.put("password", "wrong");
        params.put("new_password", "newpwd");

        assertNull(service.change_password("alice", params));
    }

    @Test
    void change_profile_success_returnsUpdatedUser() {
        when(dao.updateNameByUsername("Alice2", "alice")).thenReturn(true);
        when(dao.getByUsername("alice")).thenReturn(buildUser());

        Map<String, String> params = new HashMap<>();
        params.put("name", "Alice2");

        Map<String, Object> result = service.change_profile("alice", params);

        assertNotNull(result);
        assertEquals("Alice", result.get("name"));
        assertNull(result.get("courses_url"));
    }

    @Test
    void change_profile_missingName_returnsNull() {
        assertNull(service.change_profile("alice", new HashMap<>()));
        verify(dao, never()).updateNameByUsername(anyString(), anyString());
    }
}
