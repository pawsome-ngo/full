package com.pawsome.rescue.auth.repository;

import com.pawsome.rescue.auth.entity.Role;
import com.pawsome.rescue.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findAllByRole_NameIn(List<Role.RoleName> names);
}