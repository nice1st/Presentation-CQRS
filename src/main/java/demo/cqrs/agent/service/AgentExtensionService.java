package demo.cqrs.agent.service;

import demo.cqrs.agent.domain.AgentStatusChanged;
import demo.cqrs.agent.repository.AgentViewRepository;
import demo.cqrs.agent.view.AgentStatus;
import demo.cqrs.agent.view.AgentView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentExtensionService {

    private final AgentViewRepository agentViewRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handleBusy(String extensionNumber, String sessionId) {
        log.info("Handling BUSY event: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        AgentView agentView = findAgentViewByExtension(extensionNumber);
        if (agentView != null) {
            agentView.updateStatus(AgentStatus.BUSY);
            log.info("AgentView updated to BUSY: agentId={}", agentView.getAgentId());
        }

        // 이벤트 발행
        AgentStatusChanged event = new AgentStatusChanged(extensionNumber, AgentStatus.BUSY, sessionId);
        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void handleAvailable(String extensionNumber, String sessionId) {
        log.info("Handling AVAILABLE event: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        AgentView agentView = findAgentViewByExtension(extensionNumber);
        if (agentView != null) {
            agentView.updateStatus(AgentStatus.AVAILABLE);
            log.info("AgentView updated to AVAILABLE: agentId={}", agentView.getAgentId());
        }

        // 이벤트 발행
        AgentStatusChanged event = new AgentStatusChanged(extensionNumber, AgentStatus.AVAILABLE, sessionId);
        eventPublisher.publishEvent(event);
    }

    private AgentView findAgentViewByExtension(String extensionNumber) {
        return agentViewRepository.findAll().stream()
                .filter(av -> extensionNumber.equals(av.getExtensionNumber()))
                .findFirst()
                .orElse(null);
    }
}
