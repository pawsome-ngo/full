package com.pawsome.rescue.features.incident;

import com.pawsome.rescue.features.incident.entity.Incident;
import com.pawsome.rescue.features.incident.entity.InterestedVolunteer;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.features.incident.repository.IncidentRepository;
import com.pawsome.rescue.features.incident.repository.InterestedVolunteerRepository;
import com.pawsome.rescue.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterestService {

    @Autowired
    private InterestedVolunteerRepository interestedVolunteerRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public InterestedVolunteer expressInterest(Long incidentId, Long userId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + incidentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        InterestedVolunteer interest = new InterestedVolunteer();
        interest.setIncident(incident);
        interest.setUser(user);

        return interestedVolunteerRepository.save(interest);
    }

    public void removeInterest(Long incidentId, Long userId) {
        interestedVolunteerRepository.deleteByIncidentIdAndUserId(incidentId, userId);
    }

    public void removeAllInterestsForIncident(Long incidentId) {
        interestedVolunteerRepository.deleteAllByIncidentId(incidentId);
    }
}