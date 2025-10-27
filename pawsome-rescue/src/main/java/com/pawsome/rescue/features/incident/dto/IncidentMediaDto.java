package com.pawsome.rescue.features.incident.dto;

import com.pawsome.rescue.features.incident.entity.IncidentMedia;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidentMediaDto {
    private Long id;
    private String url;
    private IncidentMedia.MediaType mediaType;
    private Long caseId; // <-- ADD THIS

    public IncidentMediaDto(Long id, String url, IncidentMedia.MediaType mediaType, Long caseId) {
        this.id = id;
        this.url = url;
        this.mediaType = mediaType;
        this.caseId = caseId; // <-- ADD THIS
    }
}