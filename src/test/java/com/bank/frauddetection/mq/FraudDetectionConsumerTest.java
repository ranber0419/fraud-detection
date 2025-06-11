package com.bank.frauddetection.mq;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.log.exception.LogException;
import com.bank.alert.service.AlertService;
import com.bank.fraudDetection.entity.FraudResult;
import com.bank.fraudDetection.entity.Transaction;
import com.bank.fraudDetection.mq.FraudDetectionQueueConsumer;
import com.bank.fraudDetection.service.FraudDetectionService;
import com.bank.sls.enums.SLSLogTypeEnum;
import com.bank.sls.service.SlsLogService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FraudDetectionConsumerTest {

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private SlsLogService slsLogService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private FraudDetectionQueueConsumer consumer;

    @Mock
    @Qualifier("fraudDetectionExecutor")
    private Executor taskExecutor;

    private final String validMessage = "{\"accountNo\":\"123\",\"amount\":100.0}";
    private final String invalidMessage = "invalid-json";

    @BeforeEach
    void setUp() {
        // 使用 doAnswer 正确模拟 void 方法 execute()
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run(); // 同步执行任务
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));
    }
    @Test
    void listen_ShouldProcessValidMessageAndAcknowledge() throws LogException {
        // 准备测试数据
        Transaction transaction = new Transaction();
        transaction.setAccountNo("123");
        transaction.setAmount(BigDecimal.valueOf(100.0));

        FraudResult fraudResult = new FraudResult();
        fraudResult.setIsFraud(true);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "key", validMessage);
        List<ConsumerRecord<String, String>> records = Collections.singletonList(record);

        // 模拟依赖行为
        when(fraudDetectionService.detectFraud(any(Transaction.class))).thenReturn(fraudResult);

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证依赖调用
        verify(slsLogService).log(eq(SLSLogTypeEnum.message_received.getName()), eq(validMessage));
        verify(fraudDetectionService).detectFraud(transaction);
        verify(slsLogService).log(eq(SLSLogTypeEnum.fraudulent.getName()), eq(validMessage));
        verify(alertService).sendAlert(fraudResult);
        verify(acknowledgment).acknowledge(); // 确认整批消息
    }

    @Test
    void listen_ShouldHandleProcessingFailureAndNotAcknowledge() throws LogException {
        // 准备测试数据
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "key", validMessage);
        List<ConsumerRecord<String, String>> records = Collections.singletonList(record);

        // 模拟处理时抛出异常
        when(fraudDetectionService.detectFraud(any())).thenThrow(new RuntimeException("Test exception"));

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证依赖调用
        verify(slsLogService).log(eq(SLSLogTypeEnum.message_received.getName()), eq(validMessage));
        verify(fraudDetectionService).detectFraud(any());
        verify(alertService, never()).sendAlert(any());
        verify(acknowledgment, never()).acknowledge(); // 失败时不确认
    }

    @Test
    void listen_ShouldProcessMultipleMessagesAndAcknowledge() throws LogException {
        // 准备两条有效消息
        Transaction transaction = new Transaction();
        FraudResult nonFraudResult = new FraudResult();
        nonFraudResult.setIsFraud(false);

        FraudResult fraudResult = new FraudResult();
        fraudResult.setIsFraud(true);

        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("topic", 0, 0, "key1", validMessage);
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("topic", 0, 1, "key2", validMessage);
        List<ConsumerRecord<String, String>> records = List.of(record1, record2);

        // 模拟不同结果
        when(fraudDetectionService.detectFraud(any()))
                .thenReturn(nonFraudResult)  // 第一条非欺诈
                .thenReturn(fraudResult);    // 第二条欺诈

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证依赖调用
        verify(slsLogService, times(2)).log(eq(SLSLogTypeEnum.message_received.getName()), eq(validMessage));
        verify(fraudDetectionService, times(2)).detectFraud(any());
        verify(alertService).sendAlert(fraudResult); // 仅第二条触发警报
        verify(acknowledgment).acknowledge();
    }

    @Test
    void listen_ShouldHandleAsyncExecutionCorrectly() throws LogException {
        // 准备测试数据
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "key", validMessage);
        List<ConsumerRecord<String, String>> records = Collections.singletonList(record);

        // 使用 ArgumentCaptor 捕获提交的任务
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // 使用 doAnswer 捕获并执行 Runnable
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run(); // 同步执行任务
            return null;
        }).when(taskExecutor).execute(runnableCaptor.capture());

        FraudResult fraudResult = new FraudResult();
        fraudResult.setIsFraud(true);
        when(fraudDetectionService.detectFraud(any())).thenReturn(fraudResult);

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证任务是否被提交到线程池
        assertEquals(1, runnableCaptor.getAllValues().size(), "应该提交一个任务到线程池");

        // 验证处理逻辑
        verify(slsLogService).log(eq(SLSLogTypeEnum.message_received.getName()), eq(validMessage));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void listen_ShouldNotAcknowledgeWhenAnyProcessingFails() throws LogException {
        // 准备两条消息：一条成功，一条失败
        ConsumerRecord<String, String> goodRecord = new ConsumerRecord<>("topic", 0, 0, "key1", validMessage);
        ConsumerRecord<String, String> badRecord = new ConsumerRecord<>("topic", 0, 1, "key2", validMessage);
        List<ConsumerRecord<String, String>> records = List.of(goodRecord, badRecord);

        // 模拟依赖行为：第一条成功，第二条抛出异常
        when(fraudDetectionService.detectFraud(any()))
                .thenReturn(new FraudResult()) // 第一条成功
                .thenThrow(new RuntimeException("Processing failed")); // 第二条失败

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证处理逻辑
        verify(slsLogService, times(2)).log(eq(SLSLogTypeEnum.message_received.getName()), eq(validMessage));
        verify(alertService, never()).sendAlert(any());

        // 验证未确认消息
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void processRecordAsync_ShouldLogButNotProcessEmptyMessage() throws LogException {
        // 准备空消息
        ConsumerRecord<String, String> emptyRecord = new ConsumerRecord<>("topic", 0, 0, "key", "");
        List<ConsumerRecord<String, String>> records = Collections.singletonList(emptyRecord);

        // 执行测试
        consumer.listen(records, acknowledgment);

        // 验证日志记录但跳过处理
        verify(slsLogService).log(eq(SLSLogTypeEnum.message_received.getName()), eq(""));
        verify(fraudDetectionService, never()).detectFraud(any());
        verify(alertService, never()).sendAlert(any());
        verify(acknowledgment).acknowledge();
    }
}
