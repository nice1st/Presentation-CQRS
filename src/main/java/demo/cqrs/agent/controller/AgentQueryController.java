package demo.cqrs.agent.controller;

import demo.cqrs.agent.view.AgentView;
import demo.cqrs.agent.service.AgentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentQueryController {

    private final AgentQueryService agentQueryService;

    @GetMapping
    public ResponseEntity<List<AgentView>> getAllAgents() {
        return ResponseEntity.ok(agentQueryService.getAllAgents());
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentView> getAgent(@PathVariable Long agentId) {
        return agentQueryService.getAgentById(agentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
