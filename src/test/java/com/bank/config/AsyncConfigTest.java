package com.bank.config;

import com.bank.framework.config.AsyncConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AsyncConfigTest {
    @Test
    void getAsyncExecutor_ShouldReturnConfiguredThreadPool() {
        // 创建配置实例
        AsyncConfig asyncConfig = new AsyncConfig();

        // 获取执行器
        Executor executor = asyncConfig.getAsyncExecutor();

        // 验证执行器类型
        assertTrue(executor instanceof ThreadPoolTaskExecutor, "Executor should be an instance of ThreadPoolTaskExecutor");

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        // 验证核心线程数
        assertEquals(50, taskExecutor.getCorePoolSize(), "Core pool size should be 50");

        // 验证最大线程数
        assertEquals(200, taskExecutor.getMaxPoolSize(), "Max pool size should be 200");

        // 验证线程名前缀
        assertEquals("fraud-detection-", taskExecutor.getThreadNamePrefix(), "Thread name prefix should be 'fraud-detection-'");

        // 验证线程保活时间
        assertEquals(60, taskExecutor.getKeepAliveSeconds(), "Keep alive seconds should be 60");

        // 验证队列类型和容量
        ThreadPoolExecutor threadPoolExecutor = taskExecutor.getThreadPoolExecutor();
        BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
        assertTrue(queue instanceof java.util.concurrent.SynchronousQueue, "Queue should be SynchronousQueue");

        // 验证拒绝策略
        assertTrue(threadPoolExecutor.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.CallerRunsPolicy, "Rejected execution handler should be CallerRunsPolicy");
    }

    @Test
    void getAsyncExecutor_ShouldInitializeExecutor() {
        // 创建配置实例
        AsyncConfig asyncConfig = new AsyncConfig();

        // 获取执行器
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // 验证执行器已初始化
        assertTrue(taskExecutor.isRunning(), "Executor should be running");
    }

    @Test
    void getAsyncExecutor_ShouldUseSynchronousQueueBehavior() throws InterruptedException {
        // 创建配置实例
        AsyncConfig asyncConfig = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();

        // 创建一个简单的任务
        Runnable task = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // 提交任务直到达到最大线程数
        for (int i = 0; i < 200; i++) {
            executor.execute(task);
        }

        // 验证线程池状态
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        assertEquals(200, threadPoolExecutor.getPoolSize(), "Pool size should be at max pool size");
        assertEquals(0, threadPoolExecutor.getQueue().size(), "Queue should be empty as SynchronousQueue doesn't hold tasks");

        // 提交额外任务（应该由调用线程执行）
        long startTime = System.currentTimeMillis();
        executor.execute(task);
        long duration = System.currentTimeMillis() - startTime;

        // 验证任务由调用线程执行（CallerRunsPolicy）
        assertTrue(duration >= 90, "Task should have been executed by the calling thread");

        // 关闭执行器
        executor.shutdown();
        assertTrue(executor.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS), "Executor should shut down properly");
    }

}
