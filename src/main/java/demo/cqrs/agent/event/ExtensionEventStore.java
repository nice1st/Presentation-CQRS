package demo.cqrs.agent.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "extension_event_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtensionEventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionEventType eventType;

    @Column(nullable = false)
    private String extensionNumber;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    // Best-effort 추출 필드 (실패해도 이벤트 저장에 영향 없음)
    private String sessionId;  // 연관된 통화 (BUSY일 때)

    @Builder
    public ExtensionEventStore(
            ExtensionEventType eventType,
            String extensionNumber,
            Instant occurredAt,
            String payload,
            String sessionId
    ) {
        this.eventType = eventType;
        this.extensionNumber = extensionNumber;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.sessionId = sessionId;
    }
}
