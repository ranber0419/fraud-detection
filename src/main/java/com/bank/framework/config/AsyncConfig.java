package com.bank.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 欺诈检测专用线程池
     * 核心配置参数（根据实际生产环境调整）：
     * - 核心线程数：系统常驻处理线程
     * - 最大线程数：突发流量时最大扩展线程
     * - 队列容量：缓冲待处理任务
     * - 拒绝策略：队列满时的处理策略
     */
    @Bean(name = "fraudDetectionExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        // 使用 SynchronousQueue（无界，但每个任务必须立即被执行）
        executor.setQueueCapacity(0);
        // 线程名前缀
        executor.setThreadNamePrefix("fraud-detection-");
        // 拒绝策略 (CallerRunsPolicy: 由调用线程处理该任务)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 非核心线程空闲存活时间(秒)
        executor.setKeepAliveSeconds(60);
        // 等待所有任务完成再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待任务完成的超时时间(秒)
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
