package com.bank.fraudDetection.entity;

import lombok.Data;
import lombok.Generated;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Generated
public class Transaction {
    private String id;
    /**
     * 账号
     */
    private String accountNo;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 商户
     */
    private String merchant;
    /**
     * 时间戳
     */
    private Instant timestamp;

    public Transaction() {
    }

    public Transaction(String id, String accountNo, BigDecimal amount, String merchant, Instant timestamp) {
        this.id = id;
        this.accountNo = accountNo;
        this.amount = amount;
        this.merchant = merchant;
        this.timestamp = timestamp;
    }

}
