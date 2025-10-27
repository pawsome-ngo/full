package com.pawsome.rescue.features.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ItemInUseDto {
    private boolean inUse;
    private List<String> userNames;
}