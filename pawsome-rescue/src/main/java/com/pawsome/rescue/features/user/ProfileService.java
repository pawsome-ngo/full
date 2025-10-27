package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.UserProfileDto;
import com.pawsome.rescue.auth.entity.Credentials;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.features.user.entity.UserStats;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.auth.repository.UserRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Getter
    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public void updateUserLocation(Long userId, Double latitude, Double longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Credentials credentials = credentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credentials not found for user"));
        UserStats stats = user.getUserStats();

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(credentials.getUsername());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setExperienceLevel(user.getExperienceLevel());
        dto.setAvailabilityStatus(user.getAvailabilityStatus());
        dto.setHasMedicineBox(user.getHasMedicineBox());
        dto.setCasesCompleted(stats.getCasesCompleted());
        dto.setHearts(stats.getHearts());
        dto.setDistanceTraveled(Math.round(stats.getDistanceTraveled() * 10.0) / 10.0);

        // --- ✨ ADDED MAPPINGS ---
        dto.setHasVehicle(user.getHasVehicle());
        dto.setVehicleType(user.getVehicleType());
        dto.setCanProvideShelter(user.getCanProvideShelter());
        // --- End Added Mappings ---

        return dto;
    }

    @Transactional
    public void updateUserAvailability(Long userId, User.AvailabilityStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setAvailabilityStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(String username, String oldPassword, String newPassword) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, oldPassword));

        Credentials credentials = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        credentials.setPasswordHash(passwordEncoder.encode(newPassword));
        credentialsRepository.save(credentials);
    }

    @Transactional
    public void updateUserHasMedicineBox(Long userId, boolean hasMedicineBox) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setHasMedicineBox(hasMedicineBox);
        userRepository.save(user);
    }

    // --- ✨ ADDED NEW METHODS ---
    @Transactional
    public void updateUserVehicle(Long userId, Boolean hasVehicle, String vehicleType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setHasVehicle(hasVehicle);
        // Only set vehicle type if they have a vehicle
        user.setVehicleType(hasVehicle ? vehicleType : null);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserShelter(Long userId, Boolean canProvideShelter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setCanProvideShelter(canProvideShelter);
        userRepository.save(user);
    }
}