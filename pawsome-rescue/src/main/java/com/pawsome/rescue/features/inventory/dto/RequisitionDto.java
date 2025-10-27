package com.pawsome.rescue.features.inventory.dto;

import com.pawsome.rescue.features.inventory.entity.Requisition;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class RequisitionDto {
    private Long id;
    private String userName;
    private List<RequisitionItemDto> items;
    private Requisition.RequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime acknowledgedAt;
}