package demo.cqrs.call.service;

import demo.cqrs.call.event.CallEventStore;
import demo.cqrs.call.view.CallView;
import demo.cqrs.call.repository.CallViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallProjectionService {

    private final CallViewRepository callViewRepository;

    @Transactional
    public void handleRequested(CallEventStore event) {
        String sessionId = event.getSessionId();

        if (callViewRepository.existsById(sessionId)) {
            log.warn("Duplicate REQUESTED event, skipping: sessionId={}", sessionId);
            return;
        }

        CallView view = CallView.create(
                sessionId,
                event.getOccurredAt(),
                event.getSource(),
                event.getDestination()
        );

        callViewRepository.save(view);
        log.info("CallView created: sessionId={}", sessionId);
    }

    @Transactional
    public void handleConnected(CallEventStore event) {
        String sessionId = event.getSessionId();

        callViewRepository.findById(sessionId).ifPresentOrElse(
                view -> {
                    view.start();
                    log.info("CallView started: sessionId={}", sessionId);
                },
                () -> log.warn("CallView not found for CONNECTED event: sessionId={}", sessionId)
        );
    }

    @Transactional
    public void handleDisconnected(CallEventStore event) {
        String sessionId = event.getSessionId();

        callViewRepository.findById(sessionId).ifPresentOrElse(
                view -> {
                    view.end();
                    log.info("CallView ended: sessionId={}, duration={}s", sessionId, view.getDuration());
                },
                () -> log.warn("CallView not found for DISCONNECTED event: sessionId={}", sessionId)
        );
    }
}
