package com.jun_bank.transaction_service.domain.transaction.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.transaction_service.domain.transaction.domain.exception.TransactionException;

/**
 * 거래 식별자 VO (Value Object)
 * <p>
 * 거래의 고유 식별자입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>TXN-xxxxxxxx (예: TXN-a1b2c3d4)</pre>
 * <ul>
 *   <li>TXN: 거래 도메인 프리픽스 (고정)</li>
 *   <li>-: 구분자</li>
 *   <li>xxxxxxxx: 8자리 랜덤 영숫자 (UUID 기반)</li>
 * </ul>
 *
 * @param value 거래 ID 문자열 (TXN-xxxxxxxx 형식)
 */
public record TransactionId(String value) {

    /**
     * ID 프리픽스
     */
    public static final String PREFIX = "TXN";

    /**
     * TransactionId 생성자 (Compact Constructor)
     *
     * @param value 거래 ID 문자열
     * @throws TransactionException ID 형식이 유효하지 않은 경우 (TXN_001)
     */
    public TransactionId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw TransactionException.invalidTransactionIdFormat(value);
        }
    }

    /**
     * 문자열로부터 TransactionId 객체 생성
     *
     * @param value 거래 ID 문자열
     * @return TransactionId 객체
     * @throws TransactionException ID 형식이 유효하지 않은 경우
     */
    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    /**
     * 새로운 거래 ID 생성
     *
     * @return 생성된 ID 문자열 (TXN-xxxxxxxx 형식)
     */
    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}