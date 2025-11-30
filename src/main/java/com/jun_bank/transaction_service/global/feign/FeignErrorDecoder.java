package com.jun_bank.transaction_service.global.feign;

import com.jun_bank.common_lib.exception.BusinessException;
import com.jun_bank.common_lib.exception.GlobalErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign Client 에러 디코더
 * - 외부 서비스 호출 실패 시 BusinessException으로 변환
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign 호출 실패 - method: {}, status: {}, reason: {}",
                methodKey, response.status(), response.reason());

        return switch (response.status()) {
            case 400 -> new BusinessException(GlobalErrorCode.BAD_REQUEST,
                    "외부 서비스 요청이 잘못되었습니다: " + methodKey);
            case 401 -> new BusinessException(GlobalErrorCode.UNAUTHORIZED,
                    "외부 서비스 인증에 실패했습니다: " + methodKey);
            case 403 -> new BusinessException(GlobalErrorCode.FORBIDDEN,
                    "외부 서비스 접근이 거부되었습니다: " + methodKey);
            case 404 -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND,
                    "외부 서비스에서 리소스를 찾을 수 없습니다: " + methodKey);
            case 503 -> new BusinessException(GlobalErrorCode.SERVICE_UNAVAILABLE,
                    "외부 서비스를 사용할 수 없습니다: " + methodKey);
            default -> {
                if (response.status() >= 500) {
                    yield new BusinessException(GlobalErrorCode.EXTERNAL_API_ERROR,
                            "외부 서비스 오류가 발생했습니다: " + methodKey);
                }
                yield defaultDecoder.decode(methodKey, response);
            }
        };
    }
}