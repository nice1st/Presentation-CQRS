package demo.cqrs.agent.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType type;

    private String extensionNumber;

    public static Agent create(String name, AgentType type, String extensionNumber) {
        Agent agent = new Agent();
        agent.name = name;
        agent.type = type;
        agent.extensionNumber = extensionNumber;
        return agent;
    }
}
