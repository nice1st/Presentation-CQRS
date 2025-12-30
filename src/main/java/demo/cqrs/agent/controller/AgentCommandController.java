package demo.cqrs.agent.controller;

import demo.cqrs.agent.domain.Agent;
import demo.cqrs.agent.domain.AgentType;
import demo.cqrs.agent.service.AgentCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentCommandController {

    private final AgentCommandService agentCommandService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAgent(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        AgentType type = AgentType.valueOf(request.get("type"));
        String extensionNumber = request.get("extensionNumber");

        Agent agent = agentCommandService.createAgent(name, type, extensionNumber);

        return ResponseEntity.ok(Map.of(
                "message", "Agent created",
                "agentId", agent.getId(),
                "name", agent.getName()
        ));
    }
}
