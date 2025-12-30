package demo.cqrs.call;

import demo.cqrs.call.event.CallEventStore;
import demo.cqrs.call.repository.CallEventRepository;
import demo.cqrs.call.repository.CallViewRepository;
import demo.cqrs.call.service.CallCommandService;
import demo.cqrs.call.view.CallStatus;
import demo.cqrs.call.view.CallView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class CallEventSourcingTest {

    @Autowired
    private CallCommandService callCommandService;

    @Autowired
    private CallEventRepository callEventRepository;

    @Autowired
    private CallViewRepository callViewRepository;

    @Test
    void 통화_생명주기_이벤트_소싱() throws InterruptedException {
        // Given
        String sessionId = "test-call-001";

        // When: REQUESTED
        callCommandService.handleCallRequested(sessionId, "customer-1", "agent-1");
        await().until(() -> callViewRepository.findById(sessionId).isPresent());

        // Then: Event Store 확인
        List<CallEventStore> events = callEventRepository.findBySessionIdOrderByOccurredAt(sessionId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSessionId()).isEqualTo(sessionId);

        // Then: CallView 확인
        CallView view = callViewRepository.findById(sessionId).orElseThrow();
        assertThat(view.getStatus()).isEqualTo(CallStatus.REQUESTED);

        // When: CONNECTED
        callCommandService.handleCallConnected(sessionId);
        await().until(() -> {
            CallView v = callViewRepository.findById(sessionId).orElseThrow();
            return v.getStatus() == CallStatus.ACTIVE;
        });

        // Then
        events = callEventRepository.findBySessionIdOrderByOccurredAt(sessionId);
        assertThat(events).hasSize(2);
        view = callViewRepository.findById(sessionId).orElseThrow();
        assertThat(view.getStatus()).isEqualTo(CallStatus.ACTIVE);
        assertThat(view.getStartedAt()).isNotNull();

        // When: DISCONNECTED
        callCommandService.handleCallDisconnected(sessionId);
        await().until(() -> {
            CallView v = callViewRepository.findById(sessionId).orElseThrow();
            return v.getStatus() == CallStatus.ENDED;
        });

        // Then
        events = callEventRepository.findBySessionIdOrderByOccurredAt(sessionId);
        assertThat(events).hasSize(3);
        view = callViewRepository.findById(sessionId).orElseThrow();
        assertThat(view.getStatus()).isEqualTo(CallStatus.ENDED);
        assertThat(view.getEndedAt()).isNotNull();
        assertThat(view.getDuration()).isNotNull();
        assertThat(view.getDuration()).isGreaterThanOrEqualTo(0);
    }
}
