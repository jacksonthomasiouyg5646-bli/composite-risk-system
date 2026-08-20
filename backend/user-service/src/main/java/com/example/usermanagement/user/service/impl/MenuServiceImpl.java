package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MenuServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;

    public MenuServiceImpl(UserCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getMenu(id); }
    @Override public void delete(Long id) { mapper.deleteMenu(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listMenus(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countMenus(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertMenu(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateMenu(id, body); }
}
