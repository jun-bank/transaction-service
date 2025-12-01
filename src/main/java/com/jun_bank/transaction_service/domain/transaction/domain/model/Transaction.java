package com.jun_bank.transaction_service.domain.transaction.domain.model;

import com.jun_bank.transaction_service.domain.transaction.domain.exception.TransactionException;
import com.jun_bank.transaction_service.domain.transaction.domain.model.vo.IdempotencyKey;
import com.jun_bank.transaction_service.domain.transaction.domain.model.vo.Money;
import com.jun_bank.transaction_service.domain.transaction.domain.model.vo.TransactionId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 거래 도메인 모델 (Aggregate Root)
 * <p>
 * 입출금 거래의 핵심 비즈니스 로직을 포함합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>거래 생성 및 상태 관리</li>
 *   <li>거래 완료/실패/취소 처리</li>
 *   <li>멱등성 키 관리</li>
 * </ul>
 *
 * <h3>멱등성 처리:</h3>
 * <p>
 * {@code idempotencyKey}를 통해 중복 요청을 감지합니다.
 * 같은 키로 요청이 들어오면 이전 결과를 반환합니다.
 * </p>
 *
 * <h3>상태 전이:</h3>
 * <pre>
 * PENDING → SUCCESS (성공)
 *         → FAILED (실패)
 *         → CANCELLED (취소)
 * </pre>
 *
 * <h3>감사 필드 (BaseEntity 매핑):</h3>
 * <ul>
 *   <li>createdAt, updatedAt, createdBy, updatedBy</li>
 *   <li>deletedAt, deletedBy, isDeleted (Soft Delete)</li>
 * </ul>
 *
 * @see TransactionType
 * @see TransactionStatus
 */
@Getter
public class Transaction {

    // ========================================
    // 핵심 필드
    // ========================================

    /**
     * 거래 ID
     */
    private TransactionId transactionId;

    /**
     * 계좌 ID (Account Service의 ACC-xxx)
     */
    private String accountId;

    /**
     * 거래 유형
     */
    private TransactionType type;

    /**
     * 거래 금액
     */
    private Money amount;

    /**
     * 거래 후 잔액
     * <p>거래 완료 시 설정됩니다.</p>
     */
    private Money balanceAfter;

    /**
     * 거래 상태
     */
    private TransactionStatus status;

    /**
     * 거래 설명
     */
    private String description;

    /**
     * 멱등성 키
     * <p>클라이언트가 제공한 중복 요청 방지 키</p>
     */
    private IdempotencyKey idempotencyKey;

    /**
     * 참조 거래 ID
     * <p>취소/환불 시 원거래 ID</p>
     */
    private String referenceTransactionId;

    /**
     * 실패 사유
     * <p>FAILED 상태일 때 설정</p>
     */
    private String failReason;

    /**
     * 취소 사유
     * <p>CANCELLED 상태일 때 설정</p>
     */
    private String cancelReason;

    /**
     * 처리 완료 시간
     * <p>SUCCESS, FAILED, CANCELLED 상태로 변경된 시간</p>
     */
    private LocalDateTime processedAt;

    // ========================================
    // 감사 필드 (BaseEntity 매핑)
    // ========================================

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;

    /** 생성자 ID */
    private String createdBy;

    /** 수정자 ID */
    private String updatedBy;

    /** 삭제 일시 (Soft Delete) */
    private LocalDateTime deletedAt;

    /** 삭제자 ID (Soft Delete) */
    private String deletedBy;

    /** 삭제 여부 (Soft Delete) */
    private Boolean isDeleted;

    /**
     * private 생성자
     */
    private Transaction() {}

    // ========================================
    // 생성 메서드 (Builder 패턴)
    // ========================================

    /**
     * 신규 거래 생성 빌더
     * <p>
     * 상태는 PENDING으로 초기화됩니다.
     * </p>
     *
     * @return TransactionCreateBuilder
     */
    public static TransactionCreateBuilder createBuilder() {
        return new TransactionCreateBuilder();
    }

    /**
     * DB 복원용 빌더
     *
     * @return TransactionRestoreBuilder
     */
    public static TransactionRestoreBuilder restoreBuilder() {
        return new TransactionRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    /**
     * 신규 여부 확인
     *
     * @return transactionId가 null이면 true
     */
    public boolean isNew() {
        return this.transactionId == null;
    }

    /**
     * 처리 중 여부 확인
     *
     * @return PENDING이면 true
     */
    public boolean isPending() {
        return this.status.isPending();
    }

    /**
     * 성공 여부 확인
     *
     * @return SUCCESS이면 true
     */
    public boolean isSuccess() {
        return this.status.isSuccess();
    }

    /**
     * 실패 여부 확인
     *
     * @return FAILED이면 true
     */
    public boolean isFailed() {
        return this.status.isFailed();
    }

    /**
     * 취소됨 여부 확인
     *
     * @return CANCELLED이면 true
     */
    public boolean isCancelled() {
        return this.status.isCancelled();
    }

    /**
     * 최종 상태 여부 확인
     *
     * @return 최종 상태이면 true
     */
    public boolean isFinal() {
        return this.status.isFinal();
    }

    /**
     * 취소 가능 여부 확인
     * <p>
     * PENDING 상태이고, 거래 유형이 취소 가능한 경우에만 true.
     * </p>
     *
     * @return 취소 가능하면 true
     */
    public boolean canCancel() {
        return this.status.isCancellable() && this.type.isCancellable();
    }

    /**
     * 입금 거래 여부 확인
     *
     * @return 입금(잔액 증가) 거래이면 true
     */
    public boolean isCredit() {
        return this.type.isCredit();
    }

    /**
     * 출금 거래 여부 확인
     *
     * @return 출금(잔액 감소) 거래이면 true
     */
    public boolean isDebit() {
        return this.type.isDebit();
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 거래 성공 처리
     * <p>
     * 상태를 SUCCESS로 변경하고 거래 후 잔액을 설정합니다.
     * </p>
     *
     * @param balanceAfter 거래 후 잔액
     * @throws TransactionException 이미 최종 상태인 경우
     */
    public void complete(Money balanceAfter) {
        validateNotFinal();
        validateStatusTransition(TransactionStatus.SUCCESS);

        this.status = TransactionStatus.SUCCESS;
        this.balanceAfter = balanceAfter;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 거래 실패 처리
     *
     * @param reason 실패 사유
     * @throws TransactionException 이미 최종 상태인 경우
     */
    public void fail(String reason) {
        validateNotFinal();
        validateStatusTransition(TransactionStatus.FAILED);

        this.status = TransactionStatus.FAILED;
        this.failReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 거래 취소 처리
     * <p>
     * PENDING 상태의 거래만 취소할 수 있습니다.
     * </p>
     *
     * @param reason 취소 사유
     * @throws TransactionException 취소 불가 상태인 경우
     */
    public void cancel(String reason) {
        if (!canCancel()) {
            throw TransactionException.cannotCancelTransaction(
                    this.transactionId != null ? this.transactionId.value() : "NEW",
                    this.status.name());
        }
        validateStatusTransition(TransactionStatus.CANCELLED);

        this.status = TransactionStatus.CANCELLED;
        this.cancelReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    // ========================================
    // Private 검증 메서드
    // ========================================

    /**
     * 최종 상태가 아닌지 검증
     */
    private void validateNotFinal() {
        if (this.status.isFinal()) {
            if (this.status.isSuccess()) {
                throw TransactionException.transactionAlreadyProcessed(
                        this.transactionId.value(), this.status.name());
            } else if (this.status.isCancelled()) {
                throw TransactionException.transactionAlreadyCancelled(
                        this.transactionId.value());
            } else if (this.status.isFailed()) {
                throw TransactionException.transactionAlreadyFailed(
                        this.transactionId.value());
            }
        }
    }

    /**
     * 상태 전이 검증
     */
    private void validateStatusTransition(TransactionStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw TransactionException.invalidStatusTransition(
                    this.status.name(), target.name());
        }
    }

    // ========================================
    // Builder 클래스
    // ========================================

    /**
     * 신규 거래 생성 빌더
     */
    public static class TransactionCreateBuilder {
        private String accountId;
        private TransactionType type;
        private Money amount;
        private String description;
        private IdempotencyKey idempotencyKey;
        private String referenceTransactionId;

        /**
         * 계좌 ID 설정
         *
         * @param accountId ACC-xxx 형식
         * @return this
         */
        public TransactionCreateBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * 거래 유형 설정
         *
         * @param type 거래 유형
         * @return this
         */
        public TransactionCreateBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        /**
         * 금액 설정
         *
         * @param amount 금액
         * @return this
         */
        public TransactionCreateBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        /**
         * 설명 설정
         *
         * @param description 거래 설명
         * @return this
         */
        public TransactionCreateBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 멱등성 키 설정
         *
         * @param idempotencyKey 멱등성 키
         * @return this
         */
        public TransactionCreateBuilder idempotencyKey(IdempotencyKey idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        /**
         * 참조 거래 ID 설정 (환불/취소 시)
         *
         * @param referenceTransactionId 원거래 ID
         * @return this
         */
        public TransactionCreateBuilder referenceTransactionId(String referenceTransactionId) {
            this.referenceTransactionId = referenceTransactionId;
            return this;
        }

        /**
         * Transaction 객체 생성
         *
         * @return 신규 Transaction 객체
         * @throws TransactionException 필수 필드 누락 또는 유효성 오류
         */
        public Transaction build() {
            if (this.accountId == null || this.accountId.isBlank()) {
                throw TransactionException.accountNotFound("null");
            }
            if (this.type == null) {
                throw new IllegalArgumentException("Transaction type is required");
            }
            if (this.amount == null || !this.amount.isPositive()) {
                throw TransactionException.invalidAmount(
                        this.amount != null ? this.amount.amount() : null);
            }

            Transaction transaction = new Transaction();
            transaction.accountId = this.accountId;
            transaction.type = this.type;
            transaction.amount = this.amount;
            transaction.description = this.description;
            transaction.idempotencyKey = this.idempotencyKey;
            transaction.referenceTransactionId = this.referenceTransactionId;
            transaction.status = TransactionStatus.PENDING;
            transaction.isDeleted = false;

            return transaction;
        }
    }

    /**
     * DB 복원용 빌더
     */
    public static class TransactionRestoreBuilder {
        private TransactionId transactionId;
        private String accountId;
        private TransactionType type;
        private Money amount;
        private Money balanceAfter;
        private TransactionStatus status;
        private String description;
        private IdempotencyKey idempotencyKey;
        private String referenceTransactionId;
        private String failReason;
        private String cancelReason;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        public TransactionRestoreBuilder transactionId(TransactionId transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public TransactionRestoreBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public TransactionRestoreBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public TransactionRestoreBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public TransactionRestoreBuilder balanceAfter(Money balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public TransactionRestoreBuilder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public TransactionRestoreBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TransactionRestoreBuilder idempotencyKey(IdempotencyKey idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public TransactionRestoreBuilder referenceTransactionId(String referenceTransactionId) {
            this.referenceTransactionId = referenceTransactionId;
            return this;
        }

        public TransactionRestoreBuilder failReason(String failReason) {
            this.failReason = failReason;
            return this;
        }

        public TransactionRestoreBuilder cancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
            return this;
        }

        public TransactionRestoreBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public TransactionRestoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TransactionRestoreBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public TransactionRestoreBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public TransactionRestoreBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public TransactionRestoreBuilder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public TransactionRestoreBuilder deletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
            return this;
        }

        public TransactionRestoreBuilder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Transaction build() {
            Transaction transaction = new Transaction();
            transaction.transactionId = this.transactionId;
            transaction.accountId = this.accountId;
            transaction.type = this.type;
            transaction.amount = this.amount;
            transaction.balanceAfter = this.balanceAfter;
            transaction.status = this.status;
            transaction.description = this.description;
            transaction.idempotencyKey = this.idempotencyKey;
            transaction.referenceTransactionId = this.referenceTransactionId;
            transaction.failReason = this.failReason;
            transaction.cancelReason = this.cancelReason;
            transaction.processedAt = this.processedAt;
            transaction.createdAt = this.createdAt;
            transaction.updatedAt = this.updatedAt;
            transaction.createdBy = this.createdBy;
            transaction.updatedBy = this.updatedBy;
            transaction.deletedAt = this.deletedAt;
            transaction.deletedBy = this.deletedBy;
            transaction.isDeleted = this.isDeleted;
            return transaction;
        }
    }
}