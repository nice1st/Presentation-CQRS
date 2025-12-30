package demo.cqrs.call.controller;

import demo.cqrs.call.view.CallView;
import demo.cqrs.call.service.CallQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallQueryController {

    private final CallQueryService callQueryService;

    @GetMapping
    public ResponseEntity<List<CallView>> getAllCalls() {
        return ResponseEntity.ok(callQueryService.getAllCalls());
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CallView> getCall(@PathVariable String sessionId) {
        return callQueryService.getCallBySessionId(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
