package com.pawsome.rescue.features.inventory.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "first_aid_kits")
@Getter
@Setter
public class FirstAidKit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "firstAidKit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FirstAidKitItem> items = new ArrayList<>();
}