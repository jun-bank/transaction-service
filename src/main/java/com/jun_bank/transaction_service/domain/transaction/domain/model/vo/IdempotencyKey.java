package com.jun_bank.transaction_service.domain.transaction.domain.model.vo;

import com.jun_bank.transaction_service.domain.transaction.domain.exception.TransactionException;

import java.util.regex.Pattern;

/**
 * 멱등성 키 VO (Value Object)
 * <p>
 * 중복 요청 방지를 위한 멱등성 키입니다.
 * 클라이언트가 생성하여 X-Idempotency-Key 헤더로 전달합니다.
 *
 * <h3>키 형식:</h3>
 * <ul>
 *   <li>UUID 형식 권장 (예: 550e8400-e29b-41d4-a716-446655440000)</li>
 *   <li>최소 8자, 최대 128자</li>
 *   <li>영문자, 숫자, 하이픈(-), 언더스코어(_) 허용</li>
 * </ul>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // 클라이언트에서 생성
 * String key = UUID.randomUUID().toString();
 * // 헤더: X-Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 *
 * // 서버에서 검증
 * IdempotencyKey idempotencyKey = IdempotencyKey.of(headerValue);
 * }</pre>
 *
 * <h3>TTL (Time To Live):</h3>
 * <p>
 * 멱등성 키는 기본 24시간 동안 유효합니다.
 * 만료 후에는 같은 키로 새 요청이 처리됩니다.
 * </p>
 *
 * @param value 멱등성 키 문자열
 */
public record IdempotencyKey(String value) {

    /**
     * 최소 길이
     */
    private static final int MIN_LENGTH = 8;

    /**
     * 최대 길이
     */
    private static final int MAX_LENGTH = 128;

    /**
     * 키 패턴 (영문자, 숫자, 하이픈, 언더스코어)
     */
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    /**
     * 기본 TTL (24시간, 초 단위)
     */
    public static final long DEFAULT_TTL_SECONDS = 86400;

    /**
     * IdempotencyKey 생성자 (Compact Constructor)
     *
     * @param value 멱등성 키 문자열
     * @throws TransactionException 키가 null이거나 형식이 잘못된 경우 (TXN_004, TXN_005)
     */
    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw TransactionException.idempotencyKeyRequired();
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw TransactionException.invalidIdempotencyKeyFormat(value);
        }
        if (!KEY_PATTERN.matcher(value).matches()) {
            throw TransactionException.invalidIdempotencyKeyFormat(value);
        }
    }

    /**
     * 문자열로부터 IdempotencyKey 객체 생성
     *
     * @param value 멱등성 키 문자열
     * @return IdempotencyKey 객체
     * @throws TransactionException 키가 유효하지 않은 경우
     */
    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    /**
     * 헤더 값에서 IdempotencyKey 생성 (nullable)
     * <p>
     * 헤더 값이 null이거나 빈 문자열이면 null을 반환합니다.
     * 유효성 검사는 수행하지 않습니다.
     * </p>
     *
     * @param headerValue X-Idempotency-Key 헤더 값
     * @return IdempotencyKey 또는 null
     */
    public static IdempotencyKey fromHeader(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        return new IdempotencyKey(headerValue.trim());
    }

    /**
     * 유효한 키 형식인지 검사 (예외 없이)
     *
     * @param value 검사할 값
     * @return 유효하면 true
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }
        return KEY_PATTERN.matcher(value).matches();
    }
}