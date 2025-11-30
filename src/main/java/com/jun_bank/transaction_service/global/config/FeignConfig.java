package com.jun_bank.transaction_service.global.config;

import com.jun_bank.transaction_service.global.feign.FeignErrorDecoder;
import com.jun_bank.transaction_service.global.feign.FeignRequestInterceptor;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 설정
 * - 로깅 레벨: BASIC
 * - 에러 디코더: FeignErrorDecoder (BusinessException 변환)
 * - 요청 인터셉터: 인증 헤더 전파
 */
@Configuration
@EnableFeignClients(basePackages = "com.jun_bank.transaction_service")
public class FeignConfig {

    /**
     * Feign 로깅 레벨
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Feign 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Feign 요청 인터셉터 (인증 헤더 전파)
     */
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }
}