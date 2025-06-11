package com.bank.fraudDetection.entity;

import lombok.Data;
import lombok.Generated;

import java.time.Instant;
import java.util.List;

@Data
@Generated
public class FraudResult {

    private String transactionId;
    /**
     * 是否欺诈
     */
    private Boolean isFraud;
    /**
     * 原因
     */
    private List<String> reasons;
    /**
     * 时间戳
     */
    private Instant timestamp;

    public FraudResult() {
    }

    public FraudResult(Transaction transaction, List<String> reasons) {
        this.transactionId = transaction.getId();
        if (reasons.isEmpty()) {
            this.isFraud = false;
        } else {
            this.isFraud = true;
        }

        this.reasons = reasons;
        this.timestamp = Instant.now();
    }


}
