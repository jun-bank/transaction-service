package com.jun_bank.transaction_service.global.infrastructure.jpa;

import com.jun_bank.transaction_service.global.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA Auditing - 현재 사용자 정보 제공
 * - createdBy, updatedBy 자동 설정에 사용
 */
@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    private final SecurityContextUtil securityContextUtil;

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(securityContextUtil.getCurrentUserId())
                .or(() -> Optional.of("SYSTEM"));
    }
}