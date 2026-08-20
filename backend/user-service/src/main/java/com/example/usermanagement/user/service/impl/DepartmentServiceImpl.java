package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DepartmentServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;

    public DepartmentServiceImpl(UserCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getDepartment(id); }
    @Override public void delete(Long id) { mapper.deleteDepartment(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listDepartments(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countDepartments(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertDepartment(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateDepartment(id, body); }
}
