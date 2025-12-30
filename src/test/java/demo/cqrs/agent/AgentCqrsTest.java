package demo.cqrs.agent;

import demo.cqrs.agent.domain.AgentType;
import demo.cqrs.agent.repository.AgentViewRepository;
import demo.cqrs.agent.service.AgentCommandService;
import demo.cqrs.agent.view.AgentStatus;
import demo.cqrs.agent.view.AgentView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class AgentCqrsTest {

    @Autowired
    private AgentCommandService agentCommandService;

    @Autowired
    private AgentViewRepository agentViewRepository;

    @Test
    void Agent_생성_시_AgentView_프로젝션() {
        // Given
        String name = "Alice";
        AgentType type = AgentType.HUMAN;
        String extensionNumber = "1001";

        // When
        var agent = agentCommandService.createAgent(name, type, extensionNumber);

        // Then: 비동기 프로젝션 대기
        await().until(() -> agentViewRepository.findById(agent.getId()).isPresent());

        AgentView view = agentViewRepository.findById(agent.getId()).orElseThrow();
        assertThat(view.getAgentId()).isEqualTo(agent.getId());
        assertThat(view.getName()).isEqualTo(name);
        assertThat(view.getType()).isEqualTo(type);
        assertThat(view.getExtensionNumber()).isEqualTo(extensionNumber);
        assertThat(view.getStatus()).isEqualTo(AgentStatus.UNAVAILABLE);
    }
}
