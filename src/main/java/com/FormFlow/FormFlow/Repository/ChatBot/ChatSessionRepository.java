package com.FormFlow.FormFlow.Repository.ChatBot;

import com.FormFlow.FormFlow.Entity.ChatBot.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
}
