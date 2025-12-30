package demo.cqrs.call.service;

import demo.cqrs.call.view.CallView;
import demo.cqrs.call.repository.CallViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CallQueryService {

    private final CallViewRepository callViewRepository;

    public List<CallView> getAllCalls() {
        return callViewRepository.findAll();
    }

    public Optional<CallView> getCallBySessionId(String sessionId) {
        return callViewRepository.findById(sessionId);
    }
}
