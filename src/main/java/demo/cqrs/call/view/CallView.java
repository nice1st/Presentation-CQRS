package demo.cqrs.call.view;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "call_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallView {

    @Id
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant startedAt;

    private Instant endedAt;

    private Long duration; // 초 단위

    private String source;
    private String destination;

    // Mock Agent 정보
    private String agentId = "agent-1";
    private String agentType = "AI";

    public static CallView create(String sessionId, Instant requestedAt, String source, String destination) {
        CallView view = new CallView();
        view.sessionId = sessionId;
        view.status = CallStatus.REQUESTED;
        view.requestedAt = requestedAt;
        view.source = source;
        view.destination = destination;
        return view;
    }

    public void start() {
        if (this.status != CallStatus.REQUESTED) {
            return; // 멱등성: 이미 ACTIVE면 skip
        }
        this.status = CallStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    public void end() {
        if (this.status == CallStatus.ENDED) {
            return; // 멱등성: 이미 ENDED면 skip
        }
        this.status = CallStatus.ENDED;
        this.endedAt = Instant.now();
        if (this.startedAt != null) {
            this.duration = Duration.between(startedAt, endedAt).getSeconds();
        }
    }
}
