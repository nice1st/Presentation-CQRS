package demo.cqrs.agent.view;

import demo.cqrs.agent.domain.AgentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agent_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentView {

    @Id
    private Long agentId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType type;

    private String extensionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    public static AgentView create(Long agentId, String name, AgentType type, String extensionNumber) {
        AgentView view = new AgentView();
        view.agentId = agentId;
        view.name = name;
        view.type = type;
        view.extensionNumber = extensionNumber;
        view.status = AgentStatus.UNAVAILABLE;
        return view;
    }

    public void updateStatus(AgentStatus status) {
        this.status = status;
    }
}
