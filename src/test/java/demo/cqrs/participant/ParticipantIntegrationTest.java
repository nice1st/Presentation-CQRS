package demo.cqrs.participant;

import demo.cqrs.agent.domain.AgentType;
import demo.cqrs.agent.service.AgentCommandService;
import demo.cqrs.agent.service.AgentExtensionCommandService;
import demo.cqrs.call.service.CallCommandService;
import demo.cqrs.participant.domain.CallParticipant;
import demo.cqrs.participant.domain.ParticipantStatus;
import demo.cqrs.participant.repository.CallParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class ParticipantIntegrationTest {

    @Autowired
    private AgentCommandService agentCommandService;

    @Autowired
    private AgentExtensionCommandService agentExtensionCommandService;

    @Autowired
    private CallCommandService callCommandService;

    @Autowired
    private CallParticipantRepository callParticipantRepository;

    @Test
    void Agent와_Call의_관계_추적() throws InterruptedException {
        // Given: Agent 생성
        var agent = agentCommandService.createAgent("Bob", AgentType.HUMAN, "2001");
        await().until(() -> agent.getId() != null);

        String sessionId = "test-call-integration-001";

        // When: Call 요청
        callCommandService.handleCallRequested(sessionId, "customer-1", "2001");

        // When: Agent BUSY (통화 연결)
        agentExtensionCommandService.handleExtensionBusy("2001", sessionId);

        // Then: CallParticipant 생성 확인 (비동기)
        await().until(() -> !callParticipantRepository.findBySessionId(sessionId).isEmpty());

        List<CallParticipant> participants = callParticipantRepository.findBySessionId(sessionId);
        assertThat(participants).hasSize(1);

        CallParticipant participant = participants.get(0);
        assertThat(participant.getAgentId()).isEqualTo(agent.getId());
        assertThat(participant.getSessionId()).isEqualTo(sessionId);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.JOINED);
        assertThat(participant.getJoinedAt()).isNotNull();
        assertThat(participant.getLeftAt()).isNull();

        // When: Call 연결
        callCommandService.handleCallConnected(sessionId);

        // When: Call 종료
        callCommandService.handleCallDisconnected(sessionId);

        // When: Agent AVAILABLE (통화 종료)
        agentExtensionCommandService.handleExtensionAvailable("2001", sessionId);

        // Then: CallParticipant 종료 확인 (비동기)
        await().until(() -> {
            CallParticipant p = callParticipantRepository.findById(participant.getId()).orElseThrow();
            return p.getStatus() == ParticipantStatus.LEFT;
        });

        CallParticipant leftParticipant = callParticipantRepository.findById(participant.getId()).orElseThrow();
        assertThat(leftParticipant.getStatus()).isEqualTo(ParticipantStatus.LEFT);
        assertThat(leftParticipant.getLeftAt()).isNotNull();
    }

    @Test
    void Agent가_현재_처리중인_Call_조회() {
        // Given: Agent 생성
        var agent = agentCommandService.createAgent("Charlie", AgentType.AI, "3001");
        await().until(() -> agent.getId() != null);

        String sessionId1 = "test-call-query-001";
        String sessionId2 = "test-call-query-002";

        // When: 첫 번째 Call BUSY
        callCommandService.handleCallRequested(sessionId1, "customer-1", "3001");
        agentExtensionCommandService.handleExtensionBusy("3001", sessionId1);

        await().until(() -> !callParticipantRepository.findBySessionId(sessionId1).isEmpty());

        // Then: Active participant 1개
        List<CallParticipant> activeParticipants = callParticipantRepository
                .findByAgentIdAndStatus(agent.getId(), ParticipantStatus.JOINED);
        assertThat(activeParticipants).hasSize(1);
        assertThat(activeParticipants.get(0).getSessionId()).isEqualTo(sessionId1);

        // When: 첫 번째 Call 종료, 두 번째 Call BUSY
        Long firstParticipantId = activeParticipants.get(0).getId();
        agentExtensionCommandService.handleExtensionAvailable("3001", sessionId1);
        await().until(() -> {
            return callParticipantRepository.findById(firstParticipantId)
                    .map(cp -> cp.getStatus() == ParticipantStatus.LEFT)
                    .orElse(false);
        });

        callCommandService.handleCallRequested(sessionId2, "customer-2", "3001");
        agentExtensionCommandService.handleExtensionBusy("3001", sessionId2);

        await().until(() -> {
            List<CallParticipant> active = callParticipantRepository
                    .findByAgentIdAndStatus(agent.getId(), ParticipantStatus.JOINED);
            return !active.isEmpty() && active.get(0).getSessionId().equals(sessionId2);
        });

        // Then: Active participant가 두 번째 Call로 변경됨
        activeParticipants = callParticipantRepository
                .findByAgentIdAndStatus(agent.getId(), ParticipantStatus.JOINED);
        assertThat(activeParticipants).hasSize(1);
        assertThat(activeParticipants.get(0).getSessionId()).isEqualTo(sessionId2);
    }
}
