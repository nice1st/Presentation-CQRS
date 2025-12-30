package demo.cqrs.agent.domain;

public record AgentCreated(Long agentId, AgentType type, String name, String extensionNumber) {
}
