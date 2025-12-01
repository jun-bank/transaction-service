package com.jun_bank.transaction_service.domain.transaction.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 거래 상태
 * <p>
 * 거래의 처리 상태를 정의합니다.
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 *              처리 성공
 * ┌─────────┐ ─────────▶ ┌─────────┐
 * │ PENDING │            │ SUCCESS │  ← 최종 상태
 * └─────────┘            └─────────┘
 *     │ │
 *     │ │ 처리 실패
 *     │ └───────────────▶ ┌────────┐
 *     │                   │ FAILED │  ← 최종 상태
 *     │                   └────────┘
 *     │
 *     │ 사용자/관리자 취소
 *     └─────────────────▶ ┌───────────┐
 *                         │ CANCELLED │  ← 최종 상태
 *                         └───────────┘
 * </pre>
 *
 * <h3>상태별 특성:</h3>
 * <table border="1">
 *   <tr><th>상태</th><th>최종</th><th>취소 가능</th><th>재시도 가능</th></tr>
 *   <tr><td>PENDING</td><td>✗</td><td>✓</td><td>✓</td></tr>
 *   <tr><td>SUCCESS</td><td>✓</td><td>✗</td><td>✗</td></tr>
 *   <tr><td>FAILED</td><td>✓</td><td>✗</td><td>✗</td></tr>
 *   <tr><td>CANCELLED</td><td>✓</td><td>✗</td><td>✗</td></tr>
 * </table>
 *
 * @see Transaction
 */
public enum TransactionStatus {

    /**
     * 처리 중
     * <p>
     * 거래가 생성되어 처리 중인 상태입니다.
     * 이 상태에서만 SUCCESS, FAILED, CANCELLED로 전이 가능합니다.
     * </p>
     */
    PENDING("처리중", false, true, true),

    /**
     * 성공
     * <p>
     * 거래가 정상적으로 완료된 최종 상태입니다.
     * 잔액이 변경되었습니다.
     * </p>
     */
    SUCCESS("성공", true, false, false),

    /**
     * 실패
     * <p>
     * 거래 처리 중 오류가 발생한 최종 상태입니다.
     * 잔액 변경이 롤백되었습니다.
     * </p>
     */
    FAILED("실패", true, false, false),

    /**
     * 취소
     * <p>
     * 사용자 또는 관리자에 의해 취소된 최종 상태입니다.
     * PENDING 상태에서만 취소 가능합니다.
     * </p>
     */
    CANCELLED("취소", true, false, false);

    private final String description;
    private final boolean isFinal;         // 최종 상태 여부
    private final boolean cancellable;      // 취소 가능 여부
    private final boolean retryable;        // 재시도 가능 여부

    TransactionStatus(String description, boolean isFinal, boolean cancellable, boolean retryable) {
        this.description = description;
        this.isFinal = isFinal;
        this.cancellable = cancellable;
        this.retryable = retryable;
    }

    /**
     * 상태 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 최종 상태 여부 확인
     * <p>
     * 최종 상태는 더 이상 변경할 수 없습니다.
     * </p>
     *
     * @return 최종 상태이면 true
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * 취소 가능 여부 확인
     *
     * @return 취소 가능하면 true
     */
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * 재시도 가능 여부 확인
     * <p>
     * PENDING 상태에서만 재시도가 가능합니다.
     * </p>
     *
     * @return 재시도 가능하면 true
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * 처리 중 여부 확인
     *
     * @return PENDING이면 true
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 성공 여부 확인
     *
     * @return SUCCESS이면 true
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 실패 여부 확인
     *
     * @return FAILED이면 true
     */
    public boolean isFailed() {
        return this == FAILED;
    }

    /**
     * 취소됨 여부 확인
     *
     * @return CANCELLED이면 true
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * 완료 여부 확인 (성공 또는 실패)
     * <p>
     * 거래 처리가 완료되었는지 확인합니다.
     * 취소는 완료로 간주하지 않습니다.
     * </p>
     *
     * @return SUCCESS 또는 FAILED이면 true
     */
    public boolean isCompleted() {
        return this == SUCCESS || this == FAILED;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     * <p>
     * 같은 상태로의 전환은 불가능합니다.
     * 최종 상태에서는 다른 상태로 전환 불가합니다.
     * </p>
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(TransactionStatus target) {
        if (this == target) {
            return false;
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     *
     * @return 전환 가능한 상태 Set
     */
    public Set<TransactionStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(SUCCESS, FAILED, CANCELLED);
            case SUCCESS, FAILED, CANCELLED -> EnumSet.noneOf(TransactionStatus.class);  // 최종 상태
        };
    }
}