package com.jun_bank.transaction_service.domain.transaction.domain.model;

import com.jun_bank.transaction_service.domain.transaction.domain.exception.TransactionException;
import com.jun_bank.transaction_service.domain.transaction.domain.model.vo.IdempotencyKey;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 멱등성 레코드 도메인 모델
 * <p>
 * 멱등성 키와 해당 요청/응답을 저장합니다.
 * 동일한 키로 재요청 시 저장된 응답을 반환하여 중복 처리를 방지합니다.
 *
 * <h3>사용 흐름:</h3>
 * <ol>
 *   <li>요청 수신 시 멱등성 키로 레코드 조회</li>
 *   <li>존재하면 저장된 응답 반환</li>
 *   <li>없으면 요청 처리 후 결과 저장</li>
 * </ol>
 *
 * <h3>TTL (Time To Live):</h3>
 * <p>
 * 레코드는 일정 시간 후 만료됩니다.
 * 만료된 레코드는 조회되지 않으며, 같은 키로 새 요청이 처리됩니다.
 * </p>
 *
 * <h3>요청 해시:</h3>
 * <p>
 * 같은 멱등성 키로 다른 내용의 요청이 들어오면 충돌로 처리합니다.
 * requestHash를 통해 요청 내용이 동일한지 확인합니다.
 * </p>
 *
 * @see IdempotencyKey
 */
@Getter
public class IdempotencyRecord {

    /**
     * 멱등성 키 (Primary Key)
     */
    private IdempotencyKey idempotencyKey;

    /**
     * 요청 내용 해시
     * <p>요청 본문을 해시하여 저장, 충돌 감지에 사용</p>
     */
    private String requestHash;

    /**
     * 응답 본문 (JSON)
     * <p>성공 시 응답 JSON을 저장</p>
     */
    private String responseBody;

    /**
     * HTTP 상태 코드
     */
    private int httpStatus;

    /**
     * 연관 거래 ID
     * <p>생성된 거래의 ID</p>
     */
    private String transactionId;

    /**
     * 처리 상태
     */
    private IdempotencyStatus status;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * private 생성자
     */
    private IdempotencyRecord() {}

    // ========================================
    // 팩토리 메서드
    // ========================================

    /**
     * 처리 중 상태의 레코드 생성
     * <p>
     * 요청 처리 시작 시 호출합니다.
     * 동시 요청을 방지하기 위해 먼저 IN_PROGRESS 상태로 저장합니다.
     * </p>
     *
     * @param idempotencyKey 멱등성 키
     * @param requestHash 요청 해시
     * @param ttlSeconds TTL (초)
     * @return 새 IdempotencyRecord
     */
    public static IdempotencyRecord createInProgress(
            IdempotencyKey idempotencyKey,
            String requestHash,
            long ttlSeconds) {

        IdempotencyRecord record = new IdempotencyRecord();
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.status = IdempotencyStatus.IN_PROGRESS;
        record.createdAt = LocalDateTime.now();
        record.expiresAt = record.createdAt.plusSeconds(ttlSeconds);

        return record;
    }

    /**
     * DB 복원용 빌더
     *
     * @return IdempotencyRecordRestoreBuilder
     */
    public static IdempotencyRecordRestoreBuilder restoreBuilder() {
        return new IdempotencyRecordRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 만료 여부 확인
     *
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 처리 중 여부 확인
     *
     * @return IN_PROGRESS이면 true
     */
    public boolean isInProgress() {
        return this.status == IdempotencyStatus.IN_PROGRESS;
    }

    /**
     * 완료 여부 확인
     *
     * @return COMPLETED이면 true
     */
    public boolean isCompleted() {
        return this.status == IdempotencyStatus.COMPLETED;
    }

    /**
     * 유효한 레코드인지 확인
     * <p>
     * 완료 상태이고 만료되지 않은 경우 유효합니다.
     * </p>
     *
     * @return 유효하면 true
     */
    public boolean isValid() {
        return isCompleted() && !isExpired();
    }

    /**
     * 요청 해시 일치 여부 확인
     * <p>
     * 같은 키로 다른 요청이 들어왔는지 확인합니다.
     * </p>
     *
     * @param otherRequestHash 비교할 요청 해시
     * @return 일치하면 true
     */
    public boolean matchesRequest(String otherRequestHash) {
        return this.requestHash.equals(otherRequestHash);
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 처리 완료
     * <p>
     * 요청 처리가 성공적으로 완료된 경우 호출합니다.
     * </p>
     *
     * @param responseBody 응답 JSON
     * @param httpStatus HTTP 상태 코드
     * @param transactionId 생성된 거래 ID
     */
    public void complete(String responseBody, int httpStatus, String transactionId) {
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.transactionId = transactionId;
        this.status = IdempotencyStatus.COMPLETED;
    }

    /**
     * 처리 실패
     * <p>
     * 요청 처리 중 오류가 발생한 경우 호출합니다.
     * 실패한 레코드는 재시도를 허용하기 위해 삭제하거나 FAILED 상태로 변경합니다.
     * </p>
     *
     * @param responseBody 에러 응답 JSON
     * @param httpStatus HTTP 상태 코드
     */
    public void fail(String responseBody, int httpStatus) {
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.status = IdempotencyStatus.FAILED;
    }

    /**
     * 요청 충돌 검증
     * <p>
     * 같은 키로 다른 요청이 들어온 경우 예외를 발생시킵니다.
     * </p>
     *
     * @param otherRequestHash 비교할 요청 해시
     * @throws TransactionException 요청 해시가 다른 경우
     */
    public void validateRequestMatch(String otherRequestHash) {
        if (!matchesRequest(otherRequestHash)) {
            throw TransactionException.idempotencyKeyConflict(this.idempotencyKey.value());
        }
    }

    /**
     * 만료 검증
     *
     * @throws TransactionException 만료된 경우
     */
    public void validateNotExpired() {
        if (isExpired()) {
            throw TransactionException.idempotencyKeyExpired(this.idempotencyKey.value());
        }
    }

    /**
     * 처리 중 상태 검증
     *
     * @throws TransactionException 처리 중인 경우
     */
    public void validateNotInProgress() {
        if (isInProgress() && !isExpired()) {
            throw TransactionException.idempotencyKeyInProgress(this.idempotencyKey.value());
        }
    }

    // ========================================
    // 멱등성 상태 Enum
    // ========================================

    /**
     * 멱등성 레코드 상태
     */
    public enum IdempotencyStatus {
        /**
         * 처리 중
         * <p>요청이 처리 중이며 아직 완료되지 않음</p>
         */
        IN_PROGRESS,

        /**
         * 완료
         * <p>요청 처리가 성공적으로 완료됨</p>
         */
        COMPLETED,

        /**
         * 실패
         * <p>요청 처리 중 오류 발생</p>
         */
        FAILED
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * DB 복원용 빌더
     */
    public static class IdempotencyRecordRestoreBuilder {
        private IdempotencyKey idempotencyKey;
        private String requestHash;
        private String responseBody;
        private int httpStatus;
        private String transactionId;
        private IdempotencyStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public IdempotencyRecordRestoreBuilder idempotencyKey(IdempotencyKey idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public IdempotencyRecordRestoreBuilder requestHash(String requestHash) {
            this.requestHash = requestHash;
            return this;
        }

        public IdempotencyRecordRestoreBuilder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public IdempotencyRecordRestoreBuilder httpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public IdempotencyRecordRestoreBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public IdempotencyRecordRestoreBuilder status(IdempotencyStatus status) {
            this.status = status;
            return this;
        }

        public IdempotencyRecordRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public IdempotencyRecordRestoreBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public IdempotencyRecord build() {
            IdempotencyRecord record = new IdempotencyRecord();
            record.idempotencyKey = this.idempotencyKey;
            record.requestHash = this.requestHash;
            record.responseBody = this.responseBody;
            record.httpStatus = this.httpStatus;
            record.transactionId = this.transactionId;
            record.status = this.status;
            record.createdAt = this.createdAt;
            record.expiresAt = this.expiresAt;
            return record;
        }
    }
}