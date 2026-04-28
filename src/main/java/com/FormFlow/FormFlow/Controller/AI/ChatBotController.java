package com.FormFlow.FormFlow.Controller.AI;

import com.FormFlow.FormFlow.DTO.ChatBot.AgentResponseDTO;
import com.FormFlow.FormFlow.Service.AI.AgentService;
import com.FormFlow.FormFlow.Service.AI.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @Autowired
    private AgentService agentService;

    @PostMapping("/generateForm")
    public ResponseEntity<?> generateForm(@RequestBody Map<String, String> request) {
        return chatBotService.generateForm(request);
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentResponseDTO> chat(@RequestBody Map<String, String> req) {
        try {
            UUID sessionId = req.get("sessionId") != null && !req.get("sessionId").isBlank()
                    ? UUID.fromString(req.get("sessionId"))
                    : null;

            String message = req.get("message");

            AgentResponseDTO response = agentService.handleChat(sessionId, message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            AgentResponseDTO response = new AgentResponseDTO();
            response.setType("error");
            response.setMessage("Invalid sessionId");
            response.setComplete(false);
            return ResponseEntity.badRequest().body(response);
        }
    }
}