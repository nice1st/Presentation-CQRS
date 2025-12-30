package demo.cqrs.participant.handler;

import demo.cqrs.agent.event.ExtensionEventStore;
import demo.cqrs.agent.event.ExtensionEventType;
import demo.cqrs.participant.service.ParticipantProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParticipantProjectionHandler {

    private final ParticipantProjectionService projectionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtensionEvent(ExtensionEventStore event) {
        log.debug("Received ExtensionEvent: type={}, extensionNumber={}, sessionId={}",
                event.getEventType(), event.getExtensionNumber(), event.getSessionId());

        if (event.getEventType() == ExtensionEventType.BUSY && event.getSessionId() != null) {
            projectionService.handleAgentBusy(event.getExtensionNumber(), event.getSessionId());
        } else if (event.getEventType() == ExtensionEventType.AVAILABLE && event.getSessionId() != null) {
            projectionService.handleAgentAvailable(event.getExtensionNumber(), event.getSessionId());
        }
    }
}
