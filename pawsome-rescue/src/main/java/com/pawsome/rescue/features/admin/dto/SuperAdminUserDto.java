// File: pawsome-rescue/src/main/java/com/pawsome/rescue/features/admin/dto/SuperAdminUserDto.java
package com.pawsome.rescue.features.admin.dto; // Create this package if it doesn't exist

import com.pawsome.rescue.auth.entity.User;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SuperAdminUserDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
//    private Boolean isAuthorized;
    private List<String> roles;
    private User.Position position;
    private LocalDateTime joinedSince;
}