package com.bank.sls.enums;

public enum SLSLogTypeEnum {

    message_received("fraud-detection-logstore-message-received"),
    alert_triggered("fraud-detection-logstore-alert-triggered"),
    fraudulent("fraud-detection-logstore-fraudulent"),
    exception("fraud-detection-logstore-exception");
    private final String name;

    SLSLogTypeEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
