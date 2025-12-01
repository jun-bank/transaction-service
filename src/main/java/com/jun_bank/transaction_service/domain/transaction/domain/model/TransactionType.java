package com.jun_bank.transaction_service.domain.transaction.domain.model;

/**
 * 거래 유형
 * <p>
 * 거래의 종류를 정의합니다.
 * 각 유형에 따라 잔액 변동 방향이 결정됩니다.
 *
 * <h3>잔액 변동:</h3>
 * <table border="1">
 *   <tr><th>유형</th><th>잔액 변동</th><th>설명</th></tr>
 *   <tr><td>DEPOSIT</td><td>+</td><td>일반 입금</td></tr>
 *   <tr><td>WITHDRAWAL</td><td>-</td><td>일반 출금</td></tr>
 *   <tr><td>TRANSFER_IN</td><td>+</td><td>이체 받음</td></tr>
 *   <tr><td>TRANSFER_OUT</td><td>-</td><td>이체 보냄</td></tr>
 *   <tr><td>PAYMENT</td><td>-</td><td>결제 (카드)</td></tr>
 *   <tr><td>REFUND</td><td>+</td><td>환불</td></tr>
 *   <tr><td>INTEREST</td><td>+</td><td>이자 입금</td></tr>
 *   <tr><td>FEE</td><td>-</td><td>수수료</td></tr>
 * </table>
 *
 * @see Transaction
 */
public enum TransactionType {

    /**
     * 입금
     * <p>ATM, 무통장입금 등 일반 입금</p>
     */
    DEPOSIT("입금", true, "DEP"),

    /**
     * 출금
     * <p>ATM, 창구 등 일반 출금</p>
     */
    WITHDRAWAL("출금", false, "WDR"),

    /**
     * 이체 입금
     * <p>다른 계좌에서 이체받은 경우</p>
     */
    TRANSFER_IN("이체입금", true, "TRI"),

    /**
     * 이체 출금
     * <p>다른 계좌로 이체한 경우</p>
     */
    TRANSFER_OUT("이체출금", false, "TRO"),

    /**
     * 결제
     * <p>카드 결제, PG 결제 등</p>
     */
    PAYMENT("결제", false, "PAY"),

    /**
     * 환불
     * <p>결제 취소에 따른 환불</p>
     */
    REFUND("환불", true, "RFD"),

    /**
     * 이자
     * <p>예금 이자 입금</p>
     */
    INTEREST("이자", true, "INT"),

    /**
     * 수수료
     * <p>계좌 유지비, 이체 수수료 등</p>
     */
    FEE("수수료", false, "FEE");

    private final String description;
    private final boolean isCredit;  // true: 입금(+), false: 출금(-)
    private final String code;       // 짧은 코드 (트랜잭션 ID 생성 등에 활용)

    TransactionType(String description, boolean isCredit, String code) {
        this.description = description;
        this.isCredit = isCredit;
        this.code = code;
    }

    /**
     * 거래 유형 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 입금 여부 확인
     * <p>잔액이 증가하는 거래인지 확인합니다.</p>
     *
     * @return 입금(+)이면 true
     */
    public boolean isCredit() {
        return isCredit;
    }

    /**
     * 출금 여부 확인
     * <p>잔액이 감소하는 거래인지 확인합니다.</p>
     *
     * @return 출금(-)이면 true
     */
    public boolean isDebit() {
        return !isCredit;
    }

    /**
     * 짧은 코드 반환
     *
     * @return 3자리 코드 (예: "DEP", "WDR")
     */
    public String getCode() {
        return code;
    }

    /**
     * 일반 입금 여부 확인
     *
     * @return DEPOSIT이면 true
     */
    public boolean isDeposit() {
        return this == DEPOSIT;
    }

    /**
     * 일반 출금 여부 확인
     *
     * @return WITHDRAWAL이면 true
     */
    public boolean isWithdrawal() {
        return this == WITHDRAWAL;
    }

    /**
     * 이체 관련 거래 여부 확인
     *
     * @return TRANSFER_IN 또는 TRANSFER_OUT이면 true
     */
    public boolean isTransfer() {
        return this == TRANSFER_IN || this == TRANSFER_OUT;
    }

    /**
     * 결제 관련 거래 여부 확인
     *
     * @return PAYMENT 또는 REFUND이면 true
     */
    public boolean isPaymentRelated() {
        return this == PAYMENT || this == REFUND;
    }

    /**
     * 사용자 요청에 의한 거래 여부 확인
     * <p>
     * 이자, 수수료 등 시스템 자동 거래가 아닌 경우 true.
     * </p>
     *
     * @return 사용자 요청 거래이면 true
     */
    public boolean isUserInitiated() {
        return this != INTEREST && this != FEE;
    }

    /**
     * 취소 가능 여부 확인
     * <p>
     * 일반 입금, 결제, 환불은 취소 가능합니다.
     * 이체는 별도 프로세스로 처리합니다.
     * </p>
     *
     * @return 취소 가능하면 true
     */
    public boolean isCancellable() {
        return this == DEPOSIT || this == PAYMENT || this == REFUND;
    }
}