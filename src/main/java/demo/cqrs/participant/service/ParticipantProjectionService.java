package demo.cqrs.participant.service;

import demo.cqrs.agent.repository.AgentViewRepository;
import demo.cqrs.agent.view.AgentView;
import demo.cqrs.participant.domain.CallParticipant;
import demo.cqrs.participant.domain.ParticipantStatus;
import demo.cqrs.participant.repository.CallParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantProjectionService {

    private final CallParticipantRepository callParticipantRepository;
    private final AgentViewRepository agentViewRepository;

    @Transactional
    public void handleAgentBusy(String extensionNumber, String sessionId) {
        log.info("Handling Agent BUSY: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        // ExtensionNumber로 AgentView 조회
        AgentView agentView = agentViewRepository.findByExtensionNumber(extensionNumber)
                .orElse(null);

        if (agentView == null) {
            log.warn("AgentView not found for extensionNumber: {}", extensionNumber);
            return;
        }

        // CallParticipant 생성
        CallParticipant participant = CallParticipant.join(agentView.getAgentId(), sessionId);
        callParticipantRepository.save(participant);

        log.info("CallParticipant created: agentId={}, sessionId={}", agentView.getAgentId(), sessionId);
    }

    @Transactional
    public void handleAgentAvailable(String extensionNumber, String sessionId) {
        log.info("Handling Agent AVAILABLE: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        // ExtensionNumber로 AgentView 조회
        AgentView agentView = agentViewRepository.findByExtensionNumber(extensionNumber)
                .orElse(null);

        if (agentView == null) {
            log.warn("AgentView not found for extensionNumber: {}", extensionNumber);
            return;
        }

        // JOINED 상태의 CallParticipant 찾기
        callParticipantRepository.findByAgentIdAndSessionIdAndStatus(
                agentView.getAgentId(),
                sessionId,
                ParticipantStatus.JOINED
        ).ifPresentOrElse(
                participant -> {
                    participant.leave();
                    log.info("CallParticipant left: agentId={}, sessionId={}", agentView.getAgentId(), sessionId);
                },
                () -> log.warn("Active CallParticipant not found: agentId={}, sessionId={}", agentView.getAgentId(), sessionId)
        );
    }
}
