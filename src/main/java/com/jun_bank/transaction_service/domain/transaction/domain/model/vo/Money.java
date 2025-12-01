package com.jun_bank.transaction_service.domain.transaction.domain.model.vo;

import com.jun_bank.transaction_service.domain.transaction.domain.exception.TransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 금액 VO (Value Object) - Transaction Service
 * <p>
 * 거래 금액을 안전하게 다루기 위한 불변 객체입니다.
 * Account Service의 Money VO와 동일한 구조입니다.
 *
 * <h3>특징:</h3>
 * <ul>
 *   <li>불변 객체 (연산 결과는 새 객체 반환)</li>
 *   <li>양수만 허용 (거래 금액은 0보다 커야 함)</li>
 *   <li>소수점 없음 (원 단위)</li>
 * </ul>
 *
 * @param amount 금액 (BigDecimal, 0 초과)
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 0;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Money 생성자 (Compact Constructor)
     * <p>
     * 금액은 0 이상이어야 합니다.
     * </p>
     *
     * @param amount 금액
     * @throws TransactionException 금액이 null이거나 음수인 경우 (TXN_002)
     */
    public Money {
        if (amount == null) {
            throw TransactionException.invalidAmount(null);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw TransactionException.invalidAmount(amount);
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * 0원 상수
     */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /**
     * long 값으로 Money 생성
     *
     * @param amount 금액
     * @return Money 객체
     */
    public static Money of(long amount) {
        if (amount == 0) {
            return ZERO;
        }
        return new Money(BigDecimal.valueOf(amount));
    }

    /**
     * BigDecimal로 Money 생성
     *
     * @param amount 금액
     * @return Money 객체
     */
    public static Money of(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return new Money(amount);
    }

    /**
     * 문자열로 Money 생성
     *
     * @param amount 금액 문자열
     * @return Money 객체
     */
    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    /**
     * 0원 여부 확인
     *
     * @return 0원이면 true
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 양수(0보다 큰) 여부 확인
     *
     * @return 양수이면 true
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 다른 금액보다 큰지 확인
     *
     * @param other 비교 대상
     * @return 크면 true
     */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 다른 금액보다 크거나 같은지 확인
     *
     * @param other 비교 대상
     * @return 크거나 같으면 true
     */
    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * 금액 더하기
     *
     * @param other 더할 금액
     * @return 새로운 Money 객체
     */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    /**
     * 금액 빼기
     *
     * @param other 뺄 금액
     * @return 새로운 Money 객체
     * @throws TransactionException 결과가 음수인 경우
     */
    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw TransactionException.insufficientBalance(this.amount, other.amount);
        }
        return new Money(result);
    }

    /**
     * 한국 원화 형식으로 포맷팅
     *
     * @return 포맷된 문자열 (예: "100,000원")
     */
    public String formatted() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount) + "원";
    }

    /**
     * long 값으로 변환
     *
     * @return long 값
     */
    public long toLong() {
        return amount.longValue();
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}