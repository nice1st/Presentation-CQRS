package demo.cqrs.participant.controller;

import demo.cqrs.participant.domain.CallParticipant;
import demo.cqrs.participant.service.ParticipantQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantQueryController {

    private final ParticipantQueryService participantQueryService;

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<CallParticipant>> getParticipantsByAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(participantQueryService.getParticipantsByAgentId(agentId));
    }

    @GetMapping("/call/{sessionId}")
    public ResponseEntity<List<CallParticipant>> getParticipantsByCall(@PathVariable String sessionId) {
        return ResponseEntity.ok(participantQueryService.getParticipantsBySessionId(sessionId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CallParticipant>> getActiveParticipants(@RequestParam Long agentId) {
        return ResponseEntity.ok(participantQueryService.getActiveParticipantsByAgentId(agentId));
    }
}
