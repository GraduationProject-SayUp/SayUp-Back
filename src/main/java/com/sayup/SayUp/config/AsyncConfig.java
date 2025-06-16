package com.sayup.SayUp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 설정 클래스
 * 비동기 메서드(@Async 어노테이션이 붙은 메서드)가 실행될 때 별도의 스레드에서 실행
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * @return 비동기 처리를 위한 Executor 객체 반환
     */
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // 기본적으로 유지되는 스레드의 수
        executor.setMaxPoolSize(10);           // 최대 스레드의 수
        executor.setQueueCapacity(500);        // 스레드가 모두 사용 중일 때 작업을 대기시키기 위한 큐의 용량
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
