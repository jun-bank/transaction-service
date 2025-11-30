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
│                                                             │
│   Client                  Server                            │
│     │                       │                               │
│     │  1. 출금 50,000원      │                               │
│     │ ────────────────────> │  2. 처리 완료                  │
│     │                       │     (잔액: 100,000 → 50,000)   │
│     │  3. 응답 전송          │                               │
│     │ <──────── ❌ 네트워크 오류 (응답 유실)                  │
│     │                       │                               │
│     │  4. 응답 없음...재시도! │                               │
│     │ ────────────────────> │  5. 또 처리됨!!                │
│     │                       │     (잔액: 50,000 → 0)  💥     │
│     │  6. 응답 수신          │                               │
│     │ <──────────────────── │                               │
│     │                       │                               │
│   ❌ 50,000원이 두 번 빠짐!                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    멱등성 있는 경우 (안전!)                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Client                  Server                            │
│     │                       │                               │
│     │  1. 출금 50,000원      │                               │
│     │     (Key: abc-123)    │                               │
│     │ ────────────────────> │  2. Key 확인: 신규            │
│     │                       │     → 처리 완료                │
│     │                       │     → Key 저장 (결과 포함)     │
│     │  3. 응답 전송          │                               │
│     │ <──────── ❌ 네트워크 오류                             │
│     │                       │                               │
│     │  4. 재시도 (같은 Key)  │                               │
│     │     (Key: abc-123)    │                               │
│     │ ────────────────────> │  5. Key 확인: 이미 존재!       │
│     │                       │     → 저장된 결과 반환 ✓       │
│     │  6. 응답 수신          │                               │
│     │ <──────────────────── │                               │
│     │                       │                               │
│   ✅ 결과는 동일! (한 번만 처리됨)                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. Idempotency Key 구현 방법

```java
// 1. 클라이언트가 요청 시 고유 키 생성하여 헤더에 포함
// Header: X-Idempotency-Key: uuid-1234-5678

// 2. 서버에서 키 확인
@Service
public class TransactionService {

    public TransactionResult process(String idempotencyKey, TransactionRequest request) {
        // 키로 이전 처리 결과 조회
        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByKey(idempotencyKey);

        if (existing.isPresent()) {
            // 이미 처리됨 → 저장된 결과 반환
            return existing.get().getResult();
        }

        // 신규 요청 → 처리
        TransactionResult result = processTransaction(request);

        // 결과 저장 (24시간 후 만료)
        idempotencyRepository.save(
                new IdempotencyRecord(idempotencyKey, result, ttl)
        );

        return result;
    }
}
```

### 3. 거래 상태 관리
- PENDING → SUCCESS / FAILED / CANCELLED

---

## 🗄️ 도메인 모델

### Transaction Entity

```
┌─────────────────────────────────────────────┐
│               Transaction                    │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ transactionId: String (UUID, Unique)        │
│ accountId: Long (FK → Account)              │
│ type: TransactionType (DEPOSIT/WITHDRAWAL)  │
│ amount: BigDecimal                          │
│ balanceAfter: BigDecimal (거래 후 잔액)      │
│ status: TransactionStatus                   │
│ description: String                         │
│ idempotencyKey: String (Unique, Nullable)   │
│ createdAt: LocalDateTime                    │
│ processedAt: LocalDateTime                  │
└─────────────────────────────────────────────┘
```

### IdempotencyRecord Entity

```
┌─────────────────────────────────────────────┐
│            IdempotencyRecord                 │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ idempotencyKey: String (Unique)             │
│ requestHash: String (요청 내용 해시)         │
│ responseBody: String (JSON 응답)             │
│ httpStatus: Integer                         │
│ createdAt: LocalDateTime                    │
│ expiresAt: LocalDateTime (TTL)              │
└─────────────────────────────────────────────┘
```

### TransactionType Enum
```java
public enum TransactionType {
    DEPOSIT,      // 입금
    WITHDRAWAL,   // 출금
    TRANSFER_IN,  // 이체 입금
    TRANSFER_OUT, // 이체 출금
    PAYMENT,      // 결제
    REFUND        // 환불
}
```

### TransactionStatus Enum
```java
public enum TransactionStatus {
    PENDING,    // 처리 중
    SUCCESS,    // 성공
    FAILED,     // 실패
    CANCELLED   // 취소
}
```

---

## 📡 API 명세

### 1. 입금
```http
POST /api/v1/transactions/deposit
X-User-Id: 1
X-User-Role: USER
X-Idempotency-Key: deposit-uuid-12345
Content-Type: application/json

{
  "accountNumber": "110-1234-5678-90",
  "amount": 100000,
  "description": "급여 입금"
}
```

**Response (200 OK)**
```json
{
  "transactionId": "txn-uuid-abcd",
  "type": "DEPOSIT",
  "accountNumber": "110-1234-5678-90",
  "amount": 100000,
  "balanceAfter": 250000,
  "status": "SUCCESS",
  "description": "급여 입금",
  "processedAt": "2024-01-15T10:30:00"
}
```

**멱등성 동작**: 같은 `X-Idempotency-Key`로 재요청 시 동일 응답 반환

**이벤트 발행**: `transaction.deposit.completed`

---

### 2. 출금
```http
POST /api/v1/transactions/withdrawal
X-User-Id: 1
X-User-Role: USER
X-Idempotency-Key: withdrawal-uuid-67890
Content-Type: application/json

{
  "accountNumber": "110-1234-5678-90",
  "amount": 50000,
  "description": "ATM 출금"
}
```

**Response (200 OK)**
```json
{
  "transactionId": "txn-uuid-efgh",
  "type": "WITHDRAWAL",
  "accountNumber": "110-1234-5678-90",
  "amount": 50000,
  "balanceAfter": 200000,
  "status": "SUCCESS",
  "description": "ATM 출금",
  "processedAt": "2024-01-15T11:00:00"
}
```

**잔액 부족 시 (400 Bad Request)**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다.",
  "currentBalance": 30000,
  "requestedAmount": 50000
}
```

**이벤트 발행**: `transaction.withdrawal.completed`

---

### 3. 거래 내역 조회 (단건)
```http
GET /api/v1/transactions/{transactionId}
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "transactionId": "txn-uuid-abcd",
  "type": "DEPOSIT",
  "accountNumber": "110-1234-5678-90",
  "amount": 100000,
  "balanceAfter": 250000,
  "status": "SUCCESS",
  "description": "급여 입금",
  "createdAt": "2024-01-15T10:30:00",
  "processedAt": "2024-01-15T10:30:01"
}
```

---

### 4. 거래 내역 목록 조회
```http
GET /api/v1/transactions?accountNumber=110-1234-5678-90&type=DEPOSIT&page=0&size=20
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "content": [
    {
      "transactionId": "txn-uuid-abcd",
      "type": "DEPOSIT",
      "amount": 100000,
      "balanceAfter": 250000,
      "status": "SUCCESS",
      "description": "급여 입금",
      "processedAt": "2024-01-15T10:30:00"
    },
    {
      "transactionId": "txn-uuid-ijkl",
      "type": "DEPOSIT",
      "amount": 50000,
      "balanceAfter": 150000,
      "status": "SUCCESS",
      "description": "용돈",
      "processedAt": "2024-01-14T15:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### 5. 기간별 거래 내역 조회
```http
GET /api/v1/transactions/period?accountNumber=110-1234-5678-90&startDate=2024-01-01&endDate=2024-01-31
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "period": {
    "start": "2024-01-01",
    "end": "2024-01-31"
  },
  "summary": {
    "totalDeposit": 500000,
    "totalWithdrawal": 200000,
    "netChange": 300000,
    "transactionCount": 15
  },
  "transactions": [...]
}
```

---

### 6. 거래 취소 (관리자)
```http
POST /api/v1/transactions/{transactionId}/cancel
X-User-Id: 999
X-User-Role: ADMIN
Content-Type: application/json

{
  "reason": "고객 요청에 의한 취소"
}
```

**Response (200 OK)**
```json
{
  "transactionId": "txn-uuid-abcd",
  "status": "CANCELLED",
  "cancelledAt": "2024-01-15T12:00:00",
  "reason": "고객 요청에 의한 취소"
}
```

---

## 📂 패키지 구조

```
com.jun_bank.transaction_service
├── TransactionServiceApplication.java
├── global/                          # 전역 설정 레이어
│   ├── config/                      # 설정 클래스
│   │   ├── JpaConfig.java           # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java      # QueryDSL JPAQueryFactory 빈
│   │   ├── KafkaProducerConfig.java # Kafka Producer (멱등성, JacksonJsonSerializer)
│   │   ├── KafkaConsumerConfig.java # Kafka Consumer (수동 ACK, JacksonJsonDeserializer)
│   │   ├── SecurityConfig.java      # Spring Security (헤더 기반 인증)
│   │   ├── FeignConfig.java         # Feign Client 설정
│   │   ├── SwaggerConfig.java       # OpenAPI 문서화
│   │   └── AsyncConfig.java         # 비동기 처리 (ThreadPoolTaskExecutor)
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java      # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java       # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java # Gateway 헤더 인증 필터
│   │   └── SecurityContextUtil.java # SecurityContext 유틸리티
│   ├── feign/
│   │   ├── FeignErrorDecoder.java   # Feign 에러 → BusinessException 변환
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java       # 요청/응답 로깅 AOP
└── domain/
    └── transaction/                 # Transaction 도메인
        ├── domain/                  # 순수 도메인 (Entity, VO, Enum)
        ├── application/             # 유스케이스, Port, DTO
        │   └── idempotency/         # 멱등성 처리 (추후 구현)
        │       ├── Idempotent.java
        │       └── IdempotencyAspect.java
        ├── infrastructure/          # Adapter (Out) - Repository, Kafka
        └── presentation/            # Adapter (In) - Controller
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
| DEPOSIT_COMPLETED | transaction.deposit.completed | Ledger | 입금 기록 |
| WITHDRAWAL_COMPLETED | transaction.withdrawal.completed | Ledger | 출금 기록 |
| TRANSACTION_FAILED | transaction.failed | Ledger | 실패 기록 |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| Account Service | 잔액 조회/변경 | 입출금 처리 |

---

## ⚙️ 멱등성 설정

### application.yml
```yaml
transaction-service:
  idempotency-key-ttl: 86400  # 24시간
  idempotency-key-header: X-Idempotency-Key
```

### 커스텀 어노테이션
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String keyHeader() default "X-Idempotency-Key";
    long ttlSeconds() default 86400;
}
```

### AOP 적용
```java
@Aspect
@Component
public class IdempotencyAspect {

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint,
                                   Idempotent idempotent) throws Throwable {
        String key = extractIdempotencyKey();

        // 1. 기존 결과 조회
        Optional<IdempotencyRecord> existing = repository.findByKey(key);
        if (existing.isPresent()) {
            return existing.get().getResponse();
        }

        // 2. 신규 처리
        Object result = joinPoint.proceed();

        // 3. 결과 저장
        repository.save(new IdempotencyRecord(key, result, ttl));

        return result;
    }
}
```

---

## 🧪 테스트 시나리오

### 1. 멱등성 테스트
```java
@Test
void 동일한_멱등성키로_중복_요청시_동일_결과_반환() {
    // Given
    String idempotencyKey = UUID.randomUUID().toString();
    DepositRequest request = new DepositRequest("110-1234-5678-90", 100000);

    // When: 같은 키로 3번 요청
    TransactionResponse result1 = transactionService.deposit(idempotencyKey, request);
    TransactionResponse result2 = transactionService.deposit(idempotencyKey, request);
    TransactionResponse result3 = transactionService.deposit(idempotencyKey, request);

    // Then: 모두 동일한 결과
    assertThat(result1.getTransactionId()).isEqualTo(result2.getTransactionId());
    assertThat(result2.getTransactionId()).isEqualTo(result3.getTransactionId());

    // And: 실제 입금은 한 번만 발생
    Account account = accountRepository.findByAccountNumber("110-1234-5678-90");
    assertThat(account.getBalance()).isEqualTo(initialBalance + 100000);  // 300000이 아닌 100000만 추가
}
```

### 2. API 테스트
```bash
# 입금 (첫 번째 요청)
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -H "X-Idempotency-Key: test-key-123" \
  -d '{"accountNumber":"110-1234-5678-90","amount":100000}'

# 입금 (동일 키로 재요청 - 같은 결과 반환되어야 함)
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -H "X-Idempotency-Key: test-key-123" \
  -d '{"accountNumber":"110-1234-5678-90","amount":100000}'
```

### 3. 멱등성 키 없이 요청 (경고 또는 거부)
```bash
# X-Idempotency-Key 헤더 없이 요청
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -d '{"accountNumber":"110-1234-5678-90","amount":100000}'

# 응답: 400 Bad Request (또는 경고와 함께 처리)
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] TransactionService 구현
- [ ] **IdempotencyService 구현**
- [ ] **Idempotent 어노테이션 생성**
- [ ] **IdempotencyAspect 구현**
- [ ] Controller 구현
- [ ] Kafka Producer 구현
- [ ] Feign Client 구현 (Account Service)
- [ ] **멱등성 테스트 코드**
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)