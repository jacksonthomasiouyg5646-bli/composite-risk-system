package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.common.api.PageResult;
import com.example.usermanagement.common.security.PasswordHashService;
import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;
    private final PasswordHashService passwordHashService;

    public UserServiceImpl(UserCrudMapper mapper, PasswordHashService passwordHashService) {
        this.mapper = mapper;
        this.passwordHashService = passwordHashService;
    }

    @Override
    public PageResult<Map<String, Object>> list(int page, int size, String keyword) {
        PageResult<Map<String, Object>> result = super.list(page, size, keyword);
        result.items().forEach(this::withoutPasswordHash);
        return result;
    }

    @Override
    public Map<String, Object> get(Long id) {
        return withoutPasswordHash(mapper.getUser(id));
    }

    public List<Map<String, Object>> listEnabledUsers() {
        return mapper.listEnabledUsers().stream()
                .map(this::withoutPasswordHash)
                .toList();
    }

    @Override
    public Map<String, Object> create(Map<String, Object> body) {
        Object password = body.remove("password");
        body.remove("password_hash");
        body.put("password_hash", passwordHashService.encode(password == null ? null : String.valueOf(password)));
        return super.create(body);
    }

    @Override
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Object password = body.remove("password");
        body.remove("password_hash");
        if (password != null && !String.valueOf(password).isBlank()) {
            body.put("password_hash", passwordHashService.encode(String.valueOf(password)));
        }
        return super.update(id, body);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteUser(id);
    }

    @Override
    protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) {
        return mapper.listUsers(keyword, limit, offset);
    }

    @Override
    protected long countRows(String keyword) {
        return mapper.countUsers(keyword);
    }

    @Override
    protected void insert(Map<String, Object> body) {
        mapper.insertUser(body);
    }

    @Override
    protected void updateRow(Long id, Map<String, Object> body) {
        mapper.updateUser(id, body);
    }

    private Map<String, Object> withoutPasswordHash(Map<String, Object> user) {
        user.remove("password_hash");
        return user;
    }

}
