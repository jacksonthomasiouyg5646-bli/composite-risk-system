package com.example.usermanagement.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserCrudMapper {
    List<Map<String, Object>> listUsers(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    List<Map<String, Object>> listEnabledUsers();

    long countUsers(@Param("keyword") String keyword);

    Map<String, Object> getUser(@Param("id") Long id);

    void insertUser(Map<String, Object> body);

    void updateUser(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deleteUser(@Param("id") Long id);

    List<Map<String, Object>> listRoles(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countRoles(@Param("keyword") String keyword);

    Map<String, Object> getRole(@Param("id") Long id);

    void insertRole(Map<String, Object> body);

    void updateRole(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deleteRole(@Param("id") Long id);

    List<Map<String, Object>> listPermissions(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countPermissions(@Param("keyword") String keyword);

    Map<String, Object> getPermission(@Param("id") Long id);

    void insertPermission(Map<String, Object> body);

    void updatePermission(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deletePermission(@Param("id") Long id);

    List<Map<String, Object>> listDepartments(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countDepartments(@Param("keyword") String keyword);

    Map<String, Object> getDepartment(@Param("id") Long id);

    void insertDepartment(Map<String, Object> body);

    void updateDepartment(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deleteDepartment(@Param("id") Long id);

    List<Map<String, Object>> listPosts(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countPosts(@Param("keyword") String keyword);

    Map<String, Object> getPost(@Param("id") Long id);

    void insertPost(Map<String, Object> body);

    void updatePost(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deletePost(@Param("id") Long id);

    List<Map<String, Object>> listMenus(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    long countMenus(@Param("keyword") String keyword);

    Map<String, Object> getMenu(@Param("id") Long id);

    void insertMenu(Map<String, Object> body);

    void updateMenu(@Param("id") Long id, @Param("body") Map<String, Object> body);

    void deleteMenu(@Param("id") Long id);
}
