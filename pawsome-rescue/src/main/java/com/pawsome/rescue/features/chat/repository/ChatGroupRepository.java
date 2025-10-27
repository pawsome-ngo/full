package com.pawsome.rescue.features.chat.repository;

import com.pawsome.rescue.features.chat.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, String> {
}