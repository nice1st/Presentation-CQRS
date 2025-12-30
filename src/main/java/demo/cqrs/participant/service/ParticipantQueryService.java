package demo.cqrs.participant.service;

import demo.cqrs.participant.domain.CallParticipant;
import demo.cqrs.participant.domain.ParticipantStatus;
import demo.cqrs.participant.repository.CallParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParticipantQueryService {

    private final CallParticipantRepository callParticipantRepository;

    public List<CallParticipant> getParticipantsByAgentId(Long agentId) {
        return callParticipantRepository.findByAgentId(agentId);
    }

    public List<CallParticipant> getParticipantsBySessionId(String sessionId) {
        return callParticipantRepository.findBySessionId(sessionId);
    }

    public List<CallParticipant> getActiveParticipantsByAgentId(Long agentId) {
        return callParticipantRepository.findByAgentIdAndStatus(agentId, ParticipantStatus.JOINED);
    }
}
