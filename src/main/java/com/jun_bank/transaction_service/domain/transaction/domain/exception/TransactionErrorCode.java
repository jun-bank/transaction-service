package com.jun_bank.transaction_service.domain.transaction.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;

/**
 * 거래 도메인 에러 코드
 * <p>
 * 입출금 거래 관련 비즈니스 로직에서 발생할 수 있는 모든 에러를 정의합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>TXN_001~009: 유효성 검증 오류 (400)</li>
 *   <li>TXN_010~019: 조회 오류 (404)</li>
 *   <li>TXN_020~029: 잔액/금액 오류 (400)</li>
 *   <li>TXN_030~039: 상태 오류 (422)</li>
 *   <li>TXN_040~049: 멱등성 오류 (409)</li>
 *   <li>TXN_050~059: 계좌 관련 오류 (400)</li>
 * </ul>
 *
 * @see TransactionException
 * @see ErrorCode
 */
public enum TransactionErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 거래 ID 형식
     */
    INVALID_TRANSACTION_ID_FORMAT("TXN_001", "유효하지 않은 거래 ID 형식입니다", 400),

    /**
     * 유효하지 않은 금액
     * <p>금액이 0 이하이거나 음수인 경우 발생합니다.</p>
     */
    INVALID_AMOUNT("TXN_002", "유효하지 않은 금액입니다", 400),

    /**
     * 유효하지 않은 거래 유형
     */
    INVALID_TRANSACTION_TYPE("TXN_003", "유효하지 않은 거래 유형입니다", 400),

    /**
     * 멱등성 키 누락
     * <p>필수 헤더인 X-Idempotency-Key가 없는 경우 발생합니다.</p>
     */
    IDEMPOTENCY_KEY_REQUIRED("TXN_004", "멱등성 키(X-Idempotency-Key)가 필요합니다", 400),

    /**
     * 유효하지 않은 멱등성 키 형식
     */
    INVALID_IDEMPOTENCY_KEY_FORMAT("TXN_005", "유효하지 않은 멱등성 키 형식입니다", 400),

    /**
     * 요청 설명이 너무 김
     */
    DESCRIPTION_TOO_LONG("TXN_006", "거래 설명은 200자를 초과할 수 없습니다", 400),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 거래를 찾을 수 없음
     */
    TRANSACTION_NOT_FOUND("TXN_010", "거래를 찾을 수 없습니다", 404),

    /**
     * 멱등성 레코드를 찾을 수 없음
     */
    IDEMPOTENCY_RECORD_NOT_FOUND("TXN_011", "멱등성 레코드를 찾을 수 없습니다", 404),

    // ========================================
    // 잔액/금액 오류 (400 Bad Request)
    // ========================================

    /**
     * 잔액 부족
     * <p>출금/이체 시 요청 금액이 현재 잔액보다 큰 경우 발생합니다.</p>
     */
    INSUFFICIENT_BALANCE("TXN_020", "잔액이 부족합니다", 400),

    /**
     * 일일 한도 초과
     */
    DAILY_LIMIT_EXCEEDED("TXN_021", "일일 거래 한도를 초과했습니다", 400),

    /**
     * 1회 거래 한도 초과
     */
    SINGLE_TRANSACTION_LIMIT_EXCEEDED("TXN_022", "1회 거래 한도를 초과했습니다", 400),

    /**
     * 최소 금액 미달
     */
    MINIMUM_AMOUNT_NOT_MET("TXN_023", "최소 거래 금액 이상이어야 합니다", 400),

    // ========================================
    // 상태 오류 (422 Unprocessable Entity)
    // ========================================

    /**
     * 이미 처리된 거래
     * <p>취소 요청 시 이미 완료된 거래인 경우 발생합니다.</p>
     */
    TRANSACTION_ALREADY_PROCESSED("TXN_030", "이미 처리된 거래입니다", 422),

    /**
     * 이미 취소된 거래
     */
    TRANSACTION_ALREADY_CANCELLED("TXN_031", "이미 취소된 거래입니다", 422),

    /**
     * 이미 실패한 거래
     */
    TRANSACTION_ALREADY_FAILED("TXN_032", "이미 실패한 거래입니다", 422),

    /**
     * 취소 불가 상태
     * <p>PENDING 상태가 아닌 거래는 취소할 수 없습니다.</p>
     */
    CANNOT_CANCEL_TRANSACTION("TXN_033", "해당 상태에서는 거래를 취소할 수 없습니다", 422),

    /**
     * 허용되지 않은 상태 전이
     */
    INVALID_STATUS_TRANSITION("TXN_034", "허용되지 않은 상태 변경입니다", 422),

    /**
     * 입금만 취소 가능
     * <p>출금은 이미 실행되었으므로 취소 불가합니다.</p>
     */
    ONLY_DEPOSIT_CANCELLABLE("TXN_035", "입금 거래만 취소할 수 있습니다", 422),

    // ========================================
    // 멱등성 오류 (409 Conflict)
    // ========================================

    /**
     * 멱등성 키 충돌 (다른 요청 내용)
     * <p>
     * 동일한 멱등성 키로 다른 요청 내용이 들어온 경우 발생합니다.
     * 키는 같지만 금액, 계좌번호 등이 다른 경우입니다.
     * </p>
     */
    IDEMPOTENCY_KEY_CONFLICT("TXN_040", "동일한 멱등성 키로 다른 요청이 이미 처리되었습니다", 409),

    /**
     * 멱등성 키 만료
     */
    IDEMPOTENCY_KEY_EXPIRED("TXN_041", "멱등성 키가 만료되었습니다. 새로운 키로 요청하세요", 409),

    /**
     * 동시 처리 중
     * <p>같은 멱등성 키로 요청이 이미 처리 중인 경우 발생합니다.</p>
     */
    IDEMPOTENCY_KEY_IN_PROGRESS("TXN_042", "동일한 요청이 처리 중입니다. 잠시 후 다시 시도하세요", 409),

    // ========================================
    // 계좌 관련 오류 (400 Bad Request)
    // ========================================

    /**
     * 계좌를 찾을 수 없음
     */
    ACCOUNT_NOT_FOUND("TXN_050", "계좌를 찾을 수 없습니다", 400),

    /**
     * 비활성 계좌
     * <p>휴면, 동결, 해지 상태의 계좌로 거래를 시도한 경우 발생합니다.</p>
     */
    ACCOUNT_NOT_ACTIVE("TXN_051", "비활성 상태의 계좌입니다", 400),

    /**
     * 본인 계좌가 아님
     */
    ACCOUNT_NOT_OWNED("TXN_052", "본인 소유 계좌가 아닙니다", 400),

    /**
     * 입금 불가 계좌
     */
    ACCOUNT_DEPOSIT_NOT_ALLOWED("TXN_053", "입금이 불가한 계좌입니다", 400),

    /**
     * 출금 불가 계좌
     */
    ACCOUNT_WITHDRAWAL_NOT_ALLOWED("TXN_054", "출금이 불가한 계좌입니다", 400);

    private final String code;
    private final String message;
    private final int status;

    TransactionErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}