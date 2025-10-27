package com.pawsome.rescue.config;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.features.inventory.dto.FirstAidKitItemDto;
import com.pawsome.rescue.features.inventory.dto.InventoryItemDto;
import com.pawsome.rescue.features.inventory.dto.RequisitionDto;
import com.pawsome.rescue.features.inventory.dto.RequisitionItemDto;
import com.pawsome.rescue.features.inventory.entity.FirstAidKitItem;
import com.pawsome.rescue.features.inventory.entity.InventoryItem;
import com.pawsome.rescue.features.inventory.entity.Requisition;
import com.pawsome.rescue.features.inventory.entity.RequisitionItem;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // --- Maps a User object to a full name string ---
        Converter<User, String> userToNameConverter = context -> {
            User user = context.getSource();
            if (user == null) {
                return null;
            }
            return user.getFirstName() + " " + user.getLastName();
        };

        // --- Maps Requisition to RequisitionDto ---
        modelMapper.typeMap(Requisition.class, RequisitionDto.class).addMappings(mapper -> {
            mapper.using(userToNameConverter).map(Requisition::getUser, RequisitionDto::setUserName);
        });

        // --- Maps RequisitionItem to RequisitionItemDto ---
        modelMapper.typeMap(RequisitionItem.class, RequisitionItemDto.class).addMappings(mapper -> {
            mapper.map(src -> src.getInventoryItem().getName(), RequisitionItemDto::setInventoryItemName);
        });

        // --- Maps FirstAidKitItem to FirstAidKitItemDto ---
        modelMapper.typeMap(FirstAidKitItem.class, FirstAidKitItemDto.class).addMappings(mapper -> {
            mapper.map(src -> src.getInventoryItem().getName(), FirstAidKitItemDto::setInventoryItemName);
        });

        // --- NEW MAPPING for InventoryItem ---
        // This ensures the category ID and name are correctly mapped to the DTO
        modelMapper.typeMap(InventoryItem.class, InventoryItemDto.class).addMappings(mapper -> {
            mapper.map(src -> src.getCategory().getId(), InventoryItemDto::setCategoryId);
            mapper.map(src -> src.getCategory().getName(), InventoryItemDto::setCategoryName);
        });

        return modelMapper;
    }
}