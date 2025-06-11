package com.bank.fraudDetection.mq;

import com.alibaba.fastjson.JSON;
import com.bank.alert.service.AlertService;
import com.bank.fraudDetection.entity.FraudResult;
import com.bank.fraudDetection.entity.Transaction;
import com.bank.fraudDetection.service.FraudDetectionService;
import com.bank.sls.enums.SLSLogTypeEnum;
import com.bank.sls.service.SlsLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class FraudDetectionQueueConsumer {

    @Resource
    private FraudDetectionService fraudDetectionService;
    @Resource
    private AlertService alertService;

    @Resource
    private SlsLogService slsLogService;

    @Resource
    @Qualifier("fraudDetectionExecutor")
    private Executor taskExecutor;

    @KafkaListener(topics = "${aliyun.kafka.topic}",
            groupId = "${aliyun.kafka.consumer-group}",
            containerFactory = "batchFactory",
            concurrency = "99")
    public void listen(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        AtomicBoolean hasFailure = new AtomicBoolean(false);
        CompletableFuture<?>[] futures = records.stream()
                .map(record -> processRecordAsync(record, hasFailure))
                .toArray(CompletableFuture[]::new);

        // 等待所有消息处理完成
        CompletableFuture.allOf(futures).thenRun(() -> {
            if (!hasFailure.get()) {
                acknowledgment.acknowledge(); // 整批确认
            } else {
                // 可选：部分失败时重试策略（根据业务需求）
                log.error("Batch processing failed, skipping offset commit");
            }
        });
    }

    private CompletableFuture<Void> processRecordAsync(ConsumerRecord<String, String> record, AtomicBoolean hasFailure) {
        return CompletableFuture.runAsync(() -> {
            try {
                String message = record.value();
                slsLogService.log(SLSLogTypeEnum.message_received.getName(), message);
                if (StringUtils.isNotBlank(message)) {
                    Transaction transaction = JSON.parseObject(message, Transaction.class);
                    FraudResult fraudResult = fraudDetectionService.detectFraud(transaction);
                    if (fraudResult.getIsFraud()) {
                        slsLogService.log(SLSLogTypeEnum.fraudulent.getName(), message);
                        alertService.sendAlert(fraudResult);
                    }
                }
            } catch (Exception e) {
                hasFailure.set(true); // 标记失败
                log.error("Error processing record: {}", record.value(), e);
            }
        }, taskExecutor); // 使用独立线程池执行
    }

}
