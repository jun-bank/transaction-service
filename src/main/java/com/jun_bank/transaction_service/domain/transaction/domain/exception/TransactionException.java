package com.jun_bank.transaction_service.domain.transaction.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 거래 도메인 예외
 * <p>
 * 입출금 거래 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * throw TransactionException.transactionNotFound("TXN-a1b2c3d4");
 * throw TransactionException.insufficientBalance(currentBalance, requestedAmount);
 * throw TransactionException.idempotencyKeyConflict("key-123");
 * }</pre>
 *
 * @see TransactionErrorCode
 * @see BusinessException
 */
public class TransactionException extends BusinessException {

    /**
     * 에러 코드로 예외 생성
     *
     * @param errorCode 거래 에러 코드
     */
    public TransactionException(TransactionErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 에러 코드와 상세 메시지로 예외 생성
     *
     * @param errorCode 거래 에러 코드
     * @param detailMessage 상세 메시지
     */
    public TransactionException(TransactionErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    /**
     * 유효하지 않은 거래 ID 형식 예외 생성
     *
     * @param id 유효하지 않은 ID
     * @return TransactionException 인스턴스
     */
    public static TransactionException invalidTransactionIdFormat(String id) {
        return new TransactionException(TransactionErrorCode.INVALID_TRANSACTION_ID_FORMAT,
                "id=" + id);
    }

    /**
     * 유효하지 않은 금액 예외 생성
     *
     * @param amount 유효하지 않은 금액
     * @return TransactionException 인스턴스
     */
    public static TransactionException invalidAmount(BigDecimal amount) {
        return new TransactionException(TransactionErrorCode.INVALID_AMOUNT,
                "amount=" + (amount != null ? amount.toPlainString() : "null"));
    }

    /**
     * 멱등성 키 누락 예외 생성
     *
     * @return TransactionException 인스턴스
     */
    public static TransactionException idempotencyKeyRequired() {
        return new TransactionException(TransactionErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    /**
     * 유효하지 않은 멱등성 키 형식 예외 생성
     *
     * @param key 유효하지 않은 키
     * @return TransactionException 인스턴스
     */
    public static TransactionException invalidIdempotencyKeyFormat(String key) {
        return new TransactionException(TransactionErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT,
                "key=" + key);
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    /**
     * 거래를 찾을 수 없음 예외 생성
     *
     * @param transactionId 찾을 수 없는 거래 ID
     * @return TransactionException 인스턴스
     */
    public static TransactionException transactionNotFound(String transactionId) {
        return new TransactionException(TransactionErrorCode.TRANSACTION_NOT_FOUND,
                "transactionId=" + transactionId);
    }

    // ========================================
    // 잔액/금액 관련 팩토리 메서드
    // ========================================

    /**
     * 잔액 부족 예외 생성
     *
     * @param currentBalance 현재 잔액
     * @param requestedAmount 요청 금액
     * @return TransactionException 인스턴스
     */
    public static TransactionException insufficientBalance(BigDecimal currentBalance, BigDecimal requestedAmount) {
        return new TransactionException(TransactionErrorCode.INSUFFICIENT_BALANCE,
                String.format("현재잔액=%s, 요청금액=%s",
                        currentBalance.toPlainString(), requestedAmount.toPlainString()));
    }

    /**
     * 일일 한도 초과 예외 생성
     *
     * @param usedAmount 사용한 금액
     * @param limitAmount 한도 금액
     * @return TransactionException 인스턴스
     */
    public static TransactionException dailyLimitExceeded(BigDecimal usedAmount, BigDecimal limitAmount) {
        return new TransactionException(TransactionErrorCode.DAILY_LIMIT_EXCEEDED,
                String.format("사용=%s, 한도=%s", usedAmount.toPlainString(), limitAmount.toPlainString()));
    }

    /**
     * 1회 거래 한도 초과 예외 생성
     *
     * @param amount 요청 금액
     * @param maxAmount 최대 금액
     * @return TransactionException 인스턴스
     */
    public static TransactionException singleTransactionLimitExceeded(BigDecimal amount, BigDecimal maxAmount) {
        return new TransactionException(TransactionErrorCode.SINGLE_TRANSACTION_LIMIT_EXCEEDED,
                String.format("요청=%s, 한도=%s", amount.toPlainString(), maxAmount.toPlainString()));
    }

    // ========================================
    // 상태 관련 팩토리 메서드
    // ========================================

    /**
     * 이미 처리된 거래 예외 생성
     *
     * @param transactionId 거래 ID
     * @param status 현재 상태
     * @return TransactionException 인스턴스
     */
    public static TransactionException transactionAlreadyProcessed(String transactionId, String status) {
        return new TransactionException(TransactionErrorCode.TRANSACTION_ALREADY_PROCESSED,
                String.format("transactionId=%s, status=%s", transactionId, status));
    }

    /**
     * 이미 취소된 거래 예외 생성
     *
     * @param transactionId 거래 ID
     * @return TransactionException 인스턴스
     */
    public static TransactionException transactionAlreadyCancelled(String transactionId) {
        return new TransactionException(TransactionErrorCode.TRANSACTION_ALREADY_CANCELLED,
                "transactionId=" + transactionId);
    }

    /**
     * 이미 실패한 거래 예외 생성
     *
     * @param transactionId 거래 ID
     * @return TransactionException 인스턴스
     */
    public static TransactionException transactionAlreadyFailed(String transactionId) {
        return new TransactionException(TransactionErrorCode.TRANSACTION_ALREADY_FAILED,
                "transactionId=" + transactionId);
    }

    /**
     * 취소 불가 상태 예외 생성
     *
     * @param transactionId 거래 ID
     * @param status 현재 상태
     * @return TransactionException 인스턴스
     */
    public static TransactionException cannotCancelTransaction(String transactionId, String status) {
        return new TransactionException(TransactionErrorCode.CANNOT_CANCEL_TRANSACTION,
                String.format("transactionId=%s, status=%s", transactionId, status));
    }

    /**
     * 허용되지 않은 상태 전이 예외 생성
     *
     * @param from 현재 상태
     * @param to 요청한 상태
     * @return TransactionException 인스턴스
     */
    public static TransactionException invalidStatusTransition(String from, String to) {
        return new TransactionException(TransactionErrorCode.INVALID_STATUS_TRANSITION,
                String.format("from=%s, to=%s", from, to));
    }

    // ========================================
    // 멱등성 관련 팩토리 메서드
    // ========================================

    /**
     * 멱등성 키 충돌 예외 생성
     * <p>
     * 동일한 멱등성 키로 다른 요청 내용이 들어온 경우 발생합니다.
     * </p>
     *
     * @param key 충돌한 멱등성 키
     * @return TransactionException 인스턴스
     */
    public static TransactionException idempotencyKeyConflict(String key) {
        return new TransactionException(TransactionErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                "key=" + key);
    }

    /**
     * 멱등성 키 만료 예외 생성
     *
     * @param key 만료된 키
     * @return TransactionException 인스턴스
     */
    public static TransactionException idempotencyKeyExpired(String key) {
        return new TransactionException(TransactionErrorCode.IDEMPOTENCY_KEY_EXPIRED,
                "key=" + key);
    }

    /**
     * 동시 처리 중 예외 생성
     * <p>
     * 같은 멱등성 키로 요청이 이미 처리 중인 경우 발생합니다.
     * </p>
     *
     * @param key 처리 중인 키
     * @return TransactionException 인스턴스
     */
    public static TransactionException idempotencyKeyInProgress(String key) {
        return new TransactionException(TransactionErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS,
                "key=" + key);
    }

    // ========================================
    // 계좌 관련 팩토리 메서드
    // ========================================

    /**
     * 계좌를 찾을 수 없음 예외 생성
     *
     * @param accountNumber 계좌번호
     * @return TransactionException 인스턴스
     */
    public static TransactionException accountNotFound(String accountNumber) {
        return new TransactionException(TransactionErrorCode.ACCOUNT_NOT_FOUND,
                "accountNumber=" + accountNumber);
    }

    /**
     * 비활성 계좌 예외 생성
     *
     * @param accountNumber 계좌번호
     * @param status 계좌 상태
     * @return TransactionException 인스턴스
     */
    public static TransactionException accountNotActive(String accountNumber, String status) {
        return new TransactionException(TransactionErrorCode.ACCOUNT_NOT_ACTIVE,
                String.format("accountNumber=%s, status=%s", accountNumber, status));
    }

    /**
     * 본인 계좌가 아님 예외 생성
     *
     * @param accountNumber 계좌번호
     * @return TransactionException 인스턴스
     */
    public static TransactionException accountNotOwned(String accountNumber) {
        return new TransactionException(TransactionErrorCode.ACCOUNT_NOT_OWNED,
                "accountNumber=" + accountNumber);
    }
}