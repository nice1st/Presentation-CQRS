package demo.cqrs.call.repository;

import demo.cqrs.call.view.CallView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallViewRepository extends JpaRepository<CallView, String> {
}
