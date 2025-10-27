package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.UserSummaryDto;
import com.pawsome.rescue.features.user.dto.UserDetailDto;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllVolunteers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getAvailabilityStatus))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private UserSummaryDto convertToDto(User user) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFirstName() + " " + user.getLastName());
        dto.setAvailabilityStatus(user.getAvailabilityStatus());
        dto.setPosition(user.getPosition());
        return dto;
    }

    @Transactional(readOnly = true)
    public UserDetailDto getVolunteerDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return convertToDetailDto(user);
    }

    private UserDetailDto convertToDetailDto(User user) {
        UserDetailDto dto = new UserDetailDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setJoinedSince(user.getJoinedSince());
        dto.setAvailabilityStatus(user.getAvailabilityStatus());
        dto.setPosition(user.getPosition());
        dto.setExperienceLevel(user.getExperienceLevel());
        dto.setHasMedicineBox(user.getHasMedicineBox());

        dto.setRoles(user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .collect(Collectors.toList()));
        return dto;
    }
}