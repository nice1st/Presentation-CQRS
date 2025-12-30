package demo.cqrs.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.cqrs.agent.event.ExtensionEventStore;
import demo.cqrs.agent.event.ExtensionEventType;
import demo.cqrs.agent.repository.ExtensionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentExtensionCommandService {

    private final ExtensionEventRepository extensionEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handleExtensionAvailable(String extensionNumber, String sessionId) {
        log.info("Handling AVAILABLE event: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        try {
            Map<String, Object> payload = Map.of(
                    "extensionNumber", extensionNumber,
                    "sessionId", sessionId != null ? sessionId : "",
                    "timestamp", Instant.now().toString()
            );

            ExtensionEventStore event = ExtensionEventStore.builder()
                    .eventType(ExtensionEventType.AVAILABLE)
                    .extensionNumber(extensionNumber)
                    .occurredAt(Instant.now())
                    .payload(objectMapper.writeValueAsString(payload))
                    .sessionId(sessionId)
                    .build();

            extensionEventRepository.save(event);
            eventPublisher.publishEvent(event);

            log.info("AVAILABLE event saved and published: extensionNumber={}", extensionNumber);
        } catch (Exception e) {
            log.error("Failed to handle AVAILABLE event", e);
            throw new RuntimeException("Failed to handle AVAILABLE event", e);
        }
    }

    @Transactional
    public void handleExtensionUnavailable(String extensionNumber) {
        log.info("Handling UNAVAILABLE event: extensionNumber={}", extensionNumber);

        try {
            Map<String, Object> payload = Map.of(
                    "extensionNumber", extensionNumber,
                    "timestamp", Instant.now().toString()
            );

            ExtensionEventStore event = ExtensionEventStore.builder()
                    .eventType(ExtensionEventType.UNAVAILABLE)
                    .extensionNumber(extensionNumber)
                    .occurredAt(Instant.now())
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            extensionEventRepository.save(event);
            eventPublisher.publishEvent(event);

            log.info("UNAVAILABLE event saved and published: extensionNumber={}", extensionNumber);
        } catch (Exception e) {
            log.error("Failed to handle UNAVAILABLE event", e);
            throw new RuntimeException("Failed to handle UNAVAILABLE event", e);
        }
    }

    @Transactional
    public void handleExtensionBusy(String extensionNumber, String sessionId) {
        log.info("Handling BUSY event: extensionNumber={}, sessionId={}", extensionNumber, sessionId);

        try {
            Map<String, Object> payload = Map.of(
                    "extensionNumber", extensionNumber,
                    "sessionId", sessionId,
                    "timestamp", Instant.now().toString()
            );

            ExtensionEventStore event = ExtensionEventStore.builder()
                    .eventType(ExtensionEventType.BUSY)
                    .extensionNumber(extensionNumber)
                    .occurredAt(Instant.now())
                    .payload(objectMapper.writeValueAsString(payload))
                    .sessionId(sessionId)
                    .build();

            extensionEventRepository.save(event);
            eventPublisher.publishEvent(event);

            log.info("BUSY event saved and published: extensionNumber={}", extensionNumber);
        } catch (Exception e) {
            log.error("Failed to handle BUSY event", e);
            throw new RuntimeException("Failed to handle BUSY event", e);
        }
    }
}
