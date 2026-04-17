package com.FormFlow.FormFlow.Controller.AI;

import com.FormFlow.FormFlow.Service.AI.ChatBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @PostMapping("/generateForm")
    public ResponseEntity<?> generateForm(@RequestBody Map<String, String> request) {
        return chatBotService.generateForm(request);
    }
}