package com.example.usermanagement.user.service.impl;

import com.example.usermanagement.user.mapper.UserCrudMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PostServiceImpl extends AbstractUserCrudService {
    private final UserCrudMapper mapper;

    public PostServiceImpl(UserCrudMapper mapper) {
        this.mapper = mapper;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getPost(id); }
    @Override public void delete(Long id) { mapper.deletePost(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listPosts(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countPosts(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertPost(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updatePost(id, body); }
}
