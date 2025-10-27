// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/casemanagement/TeamAssignmentService.java
package com.pawsome.rescue.features.casemanagement;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.casemanagement.dto.*;
import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import com.pawsome.rescue.features.casemanagement.entity.Team;
import com.pawsome.rescue.features.casemanagement.entity.TeamMember;
import com.pawsome.rescue.features.casemanagement.repository.RescueCaseRepository;
import com.pawsome.rescue.features.casemanagement.repository.TeamRepository;
import com.pawsome.rescue.features.chat.entity.ChatGroup;
import com.pawsome.rescue.features.incident.repository.IncidentRepository;
import com.pawsome.rescue.features.incident.repository.InterestedVolunteerRepository;
import com.pawsome.rescue.features.incident.entity.Incident;
import com.pawsome.rescue.features.chat.ChatService;
// --- ✨ Import Notification classes ---
import com.pawsome.rescue.features.inventory.entity.FirstAidKit;
import com.pawsome.rescue.features.inventory.repository.FirstAidKitRepository;
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import org.slf4j.Logger; // <-- Import Logger
import org.slf4j.LoggerFactory; // <-- Import LoggerFactory
// --- End Imports ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime; // <-- Import LocalDateTime
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class TeamAssignmentService {

    // --- ✨ Add Logger ---
    private static final Logger logger = LoggerFactory.getLogger(TeamAssignmentService.class);
    // --- End Logger ---

    @Autowired private UserRepository userRepository;
    @Autowired private InterestedVolunteerRepository interestedVolunteerRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private ChatService chatService;

    // --- ✨ Inject NotificationService ---
    @Autowired private NotificationService notificationService;

    @Autowired
    private FirstAidKitRepository firstAidKitRepository;

    // --- End Injection ---

    private static final String[] TEAM_NAMES = {
            "Bark Side", "Purrfect Storm", "Whisker Warriors", "Clowder Crowd", "Meowfia",
            "Scruffy Squad", "Pawsitive Vibes", "Litter Gitters", "Howlers", "Growlers",
            "Alley Cats", "Rescue Rangers", "Fast and the Furrious", "Avengers of Animals",
            "League of Extraordinary Paws", "Pet Detectives", "Dog Whisperers", "Cat Crusaders",
            "Fur-minators", "Critter Crew", "Tail Waggers", "Wet Noses", "Slobbery Kisses",
            "Un-fur-gettable Team", "Cat-astrophe Crew", "Dog-gone Good Team", "Paw-some Posse",
            "Fur-ever Friends", "Animal House", "Zoo Crew", "Wild Things", "Pet Pack",
            "Alpha Dogs", "Top Cats", "Head Hounds", "Main Meows", "Pack Leaders",
            "Pride", "Kennel Krew", "Cattery Crew", "Animal Avengers", "Pet Patrol",
            "Rescue Squad", "Animal Alliance", "Critter Coalition", "Beastie Brigade",
            "Furry Friends", "Paw Patrol", "Animal Kingdom", "Menagerie", "Ark",
            "Barnyard Brigade", "Farm Friends", "City Critters", "Suburban Squad",
            "Rural Rescuers", "Animal Angels", "Pet Protectors", "Guardian Animals",
            "Animal Advocates", "Voice for the Voiceless", "Animal Defenders", "Pet Police",
            "Furry Five-O", "Animal SWAT Team", "Pet Paramedics", "Animal EMTs",
            "Critter Cops", "Beastie Boys (and Girls)", "Furry Force", "Paw Power",
            "Animal Army", "Pet Platoon", "Rescue Regiment", "Animal Armada",
            "Critter Cavalry", "Beastie Battalion", "Furry Fleet", "Paw Platoon",
            "Animal Artillery", "Pet Brigade", "Rescue Rangers", "Animal Air Force",
            "Critter Commandos", "Beastie Berets", "Furry Fighters", "Paw Pilots",
            "Animal Aces", "Pet Pioneers", "Rescue Rocketeers", "Animal Astronauts",
            "Critter Comets", "Beastie Blasters", "Furry Fusion", "Paw Power-ups",
            "Animal Atoms", "Pet Particles", "Rescue Rays", "Animal Alliance",
            "Critter Collective", "Beastie Brotherhood", "Furry Fellowship", "Paw Pack",
            "Animal Assembly", "Pet Partnership", "Rescue Ring", "Animal Association",
            "Critter Circle", "Beastie Band", "Furry Family", "Paw Posse",
            "Animal Clan", "Pet Crew", "Rescue Mob", "Animal Crowd", "Critter Company",
            "Beastie Bunch", "Furry Gang", "Paw Party", "Animal Group", "Pet Gang",
            "Rescue Group", "Animal Team", "Critter Team", "Beastie Team", "Furry Team",
            "Paw Team", "Animal Squad", "Pet Squad", "Rescue Squad", "Animal Force",
            "Critter Force", "Beastie Force", "Furry Force", "Paw Force", "Animal Power",
            "Pet Power", "Rescue Power", "Animal Powerhouse", "Critter Powerhouse",
            "Beastie Powerhouse", "Furry Powerhouse", "Paw Powerhouse", "Animal Machine",
            "Pet Machine", "Rescue Machine", "Animal Engine", "Critter Engine",
            "Beastie Engine", "Furry Engine", "Paw Engine"
    };

    public List<AvailableVolunteerDto> getAvailableVolunteersForIncident(Long incidentId) {
        // ... (method remains the same) ...
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found."));
        List<User> availableUsers = userRepository.findByAvailabilityStatus(User.AvailabilityStatus.Available);
        Set<Long> interestedUserIds = interestedVolunteerRepository.findByIncidentId(incidentId)
                .stream()
                .map(interest -> interest.getUser().getId())
                .collect(Collectors.toSet());
        Set<Long> engagedUserIds = rescueCaseRepository.findByIsActiveTrue()
                .stream()
                .filter(rescueCase -> !rescueCase.getIncident().getId().equals(incidentId)) // Exclude the current incident
                .flatMap(rescueCase -> rescueCase.getAssignedTeam().getTeamMembers().stream())
                .map(teamMember -> teamMember.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> previouslyWorkedUserIds = incident.getCases().stream()
                .filter(c -> !c.isActive())
                .flatMap(c -> c.getAssignedTeam().getTeamMembers().stream())
                .map(tm -> tm.getUser().getId())
                .collect(Collectors.toSet());

        List<AvailableVolunteerDto> volunteers = availableUsers.stream()
                .map(user -> {
                    AvailableVolunteerDto dto = new AvailableVolunteerDto();
                    dto.setUserId(user.getId());
                    dto.setFirstName(user.getFirstName());
                    dto.setHasVehicle(user.getHasVehicle());
                    dto.setHasMedicineBox(user.getHasMedicineBox());
                    dto.setExperienceLevel(user.getExperienceLevel());
                    dto.setHasShownInterest(interestedUserIds.contains(user.getId()));
                    dto.setIsEngagedInActiveCase(engagedUserIds.contains(user.getId()));
                    dto.setDistanceFromIncident(calculateDistance(
                            user.getLatitude(), user.getLongitude(),
                            incident.getLatitude(), incident.getLongitude()
                    ));
                    dto.setHasPreviouslyWorkedOnIncident(previouslyWorkedUserIds.contains(user.getId()));
                    return dto;
                })
                .collect(Collectors.toList());

        volunteers.sort(Comparator
                .comparing(AvailableVolunteerDto::getHasPreviouslyWorkedOnIncident, Comparator.reverseOrder())
                .thenComparing(AvailableVolunteerDto::getHasShownInterest, Comparator.reverseOrder())
                .thenComparing(AvailableVolunteerDto::getDistanceFromIncident, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AvailableVolunteerDto::getExperienceLevel, Comparator.reverseOrder())
        );

        return volunteers;
    }

    public static Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // ... (method remains the same) ...
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return distance * 1.4; // Applying a circuity factor
    }

    @Transactional
    public AssignTeamResponseDto assignTeamToIncident(Long incidentId, AssignTeamDto assignTeamDto, Long assignerId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found."));
        List<User> users = userRepository.findAllById(assignTeamDto.getUserIds());
        if (users.size() != assignTeamDto.getUserIds().size()) {
            throw new IllegalArgumentException("One or more users not found.");
        }
        User assigner = userRepository.findById(assignerId)
                .orElseThrow(() -> new IllegalArgumentException("Assigner not found."));

        // --- Deactivate/Delete Old Case and Chat Logic ---
        Optional<RescueCase> activeCaseOpt = incident.getCases().stream()
                .filter(RescueCase::isActive)
                .findFirst();

        if (activeCaseOpt.isPresent()) {
            RescueCase activeCase = activeCaseOpt.get();
            String chatGroupIdToDelete = activeCase.getChatGroup() != null ? activeCase.getChatGroup().getId() : null;
            incident.getCases().remove(activeCase);
            rescueCaseRepository.delete(activeCase); // Delete the old case
            if (chatGroupIdToDelete != null) {
                chatService.deleteChatGroupAndData(chatGroupIdToDelete);
            }
        }
        incident.getCases().forEach(c -> c.setActive(false)); // Deactivate any other stragglers
        // --- End Old Case Logic ---

        Team teamToAssign = findOrCreateTeam(users);
        String chatGroupName = teamToAssign.getName() + " - Incident #" + incident.getId();
        ChatGroup newChatGroup = chatService.createChatGroup(assignTeamDto.getUserIds(), chatGroupName, "INCIDENT", incident.getId());

        RescueCase newRescueCase = new RescueCase();
        newRescueCase.setIncident(incident);
        newRescueCase.setAssignedTeam(teamToAssign);
        newRescueCase.setChatGroup(newChatGroup);
        newRescueCase.setAssignedBy(assigner);
        newRescueCase.setActive(true);
        RescueCase savedCase = rescueCaseRepository.save(newRescueCase);

        incident.setStatus(Incident.IncidentStatus.ASSIGNED);
        incident.setLastUpdated(LocalDateTime.now()); // Update timestamp
        incidentRepository.save(incident);

        // --- ✨ Send Notifications ---
        try {
            // 1. Notify Assigned Team Members
            String memberMessage = String.format("You have been assigned to Incident #%d ('%s') by %s.",
                    incident.getId(),
                    teamToAssign.getName(),
                    assigner.getFirstName());
            for (User member : users) {
                if (!member.getId().equals(assignerId)) {
                    notificationService.createNotification(
                            member,
                            NotificationType.INCIDENT,
                            incident.getStatus(), // Pass the current status (ASSIGNED)
                            memberMessage,
                            incident.getId(),
                            assigner
                    );
                }
            }

            // 2. Notify the Assigner (Confirmation)
            String assignerMessage = String.format("You successfully assigned Team '%s' to Incident #%d.",
                    teamToAssign.getName(),
                    incident.getId());
            notificationService.createNotification(
                    assigner,
                    NotificationType.INCIDENT,
                    incident.getStatus(), // Pass the current status (ASSIGNED)
                    assignerMessage,
                    incident.getId(),
                    null
            );
            logger.info("Sent assignment notifications for Incident ID: {} to {} members and assigner.", incident.getId(), users.size());

        } catch (Exception e) {
            logger.error("Failed to send assignment notifications for Incident ID: {}. Error: {}", incident.getId(), e.getMessage(), e);
        }
        // --- End Send Notifications ---

        return convertToDto(savedCase);
    }

    private AssignTeamResponseDto convertToDto(RescueCase rescueCase) {
        // ... (method remains the same) ...
        AssignTeamResponseDto dto = new AssignTeamResponseDto();
        dto.setCaseId(rescueCase.getId());
        dto.setIncidentId(rescueCase.getIncident().getId());
        dto.setTeamName(rescueCase.getAssignedTeam().getName());
        dto.setChatGroupId(rescueCase.getChatGroup().getId());

        List<TeamMemberDto> memberDtos = rescueCase.getAssignedTeam().getTeamMembers().stream()
                .map(member -> {
                    TeamMemberDto memberDto = new TeamMemberDto();
                    memberDto.setUserId(member.getUser().getId());
                    memberDto.setFirstName(member.getUser().getFirstName());
                    memberDto.setFullName(member.getUser().getFirstName() + " " + member.getUser().getLastName());
                    return memberDto;
                })
                .collect(Collectors.toList());
        dto.setTeamMembers(memberDtos);

        return dto;
    }

    // Changed to public to be used by RescueCaseService
    public Team findOrCreateTeam(List<User> users) {
        // ... (method remains the same) ...
        List<Long> userIds = users.stream().map(User::getId).sorted().collect(Collectors.toList()); // Ensure sorted IDs
        List<Team> existingTeams = teamRepository.findTeamsByExactMembers(userIds.size(), userIds);
        if (!existingTeams.isEmpty()) {
            return existingTeams.get(0);
        }
        Team newTeam = new Team();
        newTeam.setName("Team " + TEAM_NAMES[(int) (teamRepository.count() % TEAM_NAMES.length)]);
        for (User user : users) {
            TeamMember member = new TeamMember();
            member.setTeam(newTeam);
            member.setUser(user);
            newTeam.getTeamMembers().add(member);
        }
        return teamRepository.save(newTeam);
    }

    @Transactional(readOnly = true)
    public TeamDetailsDto getTeamDetailsByIncidentId(Long incidentId) {
        // ... (method remains the same) ...
        RescueCase rescueCase = rescueCaseRepository.findByIncidentIdAndIsActiveTrue(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("No active case found for this incident."));

        Team team = rescueCase.getAssignedTeam();
        if (team == null) {
            throw new IllegalArgumentException("No team assigned to this case.");
        }

        TeamDetailsDto dto = new TeamDetailsDto();
        dto.setTeamName(team.getName());

        if (rescueCase.getAssignedBy() != null) {
            dto.setAssignedBy(rescueCase.getAssignedBy().getFirstName());
        }

        List<TeamMemberDto> memberDtos = team.getTeamMembers().stream()
                .map(member -> {
                    TeamMemberDto memberDto = new TeamMemberDto();
                    memberDto.setUserId(member.getUser().getId());
                    memberDto.setFirstName(member.getUser().getFirstName());
                    memberDto.setFullName(member.getUser().getFirstName() + " " + member.getUser().getLastName());
                    return memberDto;
                })
                .collect(Collectors.toList());
        dto.setTeamMembers(memberDtos);

        return dto;
    }

    public Set<String> getTeamKitItems(Long incidentId) {

        // 1. Find the active rescue case
        RescueCase rescueCase = rescueCaseRepository.findByIncidentIdAndIsActiveTrue(incidentId)
                .orElseThrow(() -> new RuntimeException("No active case found for this incident"));

        // 2. Find the team for this case
        Team team = rescueCase.getAssignedTeam();
        if (team == null) {
            return Set.of(); // Return an empty set if no team is assigned
        }

        // 3. Get all user IDs from the team who have a medicine box
        List<Long> userIdsWithKits = team.getTeamMembers().stream()
                .map(TeamMember::getUser)
                .filter(User::getHasMedicineBox)
                .map(User::getId) // ✨ Get the user ID (Long)
                .toList();

        if (userIdsWithKits.isEmpty()) {
            return Set.of(); // No users with kits, return empty set
        }

        // 4. Fetch all kits in ONE efficient query using the new method
        List<FirstAidKit> kits = firstAidKitRepository.findByUserIdIn(userIdsWithKits);

        // 5. Collect all item names into a Set
        return kits.stream()
                .flatMap(kit -> kit.getItems().stream()) // Get all items from all kits
                .map(item -> item.getInventoryItem().getName()) // Get the name of each item
                .collect(Collectors.toSet()); // Collect into a Set to get unique names
    }
}