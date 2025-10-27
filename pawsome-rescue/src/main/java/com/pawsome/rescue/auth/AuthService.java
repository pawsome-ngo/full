package com.pawsome.rescue.auth;

import com.pawsome.rescue.auth.dto.SignUpDto;
import com.pawsome.rescue.auth.entity.Credentials;
import com.pawsome.rescue.auth.entity.Role;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.entity.UserRole;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.auth.repository.RoleRepository;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.auth.repository.UserRoleRepository;
import com.pawsome.rescue.features.user.entity.UserStats;
// --- ✨ Import Notification classes and Logger ---
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// --- End Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pawsome.rescue.auth.entity.PendingUser; // <-- Add import
import com.pawsome.rescue.auth.repository.PendingUserRepository;

import java.util.List; // <-- Import List
import java.util.Optional;
import java.util.stream.Collectors; // <-- Import Collectors

@Service
public class AuthService {

    // --- ✨ Add Logger ---
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    // --- ✨ Inject NotificationService ---
    @Autowired
    private NotificationService notificationService;

    @Autowired private PendingUserRepository pendingUserRepository;

    @Transactional
    public PendingUser signup(SignUpDto signUpDto) { // Return PendingUser instead of User
        // Check existing authorized users
        if (credentialsRepository.findByUsername(signUpDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.findByPhoneNumber(signUpDto.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered.");
        }
        // Check existing pending users
        if (pendingUserRepository.findByUsername(signUpDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is pending approval.");
        }
        if (pendingUserRepository.findByPhoneNumber(signUpDto.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Phone number is pending approval.");
        }

        // Create PendingUser entity
        PendingUser pendingUser = new PendingUser();
        pendingUser.setUsername(signUpDto.getUsername());
        pendingUser.setPasswordHash(passwordEncoder.encode(signUpDto.getPassword())); // Hash password here
        pendingUser.setFirstName(signUpDto.getFirstName());
        pendingUser.setLastName(signUpDto.getLastName());
        pendingUser.setPhoneNumber(signUpDto.getPhoneNumber());
        pendingUser.setAddress(signUpDto.getAddress());
        pendingUser.setMotivation(signUpDto.getMotivation());
        pendingUser.setHasVehicle(signUpDto.getHasVehicle());
        pendingUser.setVehicleType(signUpDto.getHasVehicle() ? signUpDto.getVehicleType() : null);
        pendingUser.setCanProvideShelter(signUpDto.getCanProvideShelter());
        pendingUser.setHasMedicineBox(signUpDto.getHasMedicineBox());
        pendingUser.setLatitude(signUpDto.getLatitude());
        pendingUser.setLongitude(signUpDto.getLongitude());
        // ExperienceLevel is already defaulted in the entity

        // Save the PendingUser
        PendingUser savedPendingUser = pendingUserRepository.save(pendingUser);

        // Send Notification to Admins
        try {
            List<Role.RoleName> adminRoles = List.of(Role.RoleName.ADMIN, Role.RoleName.SUPER_ADMIN);
            List<User> adminRecipients = userRoleRepository.findAllByRole_NameIn(adminRoles)
                    .stream()
                    .map(UserRole::getUser)
                    .distinct()
                    .collect(Collectors.toList());

            if (!adminRecipients.isEmpty()) {
                String message = String.format("New user '%s %s' (@%s) signed up and needs approval.",
                        savedPendingUser.getFirstName(), savedPendingUser.getLastName(), savedPendingUser.getUsername());

                for (User admin : adminRecipients) {
                    // Create a dummy user object for notification trigger if needed, or adjust notification service
                    // For now, let's pass null for triggering user, as the actual User entity doesn't exist yet.
                    notificationService.createNotification(
                            admin,
                            NotificationType.APPROVAL,
                            null,
                            message,
                            savedPendingUser.getId(), // Use PendingUser ID as related ID
                            null // No actual User entity exists yet to be the trigger
                    );
                }
                logger.info("Sent signup notification for PendingUser ID {} to {} admins.", savedPendingUser.getId(), adminRecipients.size());
            } else {
                logger.warn("New user signed up (PendingUser ID: {}), but no ADMIN or SUPER_ADMIN users were found to notify.", savedPendingUser.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to send signup notifications for PendingUser ID: {}. Error: {}", savedPendingUser.getId(), e.getMessage(), e);
        }

        return savedPendingUser; // Return the saved PendingUser
    }

    public void authenticate(String username, String password) {
        // Authentication now inherently checks if the user exists in the main 'credentials' table.
        // If they don't exist there, they are either pending or don't exist at all.
        // No need to check isAuthorized anymore.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        // Add a log to confirm successful authentication based on existence in main tables
        logger.info("User '{}' successfully authenticated (found in main credentials table).", username);
    }
}