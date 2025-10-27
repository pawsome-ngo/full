package com.pawsome.rescue.features.incident.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "interested_volunteers")
@Getter
@Setter
@IdClass(InterestedVolunteer.InterestedVolunteerId.class)
public class InterestedVolunteer {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "expressed_at", updatable = false)
    private LocalDateTime expressedAt = LocalDateTime.now();

    // Composite Key Class
    public static class InterestedVolunteerId implements Serializable {
        private Long incident;
        private Long user;

        // equals and hashCode methods are crucial for composite keys
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InterestedVolunteerId that = (InterestedVolunteerId) o;
            return Objects.equals(incident, that.incident) && Objects.equals(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(incident, user);
        }
    }
}