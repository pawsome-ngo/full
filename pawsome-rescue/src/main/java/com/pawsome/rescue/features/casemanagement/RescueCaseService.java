package com.pawsome.rescue.features.casemanagement;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.casemanagement.dto.CaseCompletionDetailsDto;
import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import com.pawsome.rescue.features.casemanagement.entity.Team;
import com.pawsome.rescue.features.casemanagement.entity.TeamMember;
import com.pawsome.rescue.features.casemanagement.repository.RescueCaseRepository;
import com.pawsome.rescue.features.casemanagement.repository.TeamRepository;
import com.pawsome.rescue.features.chat.ChatService;
import com.pawsome.rescue.features.chat.entity.ChatGroup;
import com.pawsome.rescue.features.incident.IncidentService;
import com.pawsome.rescue.features.incident.dto.IncidentDto;
import com.pawsome.rescue.features.incident.entity.Incident;
import com.pawsome.rescue.features.incident.entity.IncidentMedia;
import com.pawsome.rescue.features.incident.repository.IncidentMediaRepository;
import com.pawsome.rescue.features.incident.repository.IncidentRepository;
import com.pawsome.rescue.features.incident.repository.InterestedVolunteerRepository;
import com.pawsome.rescue.features.storage.LocalStorageService;
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import com.pawsome.rescue.features.user.entity.UserStats;
import com.pawsome.rescue.features.user.repository.UserStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RescueCaseService {

    private static final Logger logger = LoggerFactory.getLogger(RescueCaseService.class);

    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private IncidentService incidentService;
    @Autowired private UserStatsRepository userStatsRepository;
    @Autowired private InterestedVolunteerRepository interestedVolunteerRepository;
    // --- ✨ Use LocalStorageService ---
    @Autowired private LocalStorageService localStorageService;
    // --- Removed GoogleDriveService ---
    @Autowired private IncidentMediaRepository incidentMediaRepository;
    @Autowired private ChatService chatService;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private NotificationService notificationService; // <-- Keep
    @Autowired private TeamAssignmentService teamAssignmentService;
    @Autowired private UserRepository userRepository;


    public List<IncidentDto> getMyCases(Long userId) {
        List<RescueCase> myCases = rescueCaseRepository.findActiveCasesByUserId(userId);
        return myCases.stream()
                .map(RescueCase::getIncident)
                .map(incidentService::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmInitiation(Long incidentId, List<Long> participatingUserIds, Long initiatorUserId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + incidentId));
        if (incident.getStatus() != Incident.IncidentStatus.ASSIGNED) {
            throw new IllegalStateException("Only an ASSIGNED case can be initiated.");
        }
        RescueCase activeCase = rescueCaseRepository.findByIncidentIdAndIsActiveTrue(incidentId)
                .orElseThrow(() -> new IllegalStateException("No active case found for this assigned incident."));
        Team originalTeam = activeCase.getAssignedTeam();
        if (originalTeam == null) {
            throw new IllegalStateException("No team assigned to the active case.");
        }
        Set<Long> originalMemberIds = originalTeam.getTeamMembers().stream()
                .map(tm -> tm.getUser().getId())
                .collect(Collectors.toSet());
        if (!originalMemberIds.contains(initiatorUserId)) {
            throw new AccessDeniedException("Only members of the assigned team can initiate the case.");
        }
        Set<Long> participatingMemberIds = new HashSet<>(participatingUserIds);
        if (participatingMemberIds.isEmpty()) {
            throw new IllegalArgumentException("At least one participating member must be selected.");
        }
        if (!originalMemberIds.containsAll(participatingMemberIds)) {
            throw new IllegalArgumentException("Selected participants include users not originally assigned to this case.");
        }
        User initiatorUser = userRepository.findById(initiatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Initiator user not found."));
        List<User> participatingUsers = userRepository.findAllById(participatingUserIds);
        boolean statusUpdated = false;
        if (originalMemberIds.equals(participatingMemberIds)) {
            incident.setStatus(Incident.IncidentStatus.IN_PROGRESS);
            incident.setLastUpdated(LocalDateTime.now());
            incidentRepository.save(incident);
            statusUpdated = true;
        } else {
            activeCase.setActive(false);
            activeCase.setClosedAt(LocalDateTime.now());
            rescueCaseRepository.save(activeCase);
            if (activeCase.getChatGroup() != null) {
                chatService.deleteChatGroupAndData(activeCase.getChatGroup().getId());
                activeCase.setChatGroup(null);
                rescueCaseRepository.save(activeCase);
            }
            Team newTeam = teamAssignmentService.findOrCreateTeam(participatingUsers);
            String newChatGroupName = newTeam.getName() + " - Incident #" + incident.getId() + " (Active)";
            ChatGroup newChatGroup = chatService.createChatGroup(participatingUserIds, newChatGroupName, "INCIDENT", incident.getId());
            RescueCase newRescueCase = new RescueCase();
            newRescueCase.setIncident(incident);
            newRescueCase.setAssignedTeam(newTeam);
            newRescueCase.setChatGroup(newChatGroup);
            newRescueCase.setAssignedBy(initiatorUser);
            newRescueCase.setActive(true);
            rescueCaseRepository.save(newRescueCase);
            incident.setStatus(Incident.IncidentStatus.IN_PROGRESS);
            incident.setLastUpdated(LocalDateTime.now());
            incidentRepository.save(incident);
            statusUpdated = true;
        }
        if (statusUpdated) {
            try {
                String initiationMessage = String.format("Case for Incident #%d has been initiated by %s.",
                        incident.getId(),
                        initiatorUser.getFirstName());
                for (User member : participatingUsers) {
                    notificationService.createNotification(
                            member,
                            NotificationType.INCIDENT,
                            Incident.IncidentStatus.IN_PROGRESS,
                            initiationMessage,
                            incident.getId(),
                            initiatorUser
                    );
                }
                logger.info("Sent initiation notification for Incident ID: {} to {} members.", incident.getId(), participatingUsers.size());
            } catch (Exception e) {
                logger.error("Failed to send initiation notifications for Incident ID: {}. Error: {}", incident.getId(), e.getMessage(), e);
            }
        }
    }


    @Transactional
    public void closeCase(Long incidentId, CaseCompletionDetailsDto details, List<MultipartFile> mediaFiles) {
        RescueCase rescueCase = rescueCaseRepository.findByIncidentIdAndIsActiveTrue(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Active case not found for incident ID: " + incidentId));

        Incident incident = rescueCase.getIncident();
        Map<Long, Map<String, Integer>> rewards = updateVolunteerStats(rescueCase, details);

        rescueCase.setActive(false);
        rescueCase.setResolutionNotes(details.getResolutionNotes());
        rescueCase.setClosedAt(LocalDateTime.now());
        incident.setStatus(Incident.IncidentStatus.ONGOING);
        incident.setCaseCount(incident.getCaseCount() + 1);
        incident.setLastUpdated(LocalDateTime.now());

        Team assignedTeam = rescueCase.getAssignedTeam();
        if (assignedTeam != null) {
            assignedTeam.setCaseCount(assignedTeam.getCaseCount() + 1);
            teamRepository.save(assignedTeam);
        }

        if (details.getFinalLatitude() != null && details.getFinalLongitude() != null) {
            incident.setLatitude(details.getFinalLatitude());
            incident.setLongitude(details.getFinalLongitude());
        }

        // --- ✨ Use LocalStorageService ---
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                try {
                    String fileName = localStorageService.storeFile(file); // <-- Use local storage
                    IncidentMedia media = new IncidentMedia();
                    media.setIncident(incident);
                    media.setRescueCase(rescueCase);
                    media.setFilePath(fileName);
                    media.setMediaType(incidentService.getMediaType(file.getContentType()));
                    incidentMediaRepository.save(media);
                } catch (Exception e) { // Catch general storage exception
                    logger.error("Could not upload file: {}", file.getOriginalFilename(), e);
                }
            }
        }
        // --- End Modification ---

        interestedVolunteerRepository.deleteAllByIncidentId(incident.getId());

        if (rescueCase.getChatGroup() != null) {
            chatService.deleteChatGroupAndData(rescueCase.getChatGroup().getId());
            rescueCase.setChatGroup(null);
        }

        rescueCaseRepository.save(rescueCase);
        incidentRepository.save(incident);

        // --- Send Notifications ---
        try {
            if (assignedTeam != null) {
                for (TeamMember member : assignedTeam.getTeamMembers()) {
                    User user = member.getUser();
                    Map<String, Integer> userReward = rewards.getOrDefault(user.getId(), Collections.emptyMap());
                    int pointsAwarded = userReward.getOrDefault("points", 0);
                    int heartsAwarded = userReward.getOrDefault("hearts", 0);

                    String rewardMessage = String.format(
                            "Case for Incident #%d completed! 🎉 You earned %d points%s.",
                            incident.getId(),
                            pointsAwarded,
                            heartsAwarded > 0 ? " and " + heartsAwarded + " heart" + (heartsAwarded > 1 ? "s" : "") : ""
                    );

                    notificationService.createNotification(
                            user,
                            NotificationType.REWARDS,
                            incident.getStatus(),
                            rewardMessage,
                            incident.getId(),
                            null
                    );
                }
                logger.info("Sent case completion/reward notifications for Incident ID: {} to {} members.", incident.getId(), assignedTeam.getTeamMembers().size());
            }
        } catch(Exception e){
            logger.error("Failed to send case completion/reward notifications for Incident ID: {}. Error: {}", incident.getId(), e.getMessage(), e);
        }
    }

    private Map<Long, Map<String, Integer>> updateVolunteerStats(RescueCase rescueCase, CaseCompletionDetailsDto details) {
        // ... (method remains the same) ...
        Incident incident = rescueCase.getIncident();
        Set<Long> interestedUserIds = interestedVolunteerRepository.findByIncidentId(incident.getId())
                .stream()
                .map(iv -> iv.getUser().getId())
                .collect(Collectors.toSet());

        List<TeamMember> teamMembers = rescueCase.getAssignedTeam().getTeamMembers();
        Map<Long, Map<String, Integer>> awardedRewards = new HashMap<>();

        for (TeamMember member : teamMembers) {
            User user = member.getUser();
            UserStats stats = userStatsRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalStateException("UserStats not found for user ID: " + user.getId()));

            int pointsToAdd = 0;
            int heartsToAdd = 0;

            if (interestedUserIds.contains(user.getId())) {
                pointsToAdd = 5;
                heartsToAdd = 1;
            } else {
                pointsToAdd = 4;
            }

            stats.setPoints(stats.getPoints() + pointsToAdd);
            stats.setHearts(stats.getHearts() + heartsToAdd);

            Double incidentLat = details.getFinalLatitude() != null ? details.getFinalLatitude() : incident.getLatitude();
            Double incidentLon = details.getFinalLongitude() != null ? details.getFinalLongitude() : incident.getLongitude();
            Double distance = TeamAssignmentService.calculateDistance(user.getLatitude(), user.getLongitude(), incidentLat, incidentLon);
            if (distance != null) {
                stats.setDistanceTraveled(stats.getDistanceTraveled() + distance);
            }

            stats.setCasesCompleted(stats.getCasesCompleted() + 1);
            userStatsRepository.save(stats);

            Map<String, Integer> userReward = new HashMap<>();
            userReward.put("points", pointsToAdd);
            userReward.put("hearts", heartsToAdd);
            awardedRewards.put(user.getId(), userReward);
        }
        return awardedRewards;
    }
}