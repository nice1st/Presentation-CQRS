package demo.cqrs.call.handler;

import demo.cqrs.call.event.CallEventStore;
import demo.cqrs.call.service.CallProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallProjectionHandler {

    private final CallProjectionService projectionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCallEvent(CallEventStore event) {
        log.debug("Received event: type={}, sessionId={}", event.getEventType(), event.getSessionId());

        switch (event.getEventType()) {
            case REQUESTED -> projectionService.handleRequested(event);
            case CONNECTED -> projectionService.handleConnected(event);
            case DISCONNECTED -> projectionService.handleDisconnected(event);
        }
    }
}
