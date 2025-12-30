package demo.cqrs.agent.repository;

import demo.cqrs.agent.view.AgentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentViewRepository extends JpaRepository<AgentView, Long> {
    Optional<AgentView> findByExtensionNumber(String extensionNumber);
}
