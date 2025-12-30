package demo.cqrs.call.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.cqrs.call.event.CallEventStore;
import demo.cqrs.call.event.CallEventType;
import demo.cqrs.call.repository.CallEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallCommandService {

    private final CallEventRepository callEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handleCallRequested(String sessionId, String source, String destination) {
        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "source", source,
                "destination", destination,
                "timestamp", Instant.now().toString()
        );

        saveAndPublishEvent(
                CallEventType.REQUESTED,
                sessionId,
                payload,
                builder -> builder
                        .source(source)
                        .destination(destination)
        );
    }

    @Transactional
    public void handleCallConnected(String sessionId) {
        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "timestamp", Instant.now().toString()
        );

        saveAndPublishEvent(CallEventType.CONNECTED, sessionId, payload, builder -> {});
    }

    @Transactional
    public void handleCallDisconnected(String sessionId) {
        Map<String, Object> payload = Map.of(
                "sessionId", sessionId,
                "timestamp", Instant.now().toString()
        );

        saveAndPublishEvent(CallEventType.DISCONNECTED, sessionId, payload, builder -> {});
    }

    private void saveAndPublishEvent(
            CallEventType eventType,
            String sessionId,
            Map<String, Object> payload,
            Consumer<CallEventStore.CallEventStoreBuilder> extraFields
    ) {
        log.info("Handling {} event: sessionId={}", eventType, sessionId);

        try {
            CallEventStore.CallEventStoreBuilder builder = CallEventStore.builder()
                    .eventType(eventType)
                    .sessionId(sessionId)
                    .occurredAt(Instant.now())
                    .payload(objectMapper.writeValueAsString(payload));

            extraFields.accept(builder);

            CallEventStore event = builder.build();
            callEventRepository.save(event);
            eventPublisher.publishEvent(event);

            log.info("{} event saved and published: sessionId={}", eventType, sessionId);
        } catch (Exception e) {
            log.error("Failed to handle {} event", eventType, e);
            throw new RuntimeException("Failed to handle " + eventType + " event", e);
        }
    }
}
