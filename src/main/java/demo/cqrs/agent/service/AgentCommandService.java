package demo.cqrs.agent.service;

import demo.cqrs.agent.domain.Agent;
import demo.cqrs.agent.domain.AgentCreated;
import demo.cqrs.agent.domain.AgentType;
import demo.cqrs.agent.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommandService {

    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Agent createAgent(String name, AgentType type, String extensionNumber) {
        log.info("Creating agent: name={}, type={}", name, type);

        Agent agent = Agent.create(name, type, extensionNumber);
        Agent saved = agentRepository.save(agent);

        AgentCreated event = new AgentCreated(
                saved.getId(),
                saved.getType(),
                saved.getName(),
                saved.getExtensionNumber()
        );
        eventPublisher.publishEvent(event);

        log.info("Agent created: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }
}
