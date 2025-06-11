package com.bank.frauddetection.service;

import com.bank.fraudDetection.entity.FraudResult;
import com.bank.fraudDetection.entity.Transaction;
import com.bank.fraudDetection.service.FraudDetectionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

@SpringBootTest
public class FraudDetectionServiceTest {
    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Test
    void shouldDetectHighAmountTransaction() {
        Transaction tx = new Transaction("tx1", "acct-9999", BigDecimal.valueOf(15000.0), "merchant", Instant.now());
        FraudResult fraudResult = fraudDetectionService.detectFraud(tx);
        Assertions.assertTrue(fraudResult.getIsFraud());
    }

    @Test
    void shouldDetectSuspiciousAccount() {
        Transaction tx = new Transaction("tx2", "acct-4444", BigDecimal.valueOf(500.0), "merchant", Instant.now());
        FraudResult fraudResult = fraudDetectionService.detectFraud(tx);
        Assertions.assertTrue(fraudResult.getIsFraud());
    }

    @Test
    void shouldPassNormalTransaction() {
        Transaction tx = new Transaction("tx4", "acct-9999", BigDecimal.valueOf(1000.0), "merchant", Instant.now());
        FraudResult fraudResult = fraudDetectionService.detectFraud(tx);
        Assertions.assertFalse(fraudResult.getIsFraud());
    }
}
