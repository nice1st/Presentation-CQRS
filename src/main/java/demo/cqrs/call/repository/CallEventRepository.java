package demo.cqrs.call.repository;

import demo.cqrs.call.event.CallEventStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallEventRepository extends JpaRepository<CallEventStore, Long> {
    List<CallEventStore> findBySessionIdOrderByOccurredAt(String sessionId);
}
