package demo.cqrs.agent.service;

import demo.cqrs.agent.domain.AgentCreated;
import demo.cqrs.agent.view.AgentStatus;
import demo.cqrs.agent.view.AgentView;
import demo.cqrs.agent.repository.AgentViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentProjectionService {

    private final AgentViewRepository agentViewRepository;

    @Transactional
    public void handleAgentCreated(AgentCreated event) {
        log.info("Handling AgentCreated: agentId={}", event.agentId());

        AgentView view = AgentView.create(
                event.agentId(),
                event.name(),
                event.type(),
                event.extensionNumber()
        );

        agentViewRepository.save(view);
        log.info("AgentView created: agentId={}", event.agentId());
    }

    @Transactional
    public void handleExtensionAvailable(String extensionNumber) {
        log.info("Handling Extension AVAILABLE: extensionNumber={}", extensionNumber);

        agentViewRepository.findByExtensionNumber(extensionNumber).ifPresent(view -> {
            view.updateStatus(AgentStatus.AVAILABLE);
            agentViewRepository.save(view);
            log.info("AgentView status updated to AVAILABLE: extensionNumber={}", extensionNumber);
        });
    }

    @Transactional
    public void handleExtensionUnavailable(String extensionNumber) {
        log.info("Handling Extension UNAVAILABLE: extensionNumber={}", extensionNumber);

        agentViewRepository.findByExtensionNumber(extensionNumber).ifPresent(view -> {
            view.updateStatus(AgentStatus.UNAVAILABLE);
            agentViewRepository.save(view);
            log.info("AgentView status updated to UNAVAILABLE: extensionNumber={}", extensionNumber);
        });
    }

    @Transactional
    public void handleExtensionBusy(String extensionNumber) {
        log.info("Handling Extension BUSY: extensionNumber={}", extensionNumber);

        agentViewRepository.findByExtensionNumber(extensionNumber).ifPresent(view -> {
            view.updateStatus(AgentStatus.BUSY);
            agentViewRepository.save(view);
            log.info("AgentView status updated to BUSY: extensionNumber={}", extensionNumber);
        });
    }
}
