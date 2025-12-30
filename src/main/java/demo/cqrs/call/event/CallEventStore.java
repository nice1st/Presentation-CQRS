package demo.cqrs.call.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "call_event_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallEventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallEventType eventType;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    // Best-effort 추출 필드 (실패해도 이벤트 저장에 영향 없음)
    private String source;
    private String destination;
    private String answeredBy;
    private String endedBy;

    @Builder
    public CallEventStore(
            CallEventType eventType,
            String sessionId,
            Instant occurredAt,
            String payload,
            String source,
            String destination,
            String answeredBy,
            String endedBy
    ) {
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.source = source;
        this.destination = destination;
        this.answeredBy = answeredBy;
        this.endedBy = endedBy;
    }
}
