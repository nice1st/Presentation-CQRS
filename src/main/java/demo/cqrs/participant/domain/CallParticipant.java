package demo.cqrs.participant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "call_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status;

    public static CallParticipant join(Long agentId, String sessionId) {
        CallParticipant participant = new CallParticipant();
        participant.agentId = agentId;
        participant.sessionId = sessionId;
        participant.joinedAt = Instant.now();
        participant.status = ParticipantStatus.JOINED;
        return participant;
    }

    public void leave() {
        if (this.status == ParticipantStatus.LEFT) {
            return; // 멱등성: 이미 LEFT면 skip
        }
        this.status = ParticipantStatus.LEFT;
        this.leftAt = Instant.now();
    }
}
