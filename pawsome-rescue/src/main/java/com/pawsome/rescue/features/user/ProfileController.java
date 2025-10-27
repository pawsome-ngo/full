// File: pawsome-rescue/src/main/java/com/pawsome/rescue/features/user/ProfileController.java
package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.UpdateLocationDto;
import com.pawsome.rescue.auth.dto.UpdatePasswordDto;
import com.pawsome.rescue.features.user.dto.UserProfileDto;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
// --- ✨ Import new DTOs ---
import com.pawsome.rescue.features.user.dto.UpdateVehicleDto;
import com.pawsome.rescue.features.user.dto.UpdateShelterDto;
// --- End Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CredentialsRepository credentialsRepository;


    private Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();
    }
    @PutMapping("/location")
    public ResponseEntity<?> updateUserLocation(Authentication authentication, @RequestBody UpdateLocationDto locationDto) {
        Long userId = getUserIdFromAuthentication(authentication);
        try {
            profileService.updateUserLocation(userId, locationDto.getLatitude(), locationDto.getLongitude());
            return ResponseEntity.ok().body("Location updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping
    public ResponseEntity<UserProfileDto> getUserProfile(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        UserProfileDto userProfile = profileService.getUserProfile(userId);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/availability")
    public ResponseEntity<?> updateUserAvailability(Authentication authentication, @RequestBody Map<String, String> body) {
        Long userId = getUserIdFromAuthentication(authentication);
        User.AvailabilityStatus status = User.AvailabilityStatus.valueOf(body.get("availabilityStatus"));
        profileService.updateUserAvailability(userId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody UpdatePasswordDto passwordDto) {
        try {
            String username = authentication.getName();
            profileService.updatePassword(username, passwordDto.getOldPassword(), passwordDto.getNewPassword());
            return ResponseEntity.ok().body("Password updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/medicine-box")
    public ResponseEntity<?> updateUserHasMedicineBox(Authentication authentication, @RequestBody Map<String, Boolean> body) {
        Long userId = getUserIdFromAuthentication(authentication);
        Boolean hasMedicineBox = body.get("hasMedicineBox");
        profileService.updateUserHasMedicineBox(userId, hasMedicineBox);
        return ResponseEntity.ok().build();
    }

    // --- ✨ ADDED NEW ENDPOINTS ---
    @PutMapping("/vehicle")
    public ResponseEntity<?> updateUserVehicle(Authentication authentication, @RequestBody UpdateVehicleDto dto) {
        Long userId = getUserIdFromAuthentication(authentication);
        profileService.updateUserVehicle(userId, dto.getHasVehicle(), dto.getVehicleType());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/shelter")
    public ResponseEntity<?> updateUserShelter(Authentication authentication, @RequestBody UpdateShelterDto dto) {
        Long userId = getUserIdFromAuthentication(authentication);
        profileService.updateUserShelter(userId, dto.getCanProvideShelter());
        return ResponseEntity.ok().build();
    }
    // --- End Added New Endpoints ---
}