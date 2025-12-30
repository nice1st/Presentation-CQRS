package demo.cqrs.agent.handler;

import demo.cqrs.agent.domain.AgentCreated;
import demo.cqrs.agent.event.ExtensionEventStore;
import demo.cqrs.agent.service.AgentProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentProjectionHandler {

    private final AgentProjectionService projectionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgentCreated(AgentCreated event) {
        log.debug("Received AgentCreated: agentId={}", event.agentId());
        projectionService.handleAgentCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtensionEvent(ExtensionEventStore event) {
        log.debug("Received ExtensionEvent: type={}, extensionNumber={}", event.getEventType(), event.getExtensionNumber());

        switch (event.getEventType()) {
            case AVAILABLE -> projectionService.handleExtensionAvailable(event.getExtensionNumber());
            case UNAVAILABLE -> projectionService.handleExtensionUnavailable(event.getExtensionNumber());
            case BUSY -> projectionService.handleExtensionBusy(event.getExtensionNumber());
        }
    }
}
