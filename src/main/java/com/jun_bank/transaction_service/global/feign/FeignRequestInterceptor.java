package com.jun_bank.transaction_service.global.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Client 요청 인터셉터
 * - 현재 요청의 인증 헤더를 다른 서비스로 전파
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.debug("RequestContext가 없습니다. 헤더 전파 생략");
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        // 인증 헤더 전파
        propagateHeader(request, template, HEADER_USER_ID);
        propagateHeader(request, template, HEADER_USER_ROLE);
        propagateHeader(request, template, HEADER_USER_EMAIL);

        // 분산 추적 헤더 전파
        propagateHeader(request, template, HEADER_TRACE_ID);
    }

    private void propagateHeader(HttpServletRequest request,
                                 RequestTemplate template,
                                 String headerName) {
        String headerValue = request.getHeader(headerName);
        if (StringUtils.hasText(headerValue)) {
            template.header(headerName, headerValue);
            log.debug("헤더 전파: {} = {}", headerName, headerValue);
        }
    }
}