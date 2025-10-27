package com.pawsome.rescue.features.admin.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class BatchDeleteUserRequestDto {
    private List<Long> userIds;
    private boolean notifyUsers = false;
}