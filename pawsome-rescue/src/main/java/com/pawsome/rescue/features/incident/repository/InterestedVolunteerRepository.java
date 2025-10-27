package com.pawsome.rescue.features.incident.repository;

import com.pawsome.rescue.features.incident.entity.InterestedVolunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InterestedVolunteerRepository extends JpaRepository<InterestedVolunteer, InterestedVolunteer.InterestedVolunteerId> {

    // Custom query to delete a single interest record
    @Transactional
    @Modifying
    @Query("DELETE FROM InterestedVolunteer iv WHERE iv.incident.id = ?1 AND iv.user.id = ?2")
    void deleteByIncidentIdAndUserId(Long incidentId, Long userId);

    // Custom query to delete all interest records for a given incident
    @Transactional
    @Modifying
    @Query("DELETE FROM InterestedVolunteer iv WHERE iv.incident.id = ?1")
    void deleteAllByIncidentId(Long incidentId);
    // New method to find all volunteers interested in a specific incident
    List<InterestedVolunteer> findByIncidentId(Long incidentId);


}