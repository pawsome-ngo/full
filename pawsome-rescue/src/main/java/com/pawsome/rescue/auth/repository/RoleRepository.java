package com.pawsome.rescue.auth.repository;

import com.pawsome.rescue.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(Role.RoleName name);
    // --- ADD THIS NEW METHOD ---
    // This method allows finding multiple roles from a list of names.
    List<Role> findAllByNameIn(List<Role.RoleName> names);
}