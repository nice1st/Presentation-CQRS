package demo.cqrs.agent.controller;

import demo.cqrs.agent.service.AgentExtensionCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agents/events")
@RequiredArgsConstructor
public class AgentExtensionController {

    private final AgentExtensionCommandService agentExtensionCommandService;

    @PostMapping("/available")
    public ResponseEntity<Map<String, String>> handleAvailable(@RequestBody Map<String, String> request) {
        String extensionNumber = request.get("extensionNumber");
        String sessionId = request.get("sessionId");

        agentExtensionCommandService.handleExtensionAvailable(extensionNumber, sessionId);

        return ResponseEntity.ok(Map.of("message", "AVAILABLE event handled", "extensionNumber", extensionNumber));
    }

    @PostMapping("/unavailable")
    public ResponseEntity<Map<String, String>> handleUnavailable(@RequestBody Map<String, String> request) {
        String extensionNumber = request.get("extensionNumber");

        agentExtensionCommandService.handleExtensionUnavailable(extensionNumber);

        return ResponseEntity.ok(Map.of("message", "UNAVAILABLE event handled", "extensionNumber", extensionNumber));
    }

    @PostMapping("/busy")
    public ResponseEntity<Map<String, String>> handleBusy(@RequestBody Map<String, String> request) {
        String extensionNumber = request.get("extensionNumber");
        String sessionId = request.get("sessionId");

        agentExtensionCommandService.handleExtensionBusy(extensionNumber, sessionId);

        return ResponseEntity.ok(Map.of("message", "BUSY event handled", "extensionNumber", extensionNumber));
    }
}
