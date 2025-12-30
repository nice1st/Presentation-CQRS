package demo.cqrs.agent.domain;

import demo.cqrs.agent.view.AgentStatus;

public record AgentStatusChanged(
        String extensionNumber,
        AgentStatus status,
        String sessionId  // nullable (AVAILABLE 전환 시에만 사용)
) {
}
