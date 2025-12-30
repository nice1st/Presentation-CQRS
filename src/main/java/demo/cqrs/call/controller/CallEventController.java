package demo.cqrs.call.controller;

import demo.cqrs.call.service.CallCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/calls/events")
@RequiredArgsConstructor
public class CallEventController {

    private final CallCommandService callCommandService;

    @PostMapping("/requested")
    public ResponseEntity<Map<String, String>> handleRequested(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String source = request.get("source");
        String destination = request.get("destination");

        callCommandService.handleCallRequested(sessionId, source, destination);

        return ResponseEntity.ok(Map.of("message", "REQUESTED event handled", "sessionId", sessionId));
    }

    @PostMapping("/connected")
    public ResponseEntity<Map<String, String>> handleConnected(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");

        callCommandService.handleCallConnected(sessionId);

        return ResponseEntity.ok(Map.of("message", "CONNECTED event handled", "sessionId", sessionId));
    }

    @PostMapping("/disconnected")
    public ResponseEntity<Map<String, String>> handleDisconnected(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");

        callCommandService.handleCallDisconnected(sessionId);

        return ResponseEntity.ok(Map.of("message", "DISCONNECTED event handled", "sessionId", sessionId));
    }
}
