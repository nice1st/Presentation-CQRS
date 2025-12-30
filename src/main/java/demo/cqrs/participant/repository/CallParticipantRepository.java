package demo.cqrs.participant.repository;

import demo.cqrs.participant.domain.CallParticipant;
import demo.cqrs.participant.domain.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CallParticipantRepository extends JpaRepository<CallParticipant, Long> {

    List<CallParticipant> findByAgentId(Long agentId);

    List<CallParticipant> findBySessionId(String sessionId);

    Optional<CallParticipant> findByAgentIdAndSessionIdAndStatus(
            Long agentId,
            String sessionId,
            ParticipantStatus status
    );

    List<CallParticipant> findByAgentIdAndStatus(Long agentId, ParticipantStatus status);
}
