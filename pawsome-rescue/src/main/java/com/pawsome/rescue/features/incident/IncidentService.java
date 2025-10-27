package com.pawsome.rescue.features.incident; // Updated package

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.archive.IncidentArchive;
import com.pawsome.rescue.features.archive.IncidentArchiveRepository;
import com.pawsome.rescue.features.casemanagement.dto.RescueCaseHistoryDto;
import com.pawsome.rescue.features.casemanagement.dto.TeamMemberDto;
import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import com.pawsome.rescue.features.casemanagement.entity.TeamMember;
import com.pawsome.rescue.features.chat.entity.ChatGroup;
import com.pawsome.rescue.features.chat.repository.*;
import com.pawsome.rescue.features.incident.dto.*;
import com.pawsome.rescue.features.incident.entity.Incident;
import com.pawsome.rescue.features.incident.entity.IncidentMedia;
import com.pawsome.rescue.features.incident.exception.IncidentNotFoundException; // Assuming this is in the exception subpackage
// **Removed:** import com.pawsome.rescue.features.incident.mapper.IncidentMapper;
import com.pawsome.rescue.features.incident.repository.IncidentMediaRepository;
import com.pawsome.rescue.features.incident.repository.IncidentRepository;
import com.pawsome.rescue.features.incident.repository.InterestedVolunteerRepository;
import com.pawsome.rescue.features.notification.NotificationService;
import com.pawsome.rescue.features.notification.NotificationType;
import com.pawsome.rescue.features.storage.LocalStorageService;
import com.pawsome.rescue.features.user.dto.UpdateLocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentMediaRepository incidentMediaRepository;
    @Autowired private LocalStorageService localStorageService;
    @Autowired private InterestedVolunteerRepository interestedVolunteerRepository;
    @Autowired private ReactionRepository reactionRepository;
    @Autowired private ReadReceiptRepository readReceiptRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ChatGroupRepository chatGroupRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private IncidentArchiveRepository incidentArchiveRepository;

    // **Removed:** @Autowired(required = false) private IncidentMapper incidentMapper;


    // --- convertToDto METHOD (Keep existing logic) ---
    public IncidentDto convertToDto(Incident incident) {
        IncidentDto dto = new IncidentDto();

        dto.setId(incident.getId());
        dto.setInformerName(incident.getInformerName());
        dto.setContactNumber(incident.getContactNumber());
        dto.setLatitude(incident.getLatitude());
        dto.setLongitude(incident.getLongitude());
        dto.setLocation(incident.getLocation());
        dto.setAnimalType(incident.getAnimalType());
        dto.setDescription(incident.getDescription());
        dto.setStatus(incident.getStatus());
        dto.setReportedAt(incident.getReportedAt());
        dto.setCaseCount(incident.getCaseCount());
        dto.setLastUpdated(incident.getLastUpdated());

        // Populate mediaFiles list
        List<IncidentMediaDto> mediaDtos = incident.getMediaFiles().stream()
                .map(media -> {
                    if (media == null || media.getFilePath() == null) {
                        logger.warn("Skipping media item due to null object or file path for incident ID {}", incident.getId());
                        return null;
                    }
                    String fileName = media.getFilePath();
                    String webLink = "/api/uploads/" + fileName;
                    Long caseId = media.getRescueCase() != null ? media.getRescueCase().getId() : null;
                    return new IncidentMediaDto(media.getId(), webLink, media.getMediaType(), caseId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        dto.setMediaFiles(mediaDtos);
        dto.setMediaFileCount(mediaDtos.size());

        // Fetch interested users
        List<InterestedUserDto> interestedUserDtos = interestedVolunteerRepository.findByIncidentId(incident.getId()).stream()
                .map(interestedVolunteer -> {
                    User user = interestedVolunteer.getUser();
                    InterestedUserDto userDto = new InterestedUserDto();
                    userDto.setId(user.getId());
                    userDto.setFirstName(user.getFirstName());
                    return userDto;
                })
                .collect(Collectors.toList());
        dto.setInterestedUsers(interestedUserDtos);

        return dto;
    }

    // --- getIncidentById (Uses convertToDto) ---
    @Transactional(readOnly = true)
    public Optional<IncidentDto> getIncidentById(Long id) {
        return incidentRepository.findById(id).map(this::convertToDto);
    }

    // --- getIncidentsByStatus (Uses convertToDto) ---
    @Transactional(readOnly = true)
    public List<IncidentDto> getIncidentsByStatus(Incident.IncidentStatus status) {
        return incidentRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // --- getAllIncidents (Uses convertToDto) ---
    @Transactional(readOnly = true)
    public List<IncidentDto> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // --- reportIncident (Keep existing logic) ---
    @Transactional
    public Incident reportIncident(IncidentReportDto reportDto, List<MultipartFile> mediaFiles) {
        Incident incident = new Incident();
        incident.setInformerName(reportDto.getInformerName());
        incident.setContactNumber(reportDto.getContactNumber());
        incident.setLatitude(reportDto.getLatitude());
        incident.setLongitude(reportDto.getLongitude());
        incident.setLocation(reportDto.getLocation());
        incident.setAnimalType(reportDto.getAnimalType());
        incident.setDescription(reportDto.getDescription());
        incident.setLastUpdated(LocalDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);

        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                try {
                    String fileName = localStorageService.storeFile(file);
                    IncidentMedia media = new IncidentMedia();
                    media.setIncident(savedIncident);
                    media.setFilePath(fileName);
                    media.setMediaType(getMediaType(file.getContentType()));
                    incidentMediaRepository.save(media);
                } catch (Exception e) {
                    logger.error("Could not store file: {}", file.getOriginalFilename(), e);
                }
            }
            savedIncident = incidentRepository.findById(savedIncident.getId()).orElse(savedIncident);
        }

        sendNotificationsForNewIncident(savedIncident);

        return savedIncident;
    }

    // --- sendNotificationsForNewIncident (Keep existing logic) ---
    @Async
    @Transactional(readOnly = true)
    public void sendNotificationsForNewIncident(Incident savedIncident) {
        try {
            List<User> availableUsers = userRepository.findByAvailabilityStatus(User.AvailabilityStatus.Available)
                    .stream()
                    .toList();

            if (!availableUsers.isEmpty()) {
                String message = String.format("New %s incident (#%d) reported near '%s'. Reported by %s.",
                        savedIncident.getAnimalType(),
                        savedIncident.getId(),
                        savedIncident.getLocation() != null ? savedIncident.getLocation() : "Unknown Location",
                        savedIncident.getInformerName());

                for (User recipient : availableUsers) {
                    notificationService.createNotification(
                            recipient,
                            NotificationType.INCIDENT,
                            savedIncident.getStatus(),
                            message,
                            savedIncident.getId(),
                            null
                    );
                }
                logger.info("Sent 'New Incident' notification to {} available users for Incident ID: {}", availableUsers.size(), savedIncident.getId());
            } else {
                logger.info("No available users found to notify for new Incident ID: {}", savedIncident.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to send new incident notifications (async) for Incident ID: {}. Error: {}", savedIncident.getId(), e.getMessage(), e);
        }
    }


    // --- updateIncidentLocation (Uses convertToDto) ---
    @Transactional
    public Optional<IncidentDto> updateIncidentLocation(Long id, UpdateLocationDto locationDto) {
        Optional<Incident> incidentOptional = incidentRepository.findById(id);
        if (incidentOptional.isPresent()) {
            Incident incident = incidentOptional.get();
            incident.setLatitude(locationDto.getLatitude());
            incident.setLongitude(locationDto.getLongitude());
//            incident.setLastUpdated(LocalDateTime.now());
            Incident savedIncident = incidentRepository.save(incident);
            return Optional.of(convertToDto(savedIncident)); // Convert within transaction
        }
        return Optional.empty();
    }

    // --- updateIncidentStatus (Keep existing logic) ---
    @Transactional
    public Optional<Incident> updateIncidentStatus(Long id, Incident.IncidentStatus newStatus) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));
        incident.setStatus(newStatus);
        incident.setLastUpdated(LocalDateTime.now());
        return Optional.of(incidentRepository.save(incident));
    }


    // --- updateIncidentDetails METHOD (Uses convertToDto) ---
    @Transactional
    public IncidentDto updateIncidentDetails(Long incidentId, UpdateIncidentRequest updateRequest) {
        logger.debug("SERVICE - Attempting to update details for incident ID: {}", incidentId);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> {
                    logger.warn("SERVICE - Incident not found for ID: {}", incidentId);
                    return new IncidentNotFoundException("Incident not found with ID: " + incidentId);
                });

        incident.setInformerName(updateRequest.getInformerName());
        incident.setContactNumber(updateRequest.getContactNumber());
        incident.setAnimalType(updateRequest.getAnimalType());
        incident.setDescription(updateRequest.getDescription());
        incident.setLocation(updateRequest.getLocation());
        incident.setLatitude(updateRequest.getLatitude());
        incident.setLongitude(updateRequest.getLongitude());

        Incident savedIncident = incidentRepository.save(incident);
        logger.info("SERVICE - Incident ID {} updated successfully.", incidentId);

        // **Always use manual conversion now**
        return convertToDto(savedIncident);
    }


    // --- initiateCase (Keep existing logic) ---
    @Transactional
    public Optional<Incident> initiateCase(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getStatus() != Incident.IncidentStatus.ASSIGNED) {
            throw new IllegalStateException("Only an ASSIGNED case can be initiated.");
        }

        incident.setStatus(Incident.IncidentStatus.IN_PROGRESS);
        incident.setLastUpdated(LocalDateTime.now());
        return Optional.of(incidentRepository.save(incident));
    }

    // --- resolveIncident (Keep existing logic) ---
    @Transactional
    public Optional<Incident> resolveIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getStatus() != Incident.IncidentStatus.ONGOING) {
            throw new IllegalStateException("Only an ONGOING case can be resolved.");
        }

        incident.setStatus(Incident.IncidentStatus.RESOLVED);
        incident.setLastUpdated(LocalDateTime.now());
        return Optional.of(incidentRepository.save(incident));
    }

    // --- closeIncident (Keep existing logic) ---
    @Transactional
    public Optional<Incident> closeIncident(Long id, String reason) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getStatus() != Incident.IncidentStatus.REPORTED) {
            throw new IllegalStateException("Only a REPORTED case can be closed.");
        }

        incident.setStatus(Incident.IncidentStatus.CLOSED);
        incident.setClosingReason(reason);
        incident.setLastUpdated(LocalDateTime.now());
        return Optional.of(incidentRepository.save(incident));
    }

    // --- reactivateIncident (Keep existing logic) ---
    @Transactional
    public Optional<Incident> reactivateIncident(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getStatus() != Incident.IncidentStatus.RESOLVED) {
            throw new IllegalStateException("Only a RESOLVED incident can be reactivated.");
        }

        incident.setStatus(Incident.IncidentStatus.ONGOING);
        incident.setClosingReason(null); // Clear reason on reactivation
        incident.setLastUpdated(LocalDateTime.now());
        return Optional.of(incidentRepository.save(incident));
    }

    // --- deleteIncident & archiveIncident (Keep existing logic) ---
    @Transactional
    public void deleteIncident(Long id, boolean shouldArchive) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + id));

        if (incident.getStatus() != Incident.IncidentStatus.RESOLVED && incident.getStatus() != Incident.IncidentStatus.CLOSED) {
            throw new IllegalStateException("Only RESOLVED or CLOSED incidents can be deleted.");
        }

        if (shouldArchive) {
            try {
                archiveIncident(incident);
            } catch (Exception e) {
                logger.error("Failed to archive incident {}: {}. Proceeding with deletion.", id, e.getMessage(), e);
            }
        }

        deleteAllMediaForIncident(id);
        interestedVolunteerRepository.deleteAllByIncidentId(id);

        for (RescueCase rescueCase : Optional.ofNullable(incident.getCases()).orElse(Collections.emptyList())) {
            if (rescueCase == null) continue; // Skip null cases if any
            ChatGroup chatGroup = rescueCase.getChatGroup();
            if (chatGroup != null) {
                String chatGroupId = chatGroup.getId();
                logger.info("Deleting chat data for chat group ID: {}", chatGroupId);
                try {
                    reactionRepository.deleteAllByChatGroupId(chatGroupId);
                    readReceiptRepository.deleteAllByChatGroupId(chatGroupId);
                    messageRepository.deleteAllByChatGroupId(chatGroupId);
                    participantRepository.deleteAllByChatGroupId(chatGroupId);
                    rescueCase.setChatGroup(null);
                    chatGroupRepository.delete(chatGroup);
                } catch (Exception e) {
                    logger.error("Error cleaning up chat group {} for incident {}: {}", chatGroupId, id, e.getMessage(), e);
                }
            }
        }

        incidentRepository.delete(incident);
        logger.info("Successfully hard-deleted incident ID: {}", id);
    }

    private void archiveIncident(Incident incident) {
        logger.info("Archiving incident ID: {}", incident.getId());
        IncidentArchive archive = new IncidentArchive();

        archive.setOriginalIncidentId(incident.getId());
        archive.setInformerName(incident.getInformerName());
        archive.setContactNumber(incident.getContactNumber());
        archive.setLocation(incident.getLocation());
        archive.setAnimalType(incident.getAnimalType() != null ? incident.getAnimalType().name() : "UNKNOWN");
        archive.setDescription(incident.getDescription());
        archive.setFinalStatus(incident.getStatus() != null ? incident.getStatus().name() : "UNKNOWN");
        archive.setReportedAt(incident.getReportedAt());
        archive.setClosingReason(incident.getClosingReason());

        Set<String> memberNames = new HashSet<>();
        List<String> resolutionNotes = new ArrayList<>();

        if (incident.getCases() != null) {
            for (RescueCase rescueCase : incident.getCases()) {
                if (rescueCase == null) continue; // Skip null cases
                if (!rescueCase.isActive() && rescueCase.getResolutionNotes() != null) {
                    resolutionNotes.add(rescueCase.getResolutionNotes());
                }
                if (rescueCase.getAssignedTeam() != null && rescueCase.getAssignedTeam().getTeamMembers() != null) {
                    for (TeamMember member : rescueCase.getAssignedTeam().getTeamMembers()) {
                        if (member != null && member.getUser() != null) {
                            memberNames.add(member.getUser().getFirstName() + " " + member.getUser().getLastName());
                        }
                    }
                }
            }
        }

        archive.setResolutionNotes(String.join("\n---\n", resolutionNotes));
        archive.setInvolvedMembers(String.join(", ", memberNames));

        incidentArchiveRepository.save(archive);
        logger.info("Successfully archived incident ID: {}", incident.getId());
    }

    // --- deleteSingleMediaItem (Keep existing logic) ---
    @Transactional
    public boolean deleteSingleMediaItem(Long incidentId, Long mediaId) {
        Optional<IncidentMedia> mediaOptional = incidentMediaRepository.findById(mediaId);

        if (mediaOptional.isEmpty()) {
            logger.warn("Attempted to delete non-existent media item with ID: {}", mediaId);
            throw new IncidentNotFoundException("Media item not found with ID: " + mediaId);
        }

        IncidentMedia media = mediaOptional.get();

        if (media.getIncident() == null || !media.getIncident().getId().equals(incidentId)) {
            logger.warn("Attempted to delete media item {} which belongs to incident {} but requested under incident {}",
                    mediaId, media.getIncident() != null ? media.getIncident().getId() : "null", incidentId);
            throw new IllegalArgumentException("Media item does not belong to the specified incident.");
        }

        try {
            if (media.getFilePath() != null && !media.getFilePath().isEmpty()) {
                localStorageService.deleteFile(media.getFilePath());
                logger.info("Successfully deleted file '{}' from storage for media ID {}", media.getFilePath(), mediaId);
            } else {
                logger.warn("Media item {} had no file path associated, skipping file deletion.", mediaId);
            }
        } catch (Exception e) {
            logger.error("Failed to delete file '{}' from storage for media ID {}: {}. Proceeding with DB record deletion.",
                    media.getFilePath(), mediaId, e.getMessage());
        }

        incidentMediaRepository.delete(media);
        logger.info("Successfully deleted media record with ID: {}", mediaId);
        return true;
    }


    // --- deleteAllMediaForIncident (Keep existing logic) ---
    @Transactional
    public void deleteAllMediaForIncident(Long incidentId) {
        List<IncidentMedia> mediaList = incidentMediaRepository.findByIncidentId(incidentId);
        logger.info("Found {} media items to delete for incident ID: {}", mediaList.size(), incidentId);
        for (IncidentMedia media : mediaList) {
            try {
                if (media != null && media.getFilePath() != null && !media.getFilePath().isEmpty()) { // Added null check for media
                    localStorageService.deleteFile(media.getFilePath());
                    logger.debug("Deleted file from storage: {}", media.getFilePath());
                }
            } catch (Exception e) {
                logger.error("Failed to delete file from local storage: {}", media != null ? media.getFilePath() : "null", e);
            }
        }
        if (!mediaList.isEmpty()) {
            incidentMediaRepository.deleteAll(mediaList);
            logger.info("Deleted all media records for incident ID: {}", incidentId);
        } else {
            logger.info("No media records found to delete for incident ID: {}", incidentId);
        }
    }

    // --- Other Helper Methods (getMediaType, convertToSummaryDto, getIncidentHistory, convertToCaseHistoryDto, loadMediaFile) ---
    public Resource loadMediaFile(String filename) {
        return localStorageService.loadFileAsResource(filename);
    }

    public IncidentMedia.MediaType getMediaType(String contentType) {
        if (contentType == null) return null;
        if (contentType.startsWith("image")) return IncidentMedia.MediaType.IMAGE;
        if (contentType.startsWith("video")) return IncidentMedia.MediaType.VIDEO;
        if (contentType.startsWith("audio")) return IncidentMedia.MediaType.AUDIO;
        logger.warn("Unsupported media content type: {}", contentType);
        return null; // Or a default like UNKNOWN
    }

    // Ensure @Transactional for lazy loading
    @Transactional(readOnly = true)
    public IncidentSummaryDto convertToSummaryDto(Incident incident) { // Changed to public for potential reuse
        IncidentSummaryDto dto = new IncidentSummaryDto();
        dto.setId(incident.getId());
        dto.setInformerName(incident.getInformerName());
        dto.setLocation(incident.getLocation());
        dto.setAnimalType(incident.getAnimalType());
        dto.setStatus(incident.getStatus());
        dto.setCaseCount(incident.getCaseCount());
        dto.setLastUpdated(incident.getLastUpdated());

        Optional<RescueCase> activeCaseOpt = Optional.ofNullable(incident.getCases()).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).filter(RescueCase::isActive).findFirst();
        activeCaseOpt.ifPresent(rescueCase -> {
            if (rescueCase.getAssignedBy() != null) {
                dto.setAssignedByUserId(rescueCase.getAssignedBy().getId());
            }
        });
        return dto;
    }

    // Ensure @Transactional for lazy loading
    @Transactional(readOnly = true)
    public List<IncidentSummaryDto> getAllIncidentsSummary() {
        return incidentRepository.findAll().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    // Ensure @Transactional for lazy loading
    @Transactional(readOnly = true)
    public List<IncidentSummaryDto> getLiveIncidentsSummary() {
        List<Incident.IncidentStatus> excludedStatuses = List.of(Incident.IncidentStatus.RESOLVED, Incident.IncidentStatus.CLOSED);
        return incidentRepository.findAll().stream()
                .filter(incident -> !excludedStatuses.contains(incident.getStatus()))
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }


    // Ensure @Transactional for lazy loading
    @Transactional(readOnly = true)
    public IncidentHistoryDto getIncidentHistory(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("Incident not found with ID: " + incidentId));

        List<RescueCaseHistoryDto> caseHistories = Optional.ofNullable(incident.getCases()).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).filter(rc -> !rc.isActive())
                .map(this::convertToCaseHistoryDto)
                .collect(Collectors.toList());

        IncidentHistoryDto historyDto = new IncidentHistoryDto();
        historyDto.setCases(caseHistories);
        return historyDto;
    }

    private RescueCaseHistoryDto convertToCaseHistoryDto(RescueCase rescueCase) {
        RescueCaseHistoryDto dto = new RescueCaseHistoryDto();
        dto.setCaseId(rescueCase.getId());
        dto.setResolutionNotes(rescueCase.getResolutionNotes());
        dto.setClosedAt(rescueCase.getClosedAt());
        dto.setAssignedBy(rescueCase.getAssignedBy() != null ? rescueCase.getAssignedBy().getFirstName() : "N/A");

        if (rescueCase.getAssignedTeam() != null) {
            dto.setTeamName(rescueCase.getAssignedTeam().getName());
            List<TeamMemberDto> memberDtos = Optional.ofNullable(rescueCase.getAssignedTeam().getTeamMembers()).get()
                    .stream().filter(member -> member != null && member.getUser() != null)
                    .map(member -> {
                        TeamMemberDto memberDto = new TeamMemberDto();
                        memberDto.setUserId(member.getUser().getId());
                        memberDto.setFirstName(member.getUser().getFirstName());
                        // memberDto.setFullName(member.getUser().getFullName()); // Add if User entity has this
                        return memberDto;
                    }).collect(Collectors.toList());
            dto.setTeamMembers(memberDtos);
        } else {
            dto.setTeamName("N/A");
            dto.setTeamMembers(Collections.emptyList());
        }
        return dto;
    }

}