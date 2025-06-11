package com.bank.common;

import java.math.BigDecimal;
import java.util.Set;

public class GlobalConstants {

    /**
     * 最大限额
     */
    public static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("10000");
    /**
     * 可疑账户
     */
    public static final Set<String> suspiciousAccounts = Set.of("acct-4444", "acct-5555");
}
