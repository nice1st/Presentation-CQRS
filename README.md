# Event Sourcing & CQRS 패턴 학습

> Tech Talk 시리즈 - Event Sourcing과 CQRS 패턴 실전 적용 가이드

## 프레젠테이션 자료

📊 **[`doc/PRESENTATION.md`](doc/PRESENTATION.md)** - 5단계 학습 시나리오 (발표용 스크립트)

🧪 **[`doc/demo-script.http`](doc/demo-script.http)** - 실습 데모 시나리오 (IntelliJ HTTP Client)

<details>
<summary><b>📋 프레젠테이션 흐름 (5단계, 50분)</b></summary>

**Step 1**: 통화 이력을 보고 싶다 → **Event Sourcing** 도입

**Step 2**: 상태를 빠르게 조회하고 싶다 → **CQRS** (Read Model 분리)

**Step 3**: 통화를 처리한 상담원을 알고 싶다 → **Participant 도메인** 추가

**Step 4**: 상담원 상태를 추적하고 싶다 → **Extension Event Store** (CQRS + Event Sourcing)

**Step 5**: 받은 이벤트로 서비스를 확장하고 싶다 → **이벤트 다중 구독**

각 단계마다 "문제 → 고민 → 해결 → 결과" 과정으로 패턴을 자연스럽게 학습합니다.

</details>

**학습 목표**:
- Event Sourcing: 모든 상태 변경을 이벤트로 기록 (감사 추적, 시간 여행)
- CQRS: Command/Query 분리로 독립 최적화
- 도메인 독립성: 이벤트 기반으로 느슨한 결합 구현

## 데모 실행

```bash
./gradlew bootRun  # 포트 8090
```

**H2 Console**: http://localhost:8090/h2-console (JDBC URL: `jdbc:h2:mem:demo-cqrs`, Username: `sa`)

실행 후 `doc/demo-script.http`의 3가지 데모 시나리오를 순서대로 실행하세요.

## 구현 특징

✅ **3개 독립 도메인이 이벤트로만 통신** - Spring ApplicationEvent 활용, 도메인 간 직접 의존 제거

✅ **Event Sourcing + CQRS 동시 구현** - Call(Event Sourcing), Agent(CQRS + Event Sourcing), Participant(관계 추적)

✅ **프로덕션 수준 설계** - Payload 우선 전략, 멱등성 처리, 트랜잭션 분리, 성능 최적화

## 아키텍처 개요

### 1. Call Domain (Event Sourcing)
- **Event Store**: `CallEventStore` - 모든 통화 이벤트 영구 저장 (append-only)
- **Read Model**: `CallView` - 조회 최적화 (REQUESTED → ACTIVE → ENDED)
- **특징**: Payload 우선 전략 (원본 JSON 전체 보존), 시간 여행 가능

### 2. Agent Domain (CQRS + Event Sourcing)
- **Command Model**: `Agent` - 기본 정보 (status 없음)
- **Event Store**: `ExtensionEventStore` - PBX Extension 이벤트 저장
- **Query Model**: `AgentView` - 상태 포함 (AVAILABLE/UNAVAILABLE/BUSY)
- **특징**: Command/Query 분리, 비동기 Projection (@Async + AFTER_COMMIT)

### 3. Participant Domain (관계 추적)
- **목적**: Agent-Call 참여 관계 추적 (느슨한 결합)
- **모델**: `CallParticipant` - JOINED → LEFT 상태 관리
- **특징**: ExtensionEventStore 구독으로 자동 생성/종료, 양방향 조회 지원

### 이벤트 플로우

```
CommandService → EventStore 저장 → ApplicationEvent 발행
                                    ↓
                        ProjectionHandler 구독 (@Async)
                                    ↓
                        Read Model 업데이트 (독립 트랜잭션)
```

**다중 구독 예시**: `ExtensionEventStore` → `AgentProjectionHandler` + `ParticipantProjectionHandler`

## 주요 API

```bash
# Agent 생성
POST /api/agents {"name":"Alice","type":"HUMAN","extensionNumber":"1001"}

# Call 이벤트
POST /api/calls/events/requested {"sessionId":"call-001","source":"customer-1","destination":"2001"}
POST /api/calls/events/connected {"sessionId":"call-001"}
POST /api/calls/events/disconnected {"sessionId":"call-001"}

# Extension 이벤트 (Agent 상태 변경)
POST /api/agents/events/busy {"extensionNumber":"1001","sessionId":"call-001"}
POST /api/agents/events/available {"extensionNumber":"1001","sessionId":"call-001"}

# 조회
GET /api/calls/{sessionId}
GET /api/agents/{id}
GET /api/participants/agent/{agentId}  # Agent별 통화 이력
GET /api/participants/call/{sessionId}  # Call별 Agent 이력
```

**전체 시나리오**: `doc/demo-script.http` 참고

## 핵심 구현 특징

### 1. Payload 우선 전략
Event Store는 원본 JSON을 `payload`에 전체 보존. 다른 필드는 Best-effort 추출.
→ 스키마 변경에도 원본 데이터 유지, Event Replay 가능

### 2. 트랜잭션 분리
`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`로 Command/Query 트랜잭션 분리
→ Read Model 장애가 Event Store에 영향 없음

### 3. 멱등성 처리
Projection Handler에서 중복 이벤트 처리 방지 (`existsById` 체크)

### 4. 성능 최적화
- N+1 쿼리 방지: `findByExtensionNumber()` 직접 쿼리
- ObjectMapper Spring Bean 주입
- 중복 코드 제거: 공통 메서드 추출

### 5. 도메인 독립성
도메인 간 직접 의존 없이 Spring ApplicationEvent로만 통신
→ 느슨한 결합, 확장 용이

## 기술 스택

Spring Boot 3.5.6, Java 21, Spring Data JPA, H2 Database, Lombok, Awaitility

## 프로젝트 구조

```
src/main/java/demo/cqrs/
├── call/        # Event Sourcing
│   ├── event/       # CallEventStore (append-only)
│   ├── view/        # CallView (Read Model)
│   ├── service/     # Command/Query/Projection
│   ├── handler/     # CallProjectionHandler (@Async)
│   └── controller/
├── agent/       # CQRS + Event Sourcing
│   ├── domain/      # Agent (Command Model)
│   ├── event/       # ExtensionEventStore
│   ├── view/        # AgentView (Query Model)
│   ├── service/     # Command/Extension/Query/Projection
│   ├── handler/     # AgentProjectionHandler
│   └── controller/
└── participant/ # 관계 추적
    ├── domain/      # CallParticipant
    ├── service/     # Query/Projection
    ├── handler/     # ParticipantProjectionHandler
    └── controller/
```

## 추가 자료

- **`CLAUDE.md`**: 개발자용 가이드 (아키텍처 상세, 구현 패턴, 코드 작성 원칙)

## 주요 테이블

- `call_event_store`, `extension_event_store`: Event Store (append-only)
- `call_view`, `agent_view`: Read Model (조회 최적화)
- `agent`: Command Model
- `call_participant`: Agent-Call 관계 추적
