package com.bank.fraudDetection.service;

import com.bank.common.GlobalConstants;
import com.bank.fraudDetection.entity.FraudResult;
import com.bank.fraudDetection.entity.Transaction;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudDetectionService {

    /**
     * 检测时候是否是欺诈
     * @param transaction
     * @return
     */
    public FraudResult detectFraud(Transaction transaction) {
        List<String> reasons = new ArrayList<>();

        // 规则1: 高风险金额交易
        if (transaction.getAmount().compareTo(GlobalConstants.HIGH_RISK_THRESHOLD) > 0) {
            reasons.add("金额超过风险阈值");
        }

        // 规则2: 可疑账户
        if (GlobalConstants.suspiciousAccounts.contains(transaction.getAccountNo())) {
            reasons.add("可疑账户");
        }
        FraudResult fraudResult = new FraudResult(transaction, reasons);
        return fraudResult;
    }
}
