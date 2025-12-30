# Event Sourcing & CQRS: 요구사항 기반 설계 여정

> **목표**: 요구사항이 발생하고 해결하는 과정을 통해 패턴을 이해하기
> **시간**: 50분 (질의응답 포함)
> **접근**: 니즈 → 설계 고민 → 구현 → 결과

---

## Step 1: 통화 이력을 보고 싶다

### 주어진 조건

**PBX 시스템**으로부터 통화 이벤트를 수신할 수 있다:
```
REQUESTED   : 통화 요청 발생
CONNECTED   : 통화 연결 완료
DISCONNECTED: 통화 종료
```

**요구사항**: 모든 통화 이력을 기록하고 조회할 수 있어야 한다.

---

### 첫 번째 시도: Call 테이블 UPDATE

PBX 이벤트를 받아서 `calls` 테이블을 업데이트하는 방식:

```
┌─────────────┐
│ PBX System  │
└──────┬──────┘
       │ REQUESTED
       ↓
┌─────────────────┐
│ Application     │
│ INSERT calls    │
│ status='REQUESTED'
└─────────────────┘
       │ CONNECTED
       ↓
┌─────────────────┐
│ Application     │
│ UPDATE calls    │
│ status='CONNECTED'
└─────────────────┘
       │ DISCONNECTED
       ↓
┌─────────────────┐
│ Application     │
│ UPDATE calls    │
│ status='ENDED'  │
└─────────────────┘
```

**테이블 구조**:
```sql
calls
├── session_id (PK)
├── status          -- REQUESTED → CONNECTED → ENDED
├── started_at
└── ended_at
```

---

### 문제점

**UPDATE 방식의 본질적 한계**:

```sql
-- 시간 경과
INSERT INTO calls (session_id, status) VALUES ('call-001', 'REQUESTED');
UPDATE calls SET status = 'CONNECTED', started_at = NOW() WHERE session_id = 'call-001';
UPDATE calls SET status = 'ENDED', ended_at = NOW() WHERE session_id = 'call-001';

-- 최종 결과
SELECT * FROM calls WHERE session_id = 'call-001';
```

| session_id | status | started_at | ended_at |
|------------|--------|------------|----------|
| call-001 | ENDED | 10:00:00 | 10:15:30 |

**무엇을 잃었는가?**

```
✅ 알 수 있는 것:
- 통화가 시작되고 종료되었다
- 시작/종료 시각

❌ 알 수 없는 것:
- 언제 REQUESTED 되었나?
- REQUESTED → CONNECTED 얼마나 걸렸나?
- 중간 과정이 있었나?
```

**핵심 문제**:
- 현재 상태(snapshot)만 있고 변화 과정(history)이 없음
- "어떻게(how) 이 상태에 도달했는가?"를 알 수 없음

---

### Event Sourcing 선택

**설계 결정**: UPDATE 대신 INSERT로 모든 변화를 기록

```
기존 (State-Oriented):
  상태 저장 → 상태 덮어쓰기 → 이력 손실

Event Sourcing (Event-Oriented):
  변화 저장 → 변화 누적 → 이력 보존
```

**아키텍처**:
```
┌─────────────┐
│ PBX System  │
└──────┬──────┘
       │ REQUESTED
       ↓
┌──────────────────┐
│ Application      │
│ INSERT event_store
│ (REQUESTED)      │
└──────────────────┘
       │ CONNECTED
       ↓
┌──────────────────┐
│ Application      │
│ INSERT event_store
│ (CONNECTED)      │
└──────────────────┘
       │ DISCONNECTED
       ↓
┌──────────────────┐
│ Application      │
│ INSERT event_store
│ (DISCONNECTED)   │
└──────────────────┘
```

---

### CallEventStore 설계

**테이블 구조**:
```sql
call_event_store (append-only)
├── id              -- AUTO_INCREMENT
├── event_type      -- REQUESTED, CONNECTED, DISCONNECTED
├── session_id
├── occurred_at     -- 이벤트 발생 시각
├── payload         -- 원본 JSON 전체 (핵심!)
├── source          -- Best-effort 추출
└── destination     -- Best-effort 추출
```

**핵심 원칙**:

#### 1. Append-Only
```sql
-- ✅ 허용
INSERT INTO call_event_store (...) VALUES (...);

-- ❌ 금지
UPDATE call_event_store SET ...;
DELETE FROM call_event_store WHERE ...;
```

#### 2. Payload 우선
```java
CallEventStore.builder()
    .eventType(REQUESTED)
    .sessionId("call-001")
    .occurredAt(Instant.now())
    .payload(원본_JSON)  // ← 무조건 성공해야 함
    .source(extractSafely(() -> json.source()))      // ← 실패해도 OK
    .destination(extractSafely(() -> json.destination()))  // ← 실패해도 OK
    .build();
```

**이유**:
- 필드 추출 실패해도 원본(payload)은 보존
- 나중에 필요한 필드 추가 시 payload에서 재추출 가능

---

### 결과

**이력 보존**:
```sql
SELECT event_type, occurred_at, payload
FROM call_event_store
WHERE session_id = 'call-001'
ORDER BY occurred_at;
```

| event_type | occurred_at | payload |
|------------|-------------|---------|
| REQUESTED | 09:59:45 | {"source":"customer-1",...} |
| CONNECTED | 10:00:00 | {"answeredBy":"agent-1",...} |
| DISCONNECTED | 10:15:30 | {"reason":"completed",...} |

```
✅ 알 수 있게 된 것:
- REQUESTED 시각: 09:59:45
- 대기 시간: 15초 (10:00:00 - 09:59:45)
- 통화 시간: 15분 30초
- 중간 과정 전체
```

---

## Step 2: 상태를 빠르게 조회하고 싶다

### 요구사항

```
"현재 통화 중인 건은 몇 개?"
"평균 통화 시간은?"
"ENDED 상태만 필터링"
```

**문제점**: Event Store 조회는 느리다 (전체 스캔, 복잡한 집계)

---

### Read Model 분리

**설계 결정**: 쓰기(이력)와 읽기(상태)를 분리

| 모델 | 목적 | 특성 |
|------|------|------|
| call_event_store | 이력 보존 | append-only, 모든 이벤트 |
| call_view | 빠른 조회 | 현재 상태, 집계 정보 |

**아키텍처**:
```
┌─────────────┐
│ PBX Event   │
└──────┬──────┘
       │
       ↓
┌──────────────────────┐
│ CallCommandService   │
│  1. Event 저장        │
│  2. 이벤트 발행       │
└────┬─────────────────┘
     │
     ├─→ call_event_store (Write)
     │
     └─→ ApplicationEvent
            ↓
      ┌──────────────────────┐
      │ CallProjectionHandler│
      │    (@Async)          │
      └────┬─────────────────┘
           │
           ↓
      call_view (Read)
```

**@Async 선택 이유**:
- Event 저장 트랜잭션과 View 업데이트 분리
- Event 저장 성공이 우선
- View 실패해도 Event Store에서 재구축 가능

---

### CallView 설계

**테이블 구조**:
```sql
call_view
├── session_id (PK)
├── status          -- REQUESTED, ACTIVE, ENDED
├── source
├── destination
├── started_at
├── ended_at
└── duration        -- 미리 계산된 값
```

**특징**:
- 비정규화 (JOIN 없이 조회)
- 집계 값 포함 (duration 미리 계산)
- 단순 쿼리로 빠른 조회

**Projection**: Event 발생 시 CallView 업데이트 (REQUESTED → ACTIVE → ENDED)

---

### 결과: Event Sourcing 패턴 완성

**2개 모델 운영**:

```sql
-- 이력 조회 (Event Store)
SELECT * FROM call_event_store
WHERE session_id = 'call-001'
ORDER BY occurred_at;

-- 현재 상태 조회 (Read Model)
SELECT * FROM call_view WHERE status = 'ACTIVE';

-- 통계 조회
SELECT AVG(duration) FROM call_view WHERE status = 'ENDED';
```

**이점**:
- ✅ 이력 보존 (Event Store)
- ✅ 빠른 조회 (Read Model)
- ✅ 각자 목적에 최적화
- ✅ Event Sourcing 패턴 완성

---

## 📹 데모 1: Event Sourcing (Step 2 완료 후)

**실행**: `doc/demo-script.http` 섹션 1

### 시나리오
1. 통화 요청 (REQUESTED) → call_event_store 저장 → CallView 생성
2. 통화 연결 (CONNECTED) → call_event_store 저장 → CallView 업데이트 (ACTIVE)
3. 통화 종료 (DISCONNECTED) → call_event_store 저장 → CallView 업데이트 (ENDED, duration 계산)

### 확인 포인트
```sql
-- Event Store (3개 이벤트 보존)
SELECT event_type, occurred_at FROM call_event_store
WHERE session_id = 'call-001' ORDER BY occurred_at;

-- Read Model (최종 상태)
SELECT status, duration FROM call_view WHERE session_id = 'call-001';
```

**핵심**: 이력(Event Store) + 빠른 조회(CallView) 분리

---

## Step 3: 통화를 처리한 상담원을 알고 싶다

### 요구사항

```
"이 통화를 누가 처리했지?"
"상담원 이름과 타입(HUMAN/AI)은?"
```

---

### 설계 고민

**옵션 1**: Call에 agentId, agentName 컬럼 추가?
```sql
call_view
├── session_id
├── agent_id
├── agent_name
└── agent_type
```

**문제점**:
- Call이 Agent 정보에 의존
- Agent 정보 변경 시 Call도 업데이트 필요
- 도메인 경계 모호

**옵션 2**: Agent를 별도 도메인으로 분리
```
Call Domain: 통화 정보
Agent Domain: 상담원 정보
```

**✅ 선택**: Agent 도메인 분리

---

### Agent 도메인 추가

**테이블 구조**:
```sql
agent
├── id (PK)
├── name
├── type              -- HUMAN, AI
└── extension_number
```

**특징**:
- 단순 Entity (일단 상태 없이)
- 상담원 기본 정보만
- Call과 독립적

---

### 결과

```
통화 정보: call_view에서 조회
상담원 정보: agent에서 조회
```

**이점**:
- ✅ 도메인 분리 명확
- ✅ Agent 정보 변경 시 Call 영향 없음
- ✅ 각 도메인 독립 발전 가능

---

## Step 4: 상담원 상태를 추적하고 싶다

### 요구사항

```
"Alice가 지금 통화 중인가?"
"현재 대기 중인 상담원은 몇 명?"
"상담원별 통화 횟수는?"
```

---

### 설계 고민

**옵션 1**: Agent에 status 컬럼 추가?
```sql
agent
├── id
├── name
├── type
└── status  -- AVAILABLE, BUSY
```

**문제 분석**:

Command(쓰기) 관점:
```java
// Agent 생성
Agent agent = Agent.create("Alice", HUMAN, "1001");
agentRepository.save(agent);  // 단순, 빠름
```

Query(읽기) 관점:
```sql
-- 복잡한 조회
SELECT a.*,
       COUNT(c.id) as call_count,
       AVG(c.duration) as avg_duration
FROM agent a
LEFT JOIN call_participant cp ON a.id = cp.agent_id
LEFT JOIN call_view c ON cp.session_id = c.session_id
WHERE a.status = 'AVAILABLE'
GROUP BY a.id;
```

**문제점**:
- 쓰기는 단순해야 함 (Agent 정보만)
- 읽기는 복잡함 (상태 + 통계 + JOIN)
- 한 테이블로 두 니즈 충족 불가

**옵션 2**: CQRS - Command/Query 분리
```
Command Model: agent (쓰기 최적화)
Query Model: agent_view (읽기 최적화)
```

**✅ 선택**: CQRS 적용

---

### CQRS 설계

**주어진 조건**: PBX로부터 상담원 상태 이벤트도 수신 가능
```
AVAILABLE   : 상담원 대기 중
UNAVAILABLE : 상담원 이석
BUSY        : 상담원 통화 중
```

**아키텍처**:
```
[1] Agent 생성 (Command)
┌──────────────────┐
│ POST /agents     │
│ Agent 생성        │
└────┬─────────────┘
     │
     ├─→ agent (Command Model)
     │
     └─→ AgentCreated
            ↓
      ┌──────────────────────────┐
      │ AgentProjectionHandler   │
      │ (@Async, AFTER_COMMIT)   │
      └────┬─────────────────────┘
           │
           ↓
      agent_view (status = UNAVAILABLE)


[2] Extension 이벤트 (Event Sourcing)
┌─────────────┐
│ PBX System  │
└──────┬──────┘
       │ AVAILABLE/BUSY
       ↓
┌────────────────────────┐
│ POST /agents/events    │
│  1. Event 저장          │
│  2. 이벤트 발행         │
└────┬───────────────────┘
     │
     ├─→ extension_event_store (append-only)
     │
     └─→ ApplicationEvent
            ↓
      ┌──────────────────────────┐
      │ AgentProjectionHandler   │
      │    (@Async)              │
      └────┬─────────────────────┘
           │
           ↓
      agent_view.status 업데이트
```

**@Async 선택 이유**:
- Extension Event 저장 트랜잭션과 AgentView 업데이트 분리
- Event 저장 성공이 우선
- AgentView 실패해도 Event Store에서 재구축 가능

---

### ExtensionEventStore 설계

**테이블 구조**:
```sql
extension_event_store (append-only)
├── id              -- AUTO_INCREMENT
├── event_type      -- AVAILABLE, UNAVAILABLE, BUSY
├── extension_number
├── session_id      -- 연관된 통화 (BUSY일 때)
├── occurred_at
└── payload         -- 원본 JSON 전체
```

**특징**:
- Call과 동일한 Event Sourcing 패턴
- PBX Extension 이벤트 이력 보존
- Payload 우선 전략

---

### 모델 분리

**Command Model (쓰기)**:
```sql
agent
├── id (PK)
├── name
├── type              -- HUMAN, AI
└── extension_number
-- status 없음! 순수 Agent 정보만
```

**Query Model (읽기)**:
```sql
agent_view
├── id (PK)
├── agent_id          -- Agent FK
├── name
├── type
├── extension_number
└── status            -- AVAILABLE, UNAVAILABLE, BUSY
```

---

### 데이터 흐름

**Agent 생성**:
- AgentCreated 이벤트 → AgentView 생성 (status = UNAVAILABLE)

**상태 업데이트**:
- Extension Event (AVAILABLE/BUSY) → extension_event_store 저장 → AgentView.status 업데이트

---

### 결과: CQRS + Event Sourcing 완성

**3개 모델 운영**:

```sql
-- Event Store (Extension 이력)
SELECT * FROM extension_event_store WHERE extension_number = '1001' ORDER BY occurred_at;

-- Command Model (Agent 정보)
SELECT * FROM agent WHERE id = 1;

-- Query Model (상담원 상태)
SELECT * FROM agent_view WHERE status = 'AVAILABLE';
```

**이점**:
- ✅ Extension 이벤트 이력 보존 (Event Store)
- ✅ Command: 단순 (Agent만)
- ✅ Query: 최적화 (상태 포함)
- ✅ 트랜잭션 분리 (장애 격리)

---

## 📹 데모 2: CQRS + Event Sourcing (Step 4 완료 후)

**실행**: `doc/demo-script.http` 섹션 2

### 시나리오
1. Agent 생성 (Alice) → agent 저장 → AgentView 생성 (status = UNAVAILABLE)
2. Extension Event (BUSY) → extension_event_store 저장 → AgentView.status = BUSY
3. Extension Event (AVAILABLE) → extension_event_store 저장 → AgentView.status = AVAILABLE

### 확인 포인트
```sql
-- Command Model (Agent 정보만)
SELECT name, type, extension_number FROM agent WHERE id = 1;

-- Event Store (Extension 이력)
SELECT event_type, occurred_at FROM extension_event_store
WHERE extension_number = '1001' ORDER BY occurred_at;

-- Query Model (상태 포함)
SELECT name, status FROM agent_view WHERE agent_id = 1;
```

**핵심**: Command/Query 분리 + Extension 이벤트 소싱

---

## Step 5: 받은 이벤트로 서비스를 확장하고 싶다

### 요구사항

지금까지 2종류 이벤트를 받았다:
```
1. PBX Call 이벤트: REQUESTED, CONNECTED, DISCONNECTED
2. PBX Extension 이벤트: AVAILABLE, BUSY
```

**새로운 니즈**:
```
"Agent BUSY 이벤트가 발생하면, 어떤 통화를 처리 중인지 알고 싶다"
"통화 종료 후, 누가 처리했는지 이력을 조회하고 싶다"
```

→ Agent와 Call을 연결하는 관계 추적 필요

---

### 설계 고민

**옵션 1**: Agent가 Call을 직접 참조?
```java
@Entity
public class AgentView {
    @OneToMany
    private List<Call> calls;  // ❌
}
```

**문제점**:
- Agent가 Call에 의존 (강결합)
- 순환 참조 위험
- Call 변경 시 Agent 영향

**옵션 2**: Call이 Agent를 직접 참조?
```java
@Entity
public class CallView {
    @ManyToOne
    private Agent agent;  // ❌
}
```

**문제점**:
- Call이 Agent에 의존 (강결합)
- Agent 장애 시 Call도 영향

**옵션 3**: 관계 전담 도메인 추가
```
Agent Domain: 상담원 정보
Call Domain: 통화 정보
Participant Domain: Agent-Call 관계 (새로 추가!)
```

**✅ 선택**: Participant 도메인 추가

---

### Participant 도메인 설계

**목적**: Agent와 Call 간의 참여 관계만 추적

**테이블 구조**:
```sql
call_participant
├── id (PK)
├── agent_id        -- Agent FK
├── session_id      -- Call FK
├── status          -- JOINED, LEFT
├── joined_at
└── left_at
```

**특징**:
- Agent도 Call도 서로를 모름
- Participant만 둘을 알고 관계 추적
- 양방향 조회 가능

---

### Extension 이벤트 기반 자동화

**아키텍처**:
```
┌─────────────┐
│ PBX System  │
└──────┬──────┘
       │ Extension Event (AVAILABLE/BUSY)
       ↓
┌────────────────────────┐
│ POST /agents/events    │
│  1. Event 저장          │
│  2. 이벤트 발행         │
└────┬───────────────────┘
     │
     ├─→ extension_event_store
     │
     └─→ ApplicationEvent
            ↓
       ┌────┴────┐
       │         │
       ↓         ↓
  [Agent]   [Participant]
  AgentView  CallParticipant
  업데이트    생성/종료
```

**이벤트 구독**:
- Agent 도메인: AgentView.status 업데이트
- Participant 도메인: CallParticipant 생성/종료

---

### 결과: 느슨한 결합

```
┌─────────┐         ┌─────────┐
│ Agent   │         │  Call   │
└─────────┘         └─────────┘
     │                   │
     └─── (서로 모름) ───┘
              ↓
     PBX Extension Event
              ↓
      ┌──────────────┐
      │ Participant  │
      │ (관계 전담)  │
      └──────────────┘
```

**양방향 조회**:
```sql
-- Agent → Call
SELECT cp.*, cv.*
FROM call_participant cp
JOIN call_view cv ON cp.session_id = cv.session_id
WHERE cp.agent_id = 1;

-- Call → Agent
SELECT cp.*, av.*
FROM call_participant cp
JOIN agent_view av ON cp.agent_id = av.agent_id
WHERE cp.session_id = 'call-001';
```

**이점**:
- ✅ Agent와 Call 독립적
- ✅ 순환 참조 없음
- ✅ 장애 격리 (Agent 장애 시 Call 영향 없음)
- ✅ 이벤트 기반 자동화 (수동 관리 불필요)

---

## 📹 데모 3: 도메인 통합 (Step 5 완료 후)

**실행**: `doc/demo-script.http` 섹션 3

### 시나리오
1. Agent 생성 (Bob, extension: 2001)
2. Call 요청 (call-002, destination: 2001)
3. Extension Event (BUSY, session: call-002)
   - → extension_event_store 저장
   - → AgentView.status = BUSY
   - → CallParticipant 생성 (agent: Bob, call: call-002, status: JOINED)
4. Call 연결 → call_event_store 저장 → CallView.status = ACTIVE
5. Call 종료 → call_event_store 저장 → CallView.status = ENDED
6. Extension Event (AVAILABLE, session: call-002)
   - → extension_event_store 저장
   - → AgentView.status = AVAILABLE
   - → CallParticipant 종료 (status: LEFT)

### 확인 포인트
```sql
-- Agent → Call 조회
SELECT cp.session_id, cp.joined_at, cv.status, cv.duration
FROM call_participant cp
JOIN call_view cv ON cp.session_id = cv.session_id
WHERE cp.agent_id = 2;

-- Call → Agent 조회
SELECT cp.agent_id, av.name, cp.joined_at, cp.left_at
FROM call_participant cp
JOIN agent_view av ON cp.agent_id = av.agent_id
WHERE cp.session_id = 'call-002';
```

**핵심**: 3개 도메인이 이벤트로만 연결 (느슨한 결합)

---

## 패턴의 효과

### 1. Event Replay: 데이터 복구 능력

**시나리오**: AgentView 테이블이 손상되었다

```sql
-- 현재 상태
SELECT COUNT(*) FROM agent_view;  -- 0 (전부 삭제됨!)

-- Event Store는 그대로
SELECT COUNT(*) FROM extension_event_store;  -- 150개 이벤트 보존됨
```

**복구 방법**:
```
1. extension_event_store에서 모든 이벤트 읽기
2. 이벤트 순서대로 재생(replay)
3. AgentView 재구축 완료
```

**결과**:
- AgentView 완전 복구
- 같은 방식으로 CallView, CallParticipant도 복구 가능
- Event Store가 유일한 신뢰 소스(Source of Truth)

---

### 2. Payload 우선 전략: 이벤트 확장성

**상황**: PBX 시스템이 새 필드를 추가했다

```json
// 기존
{"sessionId": "call-001", "source": "customer-1"}

// 신규
{"sessionId": "call-001", "source": "customer-1", "priority": "high"}
```

**Payload 우선 덕분에**:
```
1. 기존 이벤트: payload에 전체 JSON 보존됨
2. 새 필드 필요 시: payload에서 추출만 하면 됨
3. 서비스 중단 없음: 점진적 확장 가능
```

**독립성**:
- PBX 스키마 변경 ≠ 서비스 장애
- 이벤트 구조 진화 가능
- 과거 데이터 재해석 가능

---

### 3. 도메인 독립성 & 확장성

**현재 구조**:
```
Call Domain       : PBX Call 이벤트만 소싱
Agent Domain      : Agent 생성 + PBX Extension 이벤트 소싱
Participant Domain: Extension 이벤트 구독
```

**독립성 효과**:
- Call 장애 시 Agent는 정상 동작
- Agent 장애 시 Call은 정상 동작
- Participant 추가해도 기존 도메인 영향 없음

**확장성 효과**:
- 새 도메인 추가: 기존 이벤트 구독만으로 가능
- 예: Analytics 도메인 추가 → Call/Extension 이벤트 구독 → 통계 생성
- 기존 코드 수정 불필요

---

## 트레이드오프

### 최종 일관성 (Eventual Consistency)

**현상**:
```java
Agent agent = agentCommandService.create(...);
AgentView view = agentQueryService.findById(agent.getId());  // null 가능 (@Async)
```

**대응**:
- 테스트: Awaitility로 대기
- 프로덕션: 낙관적 UI 업데이트 또는 폴링

---

### 복잡도 증가

| 기존 (UPDATE) | Event Sourcing + CQRS |
|--------------|---------------------|
| 테이블 1개 | Event Store + Command + Query (3개) |
| 코드 단순 | Projection Handler 필요 |
| 이력 없음 ❌ | 이력 보존 ✅ |

**판단 기준**: 이력/복구 능력이 복잡도를 상회하는가?

---

## 질의응답 준비

### 예상 질문 #1: "AgentView 재구축은 얼마나 걸리나요?"
**답변**:
- 이벤트 수에 비례 (1만 건 = 수 초)
- 스냅샷 패턴으로 최적화 가능
- 실시간 서비스는 standby replica 유지

### 예상 질문 #2: "Event Store가 너무 커지지 않나요?"
**답변**:
- 오래된 이벤트 아카이빙 (S3 등)
- 스냅샷 후 이전 이벤트 압축
- 디스크 비용 < 이력 가치

### 예상 질문 #3: "최종 일관성이 문제가 되지 않나요?"
**답변**:
- 많은 비즈니스는 최종 일관성 허용 (예: SNS 좋아요)
- 즉시 일관성 필요 시 동기 처리 선택 가능
- 테스트는 Awaitility로 안정성 확보

### 예상 질문 #4: "왜 Spring ApplicationEvent를 썼나요?"
**답변**:
- 데모 목적: 외부 의존성(Kafka) 제거
- 패턴 학습에 집중
- 프로덕션: Kafka/RabbitMQ 권장 (다른 서비스도 구독)

### 예상 질문 #5: "기존 시스템에 어떻게 적용하나요?"
**답변**:
- Strangler Fig 패턴 (점진적 전환)
- CDC(Change Data Capture)로 기존 DB → 이벤트 변환
- 핵심 도메인부터 시작 (예: 주문, 결제)

---

## 부록: 환경 설정

```bash
# 실행
./gradlew bootRun

# 접속
- API: http://localhost:8090
- H2 Console: http://localhost:8090/h2-console
  - JDBC URL: jdbc:h2:mem:demo-cqrs
  - Username: sa
  - Password: (비워두기)
```

**데모 스크립트**: `doc/demo-script.http` 참고
