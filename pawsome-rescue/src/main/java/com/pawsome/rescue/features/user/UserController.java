package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.UserDetailDto;
import com.pawsome.rescue.features.user.dto.UserSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserSummaryDto>> getAllVolunteers() {
        List<UserSummaryDto> volunteers = userService.getAllVolunteers();
        return ResponseEntity.ok(volunteers);
    }

    // --- NEW ENDPOINT: To fetch details for the profile page ---
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDto> getVolunteerDetails(@PathVariable Long id) {
        UserDetailDto details = userService.getVolunteerDetails(id);
        return ResponseEntity.ok(details);
    }

}