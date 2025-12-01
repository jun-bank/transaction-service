# Transaction Service

> 입출금 서비스 - 입금, 출금, 거래 내역 관리

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8082 |
| 데이터베이스 | transaction_db (PostgreSQL) |
| 주요 역할 | 입출금 처리 및 거래 내역 관리 |

## 🎯 학습 포인트

### 1. 멱등성 (Idempotency) ⭐ (핵심 학습 주제)

**멱등성이란?**
> 같은 요청을 여러 번 실행해도 결과가 동일한 성질

**왜 필요한가?**
- 네트워크 오류로 클라이언트가 응답을 받지 못한 경우
- 재시도 시 중복 처리 방지 (이중 입금/출금 방지)

```
┌─────────────────────────────────────────────────────────────┐
│                    멱등성 없는 경우 (위험!)                   │
├─────────────────────────────────────────────────────────────┤
│   Client                  Server                            │
│     │  1. 출금 50,000원      │                               │
│     │ ────────────────────> │  2. 처리 완료 (잔액 50,000)    │
│     │  3. 응답 ❌ 네트워크 오류                               │
│     │  4. 재시도!            │  5. 또 처리됨!! (잔액 0) 💥   │
│   ❌ 50,000원이 두 번 빠짐!                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    멱등성 있는 경우 (안전!)                   │
├─────────────────────────────────────────────────────────────┤
│   Client                  Server                            │
│     │  1. 출금 (Key: abc)    │  2. Key 신규 → 처리 + 저장    │
│     │  3. 응답 ❌ 오류        │                               │
│     │  4. 재시도 (Key: abc)  │  5. Key 존재 → 저장된 결과 ✓  │
│   ✅ 결과는 동일! (한 번만 처리됨)                            │
└─────────────────────────────────────────────────────────────┘
```

### 2. IdempotencyRecord 도메인 모델로 구현

```
┌─────────────────────────────────────────────────────────────┐
│                    멱등성 처리 흐름                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 요청 수신 (X-Idempotency-Key 헤더)                     │
│             │                                               │
│             ▼                                               │
│   2. IdempotencyRecord 조회                                 │
│             │                                               │
│      ┌──────┴──────┐                                        │
│      │             │                                        │
│   없음           있음                                        │
│      │             │                                        │
│      ▼             ▼                                        │
│   3. IN_PROGRESS  4. 상태 확인                              │
│      로 저장          │                                     │
│      │          ┌────┴────┐                                │
│      │          │         │                                │
│      │     COMPLETED  IN_PROGRESS                          │
│      │          │         │                                │
│      │          ▼         ▼                                │
│      │     저장된 응답   충돌 에러                           │
│      │       반환         (409)                             │
│      │                                                      │
│      ▼                                                      │
│   5. 비즈니스 로직 실행                                      │
│             │                                               │
│      ┌──────┴──────┐                                        │
│      │             │                                        │
│    성공          실패                                        │
│      │             │                                        │
│      ▼             ▼                                        │
│   COMPLETED     FAILED                                      │
│   + 응답 저장   + 에러 저장                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ 도메인 모델

### 도메인 구조
```
domain/transaction/domain/
├── exception/
│   ├── TransactionErrorCode.java    # 에러 코드 정의
│   └── TransactionException.java    # 도메인 예외
└── model/
    ├── Transaction.java             # 거래 Aggregate Root
    ├── IdempotencyRecord.java       # 멱등성 레코드
    ├── TransactionType.java         # 유형 Enum
    ├── TransactionStatus.java       # 상태 Enum
    └── vo/
        ├── TransactionId.java       # TXN-xxxxxxxx
        ├── IdempotencyKey.java      # 멱등성 키 (클라이언트 제공)
        └── Money.java               # 금액 VO
```

### Transaction 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                       Transaction                            │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ transactionId: TransactionId (PK, TXN-xxxxxxxx)             │
│ accountId: String (계좌, ACC-xxx)                           │
│ type: TransactionType (DEPOSIT/WITHDRAWAL/TRANSFER_IN...)   │
│ amount: Money (거래 금액)                                    │
│ balanceAfter: Money (거래 후 잔액, 완료 시 설정)             │
│ status: TransactionStatus (PENDING/SUCCESS/FAILED/CANCELLED)│
│ description: String (거래 설명)                             │
│ idempotencyKey: IdempotencyKey (멱등성 키)                  │
│ referenceTransactionId: String (환불 시 원거래 ID)          │
│ failReason: String (실패 사유)                              │
│ cancelReason: String (취소 사유)                            │
│ processedAt: LocalDateTime (처리 완료 시간)                 │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드 - BaseEntity】                                    │
│ createdAt, updatedAt, createdBy, updatedBy                  │
│ deletedAt, deletedBy, isDeleted (Soft Delete)               │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + complete(Money balanceAfter): void  // 성공 처리           │
│ + fail(String reason): void           // 실패 처리           │
│ + cancel(String reason): void         // 취소 처리           │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isPending(), isSuccess(), isFailed(), isCancelled()       │
│ + isFinal(), canCancel()                                    │
│ + isCredit(), isDebit()                                     │
└─────────────────────────────────────────────────────────────┘
```

### IdempotencyRecord 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                    IdempotencyRecord                         │
├─────────────────────────────────────────────────────────────┤
│ 【필드】                                                      │
│ idempotencyKey: IdempotencyKey (PK, 클라이언트 제공)        │
│ requestHash: String (요청 해시, 충돌 감지용)                │
│ responseBody: String (응답 JSON)                            │
│ httpStatus: int                                             │
│ transactionId: String (생성된 거래 ID)                      │
│ status: IdempotencyStatus (IN_PROGRESS/COMPLETED/FAILED)    │
│ createdAt, expiresAt: LocalDateTime (TTL 관리)              │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + complete(responseBody, httpStatus, transactionId)         │
│ + fail(responseBody, httpStatus)                            │
│ + validateRequestMatch(requestHash)  // 충돌 검증           │
│ + validateNotExpired()               // 만료 검증           │
│ + validateNotInProgress()            // 동시 요청 방지      │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isExpired(), isInProgress(), isCompleted(), isValid()     │
│ + matchesRequest(requestHash)                               │
└─────────────────────────────────────────────────────────────┘
```

### TransactionType Enum (거래 유형)
```java
public enum TransactionType {
    DEPOSIT("입금", credit=true, code="DEP"),
    WITHDRAWAL("출금", credit=false, code="WDR"),
    TRANSFER_IN("이체입금", credit=true, code="TRI"),
    TRANSFER_OUT("이체출금", credit=false, code="TRO"),
    PAYMENT("결제", credit=false, code="PAY"),
    REFUND("환불", credit=true, code="RFD"),
    INTEREST("이자", credit=true, code="INT"),
    FEE("수수료", credit=false, code="FEE");
    
    // 정책 메서드
    public boolean isCredit();   // 입금(잔액+)
    public boolean isDebit();    // 출금(잔액-)
    public boolean isTransfer();
    public boolean isCancellable();
    public boolean isUserInitiated();
}
```

### TransactionStatus Enum (상태 정책)
```java
public enum TransactionStatus {
    PENDING("처리중", final=false, cancellable=true),
    SUCCESS("성공", final=true, cancellable=false),
    FAILED("실패", final=true, cancellable=false),
    CANCELLED("취소", final=true, cancellable=false);
    
    // 정책 메서드
    public boolean isFinal();
    public boolean isCancellable();
    public boolean canTransitionTo(target);
}
```

**상태 전이 규칙:**
```
PENDING → SUCCESS (처리 성공)
        → FAILED (처리 실패)
        → CANCELLED (사용자/관리자 취소)
SUCCESS, FAILED, CANCELLED → (최종 상태, 전이 불가)
```

### Value Objects

#### IdempotencyKey (멱등성 키)
```java
public record IdempotencyKey(String value) {
    // 클라이언트가 생성하여 X-Idempotency-Key 헤더로 전달
    // 최소 8자, 최대 128자
    // 영문자, 숫자, 하이픈, 언더스코어 허용
    // UUID 형식 권장
    
    public static final long DEFAULT_TTL_SECONDS = 86400;  // 24시간
    
    public static IdempotencyKey fromHeader(String headerValue);
    public static boolean isValid(String value);
}
```

### Exception 체계

#### TransactionErrorCode
```java
public enum TransactionErrorCode implements ErrorCode {
    // 유효성 (400)
    INVALID_TRANSACTION_ID_FORMAT, INVALID_AMOUNT,
    IDEMPOTENCY_KEY_REQUIRED, INVALID_IDEMPOTENCY_KEY_FORMAT,
    
    // 조회 (404)
    TRANSACTION_NOT_FOUND,
    
    // 잔액/한도 (400)
    INSUFFICIENT_BALANCE, DAILY_LIMIT_EXCEEDED,
    
    // 상태 (422)
    TRANSACTION_ALREADY_PROCESSED, TRANSACTION_ALREADY_CANCELLED,
    CANNOT_CANCEL_TRANSACTION, INVALID_STATUS_TRANSITION,
    
    // 멱등성 (409)
    IDEMPOTENCY_KEY_CONFLICT, IDEMPOTENCY_KEY_EXPIRED,
    IDEMPOTENCY_KEY_IN_PROGRESS,
    
    // 계좌 (400)
    ACCOUNT_NOT_FOUND, ACCOUNT_NOT_ACTIVE;
}
```

#### TransactionException (팩토리 메서드)
```java
public class TransactionException extends BusinessException {
    public static TransactionException transactionNotFound(String transactionId);
    public static TransactionException insufficientBalance(BigDecimal current, BigDecimal requested);
    public static TransactionException idempotencyKeyConflict(String key);
    public static TransactionException idempotencyKeyInProgress(String key);
    // ...
}
```

---

## 📡 API 명세

### 1. 입금
```http
POST /api/v1/transactions/deposit
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
X-Idempotency-Key: deposit-uuid-12345  ← 필수!
Content-Type: application/json

{
  "accountNumber": "110-1234-5678-90",
  "amount": 100000,
  "description": "급여 입금"
}
```

**Response (201 Created)**
```json
{
  "transactionId": "TXN-a1b2c3d4",
  "accountId": "ACC-12345678",
  "type": "DEPOSIT",
  "amount": 100000,
  "balanceAfter": 250000,
  "status": "SUCCESS",
  "description": "급여 입금",
  "processedAt": "2024-01-15T10:30:00"
}
```

**멱등성 처리:**
- 동일한 `X-Idempotency-Key`로 재요청 시 저장된 응답 반환
- 24시간 후 키 만료

### 2. 출금
```http
POST /api/v1/transactions/withdrawal
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
X-Idempotency-Key: withdrawal-uuid-67890  ← 필수!
Content-Type: application/json

{
  "accountNumber": "110-1234-5678-90",
  "amount": 50000,
  "description": "ATM 출금"
}
```

**Response (201 Created)**
```json
{
  "transactionId": "TXN-e5f6g7h8",
  "accountId": "ACC-12345678",
  "type": "WITHDRAWAL",
  "amount": 50000,
  "balanceAfter": 200000,
  "status": "SUCCESS",
  "description": "ATM 출금",
  "processedAt": "2024-01-15T11:00:00"
}
```

**도메인 검증:**
- `balance >= amount` 확인 (잔액 부족 → 400)
- `dailyUsed + amount <= dailyLimit` 확인 (한도 초과 → 400)

### 3. 거래 내역 조회
```http
GET /api/v1/transactions?accountNumber=110-1234-5678-90&page=0&size=20
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "transactions": [
    {
      "transactionId": "TXN-a1b2c3d4",
      "type": "DEPOSIT",
      "amount": 100000,
      "balanceAfter": 250000,
      "status": "SUCCESS",
      "description": "급여 입금",
      "processedAt": "2024-01-15T10:30:00"
    },
    {
      "transactionId": "TXN-e5f6g7h8",
      "type": "WITHDRAWAL",
      "amount": 50000,
      "balanceAfter": 200000,
      "status": "SUCCESS",
      "description": "ATM 출금",
      "processedAt": "2024-01-15T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100
}
```

### 4. 거래 취소
```http
POST /api/v1/transactions/{transactionId}/cancel
X-User-Id: USR-a1b2c3d4
X-User-Role: USER

{
  "reason": "고객 요청 취소"
}
```

**Response (200 OK)**
```json
{
  "transactionId": "TXN-e5f6g7h8",
  "status": "CANCELLED",
  "cancelReason": "고객 요청 취소",
  "cancelledAt": "2024-01-15T11:30:00"
}
```

---

## 📂 패키지 구조

```
com.jun_bank.transaction_service
├── TransactionServiceApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/                          # 설정 클래스
│   │   ├── JpaConfig.java               # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java          # QueryDSL JPAQueryFactory 빈
│   │   ├── KafkaProducerConfig.java     # Kafka Producer (멱등성, JacksonJsonSerializer)
│   │   ├── KafkaConsumerConfig.java     # Kafka Consumer (수동 ACK, JacksonJsonDeserializer)
│   │   ├── SecurityConfig.java          # Spring Security (헤더 기반 인증)
│   │   ├── FeignConfig.java             # Feign Client 설정
│   │   ├── SwaggerConfig.java           # OpenAPI 문서화
│   │   └── AsyncConfig.java             # 비동기 처리 (ThreadPoolTaskExecutor)
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java          # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java    # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java           # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java # Gateway 헤더 인증 필터
│   │   └── SecurityContextUtil.java     # SecurityContext 유틸리티
│   ├── feign/
│   │   ├── FeignErrorDecoder.java       # Feign 에러 → BusinessException 변환
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java           # 요청/응답 로깅 AOP
└── domain/
    └── transaction/                     # Transaction Bounded Context
        ├── domain/                      # 순수 도메인 ★ 구현 완료
        │   ├── exception/
        │   │   ├── TransactionErrorCode.java
        │   │   └── TransactionException.java
        │   └── model/
        │       ├── Transaction.java          # Aggregate Root
        │       ├── IdempotencyRecord.java    # 멱등성 레코드
        │       ├── TransactionType.java      # 유형 (정책)
        │       ├── TransactionStatus.java    # 상태 (정책)
        │       └── vo/
        │           ├── TransactionId.java
        │           ├── IdempotencyKey.java
        │           └── Money.java
        ├── application/                 # 유스케이스 (TODO)
        │   ├── port/
        │   │   ├── in/
        │   │   └── out/
        │   ├── service/
        │   ├── dto/
        │   └── idempotency/             # 멱등성 AOP
        │       ├── Idempotent.java      # 어노테이션
        │       └── IdempotencyAspect.java
        ├── infrastructure/              # Adapter Out (TODO)
        │   ├── persistence/
        │   │   ├── entity/              # JPA Entity
        │   │   ├── repository/
        │   │   └── adapter/
        │   └── kafka/
        └── presentation/                # Adapter In (TODO)
            ├── controller/
            └── dto/
```

---

## 🔧 Global 레이어 상세

### Config 설정

| 클래스 | 설명 |
|--------|------|
| `JpaConfig` | JPA Auditing 활성화 (`@EnableJpaAuditing`) |
| `QueryDslConfig` | `JPAQueryFactory` 빈 등록 |
| `KafkaProducerConfig` | 멱등성 Producer (ENABLE_IDEMPOTENCE=true, ACKS=all) |
| `KafkaConsumerConfig` | 수동 ACK (MANUAL_IMMEDIATE), group-id: transaction-service-group |
| `SecurityConfig` | Stateless 세션, 헤더 기반 인증, CSRF 비활성화 |
| `FeignConfig` | 로깅 레벨 BASIC, 에러 디코더, 요청 인터셉터 |
| `SwaggerConfig` | OpenAPI 3.0 문서화 설정 |
| `AsyncConfig` | ThreadPoolTaskExecutor (core=5, max=10, queue=25) |

### Security 설정

| 클래스 | 설명 |
|--------|------|
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`, `X-User-Email` 헤더 → SecurityContext |
| `UserPrincipal` | `UserDetails` 구현체, 인증된 사용자 정보 |
| `SecurityContextUtil` | 현재 사용자 조회 유틸리티 |

### BaseEntity (Soft Delete 지원)

```java
@MappedSuperclass
public abstract class BaseEntity {
    private LocalDateTime createdAt;      // 생성일시 (자동)
    private LocalDateTime updatedAt;      // 수정일시 (자동)
    private String createdBy;             // 생성자 (자동)
    private String updatedBy;             // 수정자 (자동)
    private LocalDateTime deletedAt;      // 삭제일시
    private String deletedBy;             // 삭제자
    private Boolean isDeleted = false;    // 삭제 여부
    
    public void delete(String deletedBy);  // Soft Delete
    public void restore();                 // 복구
}
```

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer)
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| DEPOSIT_COMPLETED | transaction.deposit.completed | Ledger | 입금 완료 기록 |
| WITHDRAWAL_COMPLETED | transaction.withdrawal.completed | Ledger | 출금 완료 기록 |
| TRANSACTION_FAILED | transaction.failed | Ledger | 거래 실패 기록 |
| TRANSACTION_CANCELLED | transaction.cancelled | Ledger | 거래 취소 기록 |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| Account Service | 잔액 조회/변경 | 입출금 처리 |

---

## 🧪 테스트 시나리오

### 1. 멱등성 테스트 - 동일 키 재요청
```java
@Test
void 동일_키로_재요청시_저장된_응답_반환() {
    // Given: 첫 번째 입금 요청
    String idempotencyKey = "test-key-12345";
    
    // When: 같은 키로 재요청
    
    // Then:
    // 1. 두 번째 요청에도 동일한 응답 반환
    // 2. 잔액은 한 번만 증가
    // 3. 거래 기록도 1건만 생성
}
```

### 2. 멱등성 테스트 - 키 충돌
```java
@Test
void 같은_키_다른_요청시_충돌_에러() {
    // Given: 첫 번째 요청 (50,000원 입금)
    String idempotencyKey = "test-key-12345";
    
    // When: 같은 키로 다른 요청 (100,000원 입금)
    
    // Then: IDEMPOTENCY_KEY_CONFLICT (409) 에러
}
```

### 3. 동시 요청 테스트
```java
@Test
void 동시_요청시_하나만_처리() {
    // Given: 같은 멱등성 키로 동시에 2개 요청
    
    // When: 동시 실행
    
    // Then:
    // 1. 하나는 정상 처리 (201)
    // 2. 다른 하나는 IN_PROGRESS 에러 (409) 또는 저장된 응답 반환
}
```

### 4. API 테스트
```bash
# 입금 요청
curl -X POST http://localhost:8082/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "X-User-Id: USR-xxx" \
  -H "X-User-Role: USER" \
  -H "X-Idempotency-Key: deposit-12345" \
  -d '{"accountNumber":"110-1234-5678-90","amount":100000,"description":"급여"}'

# 출금 요청
curl -X POST http://localhost:8082/api/v1/transactions/withdrawal \
  -H "Content-Type: application/json" \
  -H "X-User-Id: USR-xxx" \
  -H "X-User-Role: USER" \
  -H "X-Idempotency-Key: withdrawal-67890" \
  -d '{"accountNumber":"110-1234-5678-90","amount":50000}'

# 거래 내역 조회
curl "http://localhost:8082/api/v1/transactions?accountNumber=110-1234-5678-90" \
  -H "X-User-Id: USR-xxx" \
  -H "X-User-Role: USER"
```

---

## 📝 구현 체크리스트

### Domain Layer ✅
- [x] TransactionErrorCode
- [x] TransactionException
- [x] TransactionType (정책 메서드)
- [x] TransactionStatus (정책 메서드)
- [x] TransactionId VO
- [x] IdempotencyKey VO
- [x] Money VO
- [x] Transaction (Aggregate Root)
- [x] IdempotencyRecord (멱등성 관리)

### Application Layer
- [ ] DepositUseCase
- [ ] WithdrawUseCase
- [ ] GetTransactionUseCase
- [ ] CancelTransactionUseCase
- [ ] TransactionPort
- [ ] IdempotencyPort
- [ ] @Idempotent 어노테이션
- [ ] IdempotencyAspect
- [ ] DTO 정의

### Infrastructure Layer
- [ ] TransactionEntity
- [ ] IdempotencyRecordEntity
- [ ] JpaRepository
- [ ] TransactionKafkaProducer
- [ ] AccountFeignClient

### Presentation Layer
- [ ] TransactionController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트
- [ ] 멱등성 테스트 (동일 키 재요청)
- [ ] 키 충돌 테스트 (같은 키, 다른 요청)
- [ ] 동시 요청 테스트
- [ ] API 통합 테스트