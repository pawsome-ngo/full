package com.pawsome.rescue.features.chat.repository;

import com.pawsome.rescue.features.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByChatGroup_IdOrderByTimestampAsc(String chatId);
    void deleteAllByChatGroupId(String chatGroupId);
    Message findTopByChatGroupIdOrderByTimestampDesc(String chatId);
}