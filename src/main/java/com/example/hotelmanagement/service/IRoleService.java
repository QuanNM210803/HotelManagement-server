package com.example.hotelmanagement.service;

import com.example.hotelmanagement.model.Role;
import com.example.hotelmanagement.model.User;

import java.util.List;

public interface IRoleService {
    List<Role> getRoles();
    Role createRole(Role theRole);
    void deleteRole(Long id);
    Role findByName(String name);
    User removeUserFromRole(Long userId,Long roleId);
    User assignRoleToUser(Long userId,Long roleId);
    Role removeAllUserFromRole(Long roleId);
}
