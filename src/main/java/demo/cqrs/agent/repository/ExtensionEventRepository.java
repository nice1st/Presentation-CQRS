package demo.cqrs.agent.repository;

import demo.cqrs.agent.event.ExtensionEventStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtensionEventRepository extends JpaRepository<ExtensionEventStore, Long> {
    List<ExtensionEventStore> findByExtensionNumberOrderByOccurredAt(String extensionNumber);
}
