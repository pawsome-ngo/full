package com.pawsome.rescue.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 50) // <-- MODIFIED
    private RoleName name;

    public enum RoleName {
        MEMBER,
        RESCUE_CAPTAIN,
        INVENTORY_MANAGER,
        ADMIN,
        SUPER_ADMIN
    }
}