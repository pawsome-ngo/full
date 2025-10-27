package com.pawsome.rescue.auth;

import com.pawsome.rescue.auth.dto.PendingUserDto;
import com.pawsome.rescue.auth.entity.*;
import com.pawsome.rescue.auth.repository.*;
import com.pawsome.rescue.exception.UnauthorizedActionException;

import com.pawsome.rescue.features.admin.dto.SuperAdminUserDto;
import com.pawsome.rescue.features.archive.IncidentArchiveRepository;
import com.pawsome.rescue.features.casemanagement.entity.TeamMember;
import com.pawsome.rescue.features.casemanagement.repository.*;
import com.pawsome.rescue.features.chat.ChatService;
import com.pawsome.rescue.features.chat.repository.ChatGroupRepository;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatMessageRepository;
import com.pawsome.rescue.features.globalchat.service.GlobalChatService;
import com.pawsome.rescue.features.incident.repository.*;
import com.pawsome.rescue.features.inventory.repository.*;
import com.pawsome.rescue.features.notification.NotificationRepository;
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import com.pawsome.rescue.features.notification.PushSubscriptionRepository;
import com.pawsome.rescue.features.storage.LocalStorageService;
import com.pawsome.rescue.features.user.entity.UserStats;
import com.pawsome.rescue.features.user.repository.UserStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pawsome.rescue.auth.entity.PendingUser; // <-- Add import
import com.pawsome.rescue.auth.repository.PendingUserRepository; // <

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    // --- All Repositories ---
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private UserStatsRepository userStatsRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentMediaRepository incidentMediaRepository;
    @Autowired private InterestedVolunteerRepository interestedVolunteerRepository;
    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private ChatService chatService;
    @Autowired private ChatGroupRepository chatGroupRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
    @Autowired private LocalStorageService localStorageService;
    @Autowired private IncidentArchiveRepository incidentArchiveRepository;
    @Autowired private ItemCategoryRepository itemCategoryRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private FirstAidKitRepository firstAidKitRepository;
    @Autowired private FirstAidKitItemRepository firstAidKitItemRepository;
    @Autowired private RequisitionRepository requisitionRepository;
    @Autowired private RequisitionItemRepository requisitionItemRepository;
    @Autowired private InventoryLogRepository inventoryLogRepository;
    @Autowired private PendingUserRepository pendingUserRepository;
    @Autowired private GlobalChatService globalChatService;

    @Autowired private GlobalChatMessageRepository globalChatMessageRepository;



    @Transactional(readOnly = true)
    public List<PendingUserDto> findPendingUsers() {
        // Query the new table
        return pendingUserRepository.findAll().stream()
                .map(this::convertToPendingUserDto) // Use the updated converter below
                .collect(Collectors.toList());
    }

    @Transactional
    public void authorizeUser(Long pendingUserId) {
        // 1. Find the PendingUser
        PendingUser pendingUser = pendingUserRepository.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pending user not found with ID: " + pendingUserId));

        // 2. Create the main User entity
        User user = new User();
        user.setFirstName(pendingUser.getFirstName());
        user.setLastName(pendingUser.getLastName());
        user.setPhoneNumber(pendingUser.getPhoneNumber());
        user.setAddress(pendingUser.getAddress());
        user.setMotivation(pendingUser.getMotivation());
        user.setHasVehicle(pendingUser.getHasVehicle());
        user.setVehicleType(pendingUser.getVehicleType());
        user.setCanProvideShelter(pendingUser.getCanProvideShelter());
        user.setHasMedicineBox(pendingUser.getHasMedicineBox());
        user.setLatitude(pendingUser.getLatitude());
        user.setLongitude(pendingUser.getLongitude());
        user.setExperienceLevel(pendingUser.getExperienceLevel());
        user.setJoinedSince(pendingUser.getSignedUpAt());

        // 3. Create and link initial stats
        UserStats initialStats = new UserStats();
        initialStats.setUser(user);
        user.setUserStats(initialStats);

        // 4. Save the main User (cascades to UserStats)
        userRepository.save(user); // User ID is generated here

        // 5. Create and save Credentials
        Credentials credentials = new Credentials();
        credentials.setUser(user);
        credentials.setUsername(pendingUser.getUsername());
        credentials.setPasswordHash(pendingUser.getPasswordHash());
        credentialsRepository.save(credentials);

        // 6. Assign default role
        Role defaultRole = roleRepository.findByName(Role.RoleName.MEMBER)
                .orElseThrow(() -> new IllegalStateException("Default role 'MEMBER' not found."));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(defaultRole);
        userRoleRepository.save(userRole);

        // 7. Delete the PendingUser record
        pendingUserRepository.delete(pendingUser);

        // --- 8. ADD USER TO GLOBAL CHAT ---
        try {
            globalChatService.addUserToGlobalChat(user);
        } catch (Exception e) {
            // Log error but don't fail the authorization
            logger.error("Failed to add new user {} to global chat: {}", user.getId(), e.getMessage(), e);
        }
        // --- END ADD ---

        // 9. Send Notifications (unchanged)
        String personalMessage = "Welcome aboard! Your Pawsome volunteer account has been approved.";
        notificationService.createNotification(
                user, NotificationType.APPROVAL, null, personalMessage, null, null
        );
        logger.info("Sent approval notification to newly authorized user {}", user.getId());

        try {
            String publicMessage = String.format("%s %s (@%s) has joined Pawsome! Welcome them aboard. 🐾",
                    user.getFirstName(), user.getLastName(), credentials.getUsername());
            List<User> allAuthorizedUsers = userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(user.getId()) && u.getCredentials() != null)
                    .toList();
            if (!allAuthorizedUsers.isEmpty()) {
                for (User recipient : allAuthorizedUsers) {
                    notificationService.createNotification(
                            recipient, NotificationType.GENERAL, null, publicMessage, user.getId(), user
                    );
                }
                logger.info("Sent 'new user joined' notification to {} other members.", allAuthorizedUsers.size());
            }
        } catch (Exception e) {
            logger.error("Failed to send 'new user joined' notifications for user ID: {}. Error: {}", user.getId(), e.getMessage(), e);
        }

        logger.info("Successfully authorized and migrated PendingUser ID {} to User ID {}", pendingUserId, user.getId());
    }
    @Transactional
    public void denyUser(Long pendingUserId) { // Parameter is now pendingUserId
        // Find the pending user record
        PendingUser pendingUser = pendingUserRepository.findById(pendingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Pending user not found with ID: " + pendingUserId));

        // Simply delete the record from the pending table
        pendingUserRepository.delete(pendingUser);

        logger.info("Denied and deleted PendingUser ID {}", pendingUserId);
        // Optional: Send a notification to admins that the user was denied?
    }
    // --- End Fix ---

    // Update the converter method to accept PendingUser
    private PendingUserDto convertToPendingUserDto(PendingUser pendingUser) {
        PendingUserDto dto = new PendingUserDto();
        dto.setUserId(pendingUser.getId()); // Use PendingUser ID
        dto.setFirstName(pendingUser.getFirstName());
        dto.setLastName(pendingUser.getLastName());
        dto.setPhoneNumber(pendingUser.getPhoneNumber());
        dto.setAddress(pendingUser.getAddress());
        dto.setMotivation(pendingUser.getMotivation());
        dto.setHasVehicle(pendingUser.getHasVehicle());
        dto.setVehicleType(pendingUser.getVehicleType());
        dto.setCanProvideShelter(pendingUser.getCanProvideShelter());
        dto.setHasMedicineBox(pendingUser.getHasMedicineBox());
        dto.setExperienceLevel(pendingUser.getExperienceLevel());
        return dto;
    }

    @Transactional
    public void resetApplicationDataKeepUsers() {
        logger.warn("Executing application reset while keeping users, roles, and archive...");

        // 1. Delete Files (Keep as is)
        try {
            localStorageService.deleteAllFiles();
        } catch (Exception e) {
            logger.error("Error deleting files during reset: {}", e.getMessage(), e);
            // Optionally re-throw or handle more gracefully if file deletion failure is critical
        }

        // 2. Delete Chat Data (Keep as is)
        chatGroupRepository.findAll().forEach(cg -> chatService.deleteChatGroupAndData(cg.getId()));
        logger.info("Deleted all chat data.");

        // 3. Delete Incident related data (excluding Incidents themselves yet)
        incidentMediaRepository.deleteAllInBatch();
        // Reset Auto Increment for incident_media if it has one (assuming it does)
        // incidentMediaRepository.resetAutoIncrement(); // Add if incident_media has auto-increment ID
        interestedVolunteerRepository.deleteAllInBatch();
        logger.info("Deleted incident media and interest records.");

        // 4. Delete Team Members (Association table)
        teamMemberRepository.deleteAllInBatch();
        teamMemberRepository.resetAutoIncrement(); // Reset team_members
        logger.info("Deleted team member associations.");

        // 5. Delete Inventory Data (Delete Items first, then Categories)
        firstAidKitItemRepository.deleteAllInBatch();
        firstAidKitItemRepository.resetAutoIncrement(); // Reset first_aid_kit_items
        firstAidKitRepository.deleteAllInBatch();
        firstAidKitRepository.resetAutoIncrement(); // Reset first_aid_kits
        requisitionItemRepository.deleteAllInBatch();
        requisitionItemRepository.resetAutoIncrement(); // Reset requisition_items
        requisitionRepository.deleteAllInBatch();
        requisitionRepository.resetAutoIncrement(); // Reset requisitions
        inventoryLogRepository.deleteAllInBatch();
        inventoryLogRepository.resetAutoIncrement(); // Reset inventory_logs
        inventoryItemRepository.deleteAllInBatch();
        inventoryItemRepository.resetAutoIncrement(); // Reset inventory_items
        itemCategoryRepository.deleteAllInBatch();
        itemCategoryRepository.resetAutoIncrement(); // Reset item_categories
        logger.info("Deleted all inventory data.");

        // 6. Delete Notifications and Push Subscriptions
        notificationRepository.deleteAllInBatch();
        notificationRepository.resetAutoIncrement(); // Reset notifications
        pushSubscriptionRepository.deleteAllInBatch();
        pushSubscriptionRepository.resetAutoIncrement(); // Reset push_subscriptions
        logger.info("Deleted all notifications and push subscriptions.");

        // 7. Delete Core Entities (Cases, Teams, Incidents)
        // Cases depend on Incidents and Teams, delete them first
        rescueCaseRepository.deleteAllInBatch();
        rescueCaseRepository.resetAutoIncrement(); // Reset cases
        // Teams can be deleted after Cases and TeamMembers
        teamRepository.deleteAllInBatch();
        teamRepository.resetAutoIncrement(); // Reset teams
        // Incidents can be deleted last among these
        incidentRepository.deleteAllInBatch();
        incidentRepository.resetAutoIncrement(); // Reset incidents
        logger.info("Deleted all incidents, cases, and teams.");

        // 8. Delete Incident Archive (Optional - uncomment if desired)
        // incidentArchiveRepository.deleteAllInBatch();
        // incidentArchiveRepository.resetAutoIncrement();
        // logger.info("Deleted incident archive.");

        // 9. Reset User Stats (Keep as is)
        List<UserStats> allStats = userStatsRepository.findAll();
        for (UserStats stats : allStats) {
            stats.setPoints(0);
            stats.setHearts(0);
            stats.setDistanceTraveled(0.0);
            stats.setCasesCompleted(0);
        }
        userStatsRepository.saveAllAndFlush(allStats);
        logger.info("Reset stats for {} users.", allStats.size());

        // Note: We are NOT deleting users, credentials, roles, user_roles as per method name.

        logger.warn("Application reset (keeping users) and auto-increment reset complete.");
    }

    @Transactional
    public void updateUserPosition(Long userId, User.Position position) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.setPosition(position);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserExperienceLevel(Long userId, User.ExperienceLevel experienceLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.setExperienceLevel(experienceLevel);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRoles(Long userId, List<String> roleNames, Authentication currentUserAuth) {
        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Collection<? extends GrantedAuthority> currentUserAuthorities = currentUserAuth.getAuthorities();
        boolean isSuperAdmin = currentUserAuthorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"));
        List<String> userToUpdateRoles = userToUpdate.getUserRoles().stream()
                .map(ur -> ur.getRole().getName().name())
                .collect(Collectors.toList());
        boolean isTargetSuperAdmin = userToUpdateRoles.contains("SUPER_ADMIN");
        boolean isTargetAdmin = userToUpdateRoles.contains("ADMIN");
        boolean isCurrentUserAdmin = currentUserAuthorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        List<Role.RoleName> finalRoles;
        if (isSuperAdmin) {
            if (isTargetSuperAdmin && !roleNames.contains("SUPER_ADMIN")) {
                throw new UnauthorizedActionException("Super Admin role cannot be removed via this endpoint.");
            }
            finalRoles = roleNames.stream().map(Role.RoleName::valueOf).collect(Collectors.toList());
        } else if (isCurrentUserAdmin) {
            if (isTargetAdmin || isTargetSuperAdmin) {
                throw new UnauthorizedActionException("You are not authorized to modify the roles of an Admin or a Super Admin.");
            }
            List<Role.RoleName> allowedRoles = List.of(Role.RoleName.MEMBER, Role.RoleName.RESCUE_CAPTAIN);
            finalRoles = roleNames.stream()
                    .map(Role.RoleName::valueOf)
                    .filter(allowedRoles::contains)
                    .collect(Collectors.toList());
        } else {
            throw new UnauthorizedActionException("You are not authorized to modify user roles.");
        }
        userRoleRepository.deleteAll(userToUpdate.getUserRoles());
        userToUpdate.getUserRoles().clear();
        List<Role> newRoles = roleRepository.findAllByNameIn(finalRoles);
        for (Role role : newRoles) {
            UserRole userRole = new UserRole();
            userRole.setUser(userToUpdate);
            userRole.setRole(role);
            userToUpdate.getUserRoles().add(userRole);
        }
        userRepository.save(userToUpdate);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminUserDto> getUsersForSuperAdminView(Long currentSuperAdminId) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentSuperAdminId))
                .map(this::convertToSuperAdminUserDto)
                .sorted(Comparator.comparing(SuperAdminUserDto::getFirstName))
                .collect(Collectors.toList());
    }

    private SuperAdminUserDto convertToSuperAdminUserDto(User user) {
        SuperAdminUserDto dto = new SuperAdminUserDto();
        dto.setUserId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPosition(user.getPosition());
        dto.setJoinedSince(user.getJoinedSince());
        Credentials creds = user.getCredentials();
        dto.setUsername(creds != null ? creds.getUsername() : "[N/A]");
//        dto.setIsAuthorized(creds != null ? creds.getIsAuthorized() : false);
        dto.setRoles(user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName().name())
                .collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public void deleteSingleUserAsSuperAdmin(Long userIdToDelete, Long currentSuperAdminId, boolean notifyUsers) {
        User userToDelete = userRepository.findById(userIdToDelete)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userIdToDelete));
        if (userIdToDelete.equals(currentSuperAdminId)) {
            throw new IllegalArgumentException("Super Admins cannot delete their own account.");
        }
        boolean isTargetSuperAdmin = userToDelete.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName() == Role.RoleName.SUPER_ADMIN);
        if (isTargetSuperAdmin) {
            throw new IllegalArgumentException("Cannot delete another Super Admin via this function.");
        }
        boolean isInActiveCase = rescueCaseRepository.isUserInAnyActiveCase(userIdToDelete);
        if (isInActiveCase) {
            throw new IllegalStateException("Cannot delete user: They are currently assigned to an active rescue case. Please close or reassign the case first.");
        }
        String deletedUserName = userToDelete.getFirstName() + " " + userToDelete.getLastName();
        try {
            deleteSingleUserData(userToDelete);
            logger.info("Successfully deleted user ID {} ({}) by Super Admin ID {}", userIdToDelete, deletedUserName, currentSuperAdminId);
        } catch (Exception e) {
            logger.error("Error during deletion of user ID {}: {}", userIdToDelete, e.getMessage(), e);
            throw new RuntimeException("An error occurred during user data cleanup.", e);
        }
        if (notifyUsers) {
            List<User> allOtherUsers = userRepository.findAll().stream()
                    .filter(user -> !user.getId().equals(userIdToDelete))
                    .toList();
            String message = String.format("User '%s' has been removed from the system.", deletedUserName);
            User adminUser = userRepository.findById(currentSuperAdminId).orElse(null);
            for (User recipient : allOtherUsers) {
                notificationService.createNotification(recipient, NotificationType.GENERAL, null, message, null, adminUser);
            }
            logger.info("Sent removal notification for user {} to {} users.", deletedUserName, allOtherUsers.size());
        }
    }

    @Transactional
    public Map<String, List<Map<String, Object>>> deleteUsersBatch(List<Long> userIdsToDelete, Long currentSuperAdminId, boolean notifyUsers) {
        List<Map<String, Object>> deletedUsersInfo = new ArrayList<>();
        List<Map<String, Object>> skippedUsersInfo = new ArrayList<>();
        User adminUser = userRepository.findById(currentSuperAdminId).orElse(null);
        for (Long userId : userIdsToDelete) {
            if (userId.equals(currentSuperAdminId)) {
                skippedUsersInfo.add(Map.of("userId", userId, "reason", "Cannot delete self"));
                continue;
            }
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                skippedUsersInfo.add(Map.of("userId", userId, "reason", "User not found"));
                continue;
            }
            User userToDelete = userOpt.get();
            boolean isTargetSuperAdmin = userToDelete.getUserRoles().stream()
                    .anyMatch(ur -> ur.getRole().getName() == Role.RoleName.SUPER_ADMIN);
            if (isTargetSuperAdmin) {
                skippedUsersInfo.add(Map.of("userId", userId, "username", userToDelete.getCredentials().getUsername(), "reason", "Cannot delete Super Admin"));
                continue;
            }
            boolean isInActiveCase = rescueCaseRepository.isUserInAnyActiveCase(userId);
            if (isInActiveCase) {
                skippedUsersInfo.add(Map.of("userId", userId, "username", userToDelete.getCredentials().getUsername(), "reason", "User is in an active case"));
                continue;
            }
            try {
                String deletedUserName = userToDelete.getFirstName() + " " + userToDelete.getLastName();
                deleteSingleUserData(userToDelete);
                deletedUsersInfo.add(Map.of("userId", userId, "username", deletedUserName));
                logger.info("Successfully deleted user ID {} ({}) by Super Admin ID {}", userId, deletedUserName, currentSuperAdminId);
            } catch (Exception e) {
                logger.error("Error during deletion of user ID {}: {}", userId, e.getMessage(), e);
                skippedUsersInfo.add(Map.of("userId", userId, "username", userToDelete.getCredentials().getUsername(), "reason", "Deletion error: " + e.getMessage()));
            }
        }
        if (notifyUsers && !deletedUsersInfo.isEmpty()) {
            List<User> allRemainingUsers = userRepository.findAll();
            String deletedNames = deletedUsersInfo.stream()
                    .map(info -> (String) info.get("username"))
                    .collect(Collectors.joining(", "));
            String message = String.format("The following user(s) have been removed from the system: %s.", deletedNames);
            for (User recipient : allRemainingUsers) {
                boolean wasDeleted = deletedUsersInfo.stream().anyMatch(info -> info.get("userId").equals(recipient.getId()));
                if (!wasDeleted) {
                    notificationService.createNotification(recipient, NotificationType.GENERAL, null, message, null, adminUser);
                }
            }
            logger.info("Sent batch removal notification triggered by Super Admin ID {}", currentSuperAdminId);
        }
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("deleted", deletedUsersInfo);
        result.put("skipped", skippedUsersInfo);
        return result;
    }

    private void deleteSingleUserData(User userToDelete) {
        // This helper is the key. It deletes associated data first.

        // 1. Delete Team Memberships
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userToDelete.getId());
        if (!memberships.isEmpty()) {
            teamMemberRepository.deleteAll(memberships);
        }

        // 2. Delete First Aid Kit
        firstAidKitRepository.findByUserId(userToDelete.getId()).ifPresent(firstAidKitRepository::delete);

        // 3. Delete Notifications related to the user
        notificationRepository.deleteByRecipientUser(userToDelete);
        notificationRepository.deleteByTriggeringUser(userToDelete);

        // 4. Delete Push Subscriptions
        pushSubscriptionRepository.deleteByUser(userToDelete);

        // 5. Delete User (Cascade should handle Credentials, UserRoles, UserStats)
        userRepository.delete(userToDelete);
    }

    // --- ADD THIS NEW METHOD ---
    @Transactional
    public void resetUserPassword(Long userIdToReset, Long currentSuperAdminId) {
        if (userIdToReset.equals(currentSuperAdminId)) {
            throw new IllegalArgumentException("Super Admins cannot reset their own password.");
        }

        User userToReset = userRepository.findById(userIdToReset)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userIdToReset));

        boolean isTargetSuperAdmin = userToReset.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName() == Role.RoleName.SUPER_ADMIN);

        if (isTargetSuperAdmin) {
            throw new UnauthorizedActionException("Cannot reset the password of another Super Admin.");
        }

        Credentials credentials = credentialsRepository.findByUserId(userIdToReset)
                .orElseThrow(() -> new IllegalStateException("Credentials not found for user"));
        credentials.setPasswordHash("$2a$10$S3lG.7NIh87Sn79rODgPVusaPS2bZTi0XjBxJzr1DfR702JVPMHCm");

        credentialsRepository.save(credentials);

        logger.warn("User ID {}'s password has been reset to the default by Super Admin ID {}", userIdToReset, currentSuperAdminId);

        // Optionally, send a notification to the user
        String message = "Your password has been reset by an administrator. Please log in using the default password 'pawsome' and change it from your profile immediately.";
        notificationService.createNotification(
                userToReset,
                NotificationType.GENERAL, // Or a new NotificationType.SECURITY
                null,
                message,
                null,
                null // Or pass the admin user
        );
    }

    @Transactional
    public int clearGlobalChatMessages(int messagesToKeep) {
        if (messagesToKeep < 0) {
            throw new IllegalArgumentException("Number of messages to keep must be non-negative.");
        }
        if (messagesToKeep == 0) {
            logger.info("Clearing ALL global chat messages.");
            List<GlobalChatMessage> allMessages = globalChatMessageRepository.findAll();
            AtomicInteger deletedCount = new AtomicInteger(0);
            allMessages.forEach(msg -> {
                deleteMessageAndMedia(msg);
                deletedCount.incrementAndGet();
            });
            logger.info("Deleted {} global chat messages and associated media.", deletedCount.get());
            return deletedCount.get();
        }

        // Find the timestamp of the Nth message to keep (offset is messagesToKeep - 1)
        Optional<LocalDateTime> thresholdTimestampOpt = globalChatMessageRepository.findNthMostRecentTimestamp(messagesToKeep - 1);

        if (thresholdTimestampOpt.isEmpty()) {
            logger.info("Not enough messages ({}) found to clear, keeping all.", messagesToKeep);
            return 0; // Fewer messages exist than messagesToKeep, nothing to delete
        }

        LocalDateTime thresholdTimestamp = thresholdTimestampOpt.get();
        logger.info("Clearing global chat messages older than {}", thresholdTimestamp);

        // Find all messages older than the threshold
        List<GlobalChatMessage> messagesToDelete = globalChatMessageRepository.findByTimestampBeforeOrderByTimestampAsc(thresholdTimestamp);

        if (messagesToDelete.isEmpty()) {
            logger.info("No global chat messages found older than the threshold.");
            return 0;
        }

        AtomicInteger deletedCount = new AtomicInteger(0);
        messagesToDelete.forEach(msg -> {
            deleteMessageAndMedia(msg);
            deletedCount.incrementAndGet();
        });

        logger.info("Deleted {} global chat messages and associated media older than {}", deletedCount.get(), thresholdTimestamp);
        return deletedCount.get();
    }

    // Helper method to delete a message and its associated media file
    private void deleteMessageAndMedia(GlobalChatMessage message) {
        try {
            // Delete media file if it exists
            if (message.getMediaUrl() != null && !message.getMediaUrl().isBlank()) {
                try {
                    // Extract filename from the URL path
                    String relativePath = message.getMediaUrl();
                    // Assuming URL is like /uploads/filename.ext
                    if (relativePath.startsWith("/uploads/")) {
                        String filename = relativePath.substring("/uploads/".length());
                        localStorageService.deleteFile(filename);
                    } else {
                        logger.warn("Media URL format not recognized for deletion: {}", relativePath);
                    }
                } catch (Exception e) {
                    logger.error("Error deleting media file for message ID {}: {}", message.getId(), e.getMessage());
                    // Decide if you want to continue deleting the message even if file deletion fails
                }
            }
            // Delete the message from the repository
            globalChatMessageRepository.delete(message);
        } catch (Exception e) {
            logger.error("Error deleting global chat message ID {}: {}", message.getId(), e.getMessage());
            // Consider re-throwing or handling more robustly depending on requirements
        }
    }
}