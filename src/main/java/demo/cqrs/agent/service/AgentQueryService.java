package demo.cqrs.agent.service;

import demo.cqrs.agent.view.AgentView;
import demo.cqrs.agent.repository.AgentViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentQueryService {

    private final AgentViewRepository agentViewRepository;

    public List<AgentView> getAllAgents() {
        return agentViewRepository.findAll();
    }

    public Optional<AgentView> getAgentById(Long agentId) {
        return agentViewRepository.findById(agentId);
    }
}
