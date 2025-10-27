package com.pawsome.rescue.features.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderboardDto {
    private long id;
    private int rank;
    private String firstName;
    private int points;
    private int hearts;
    private double distanceTraveled;
    private int casesCompleted;
}