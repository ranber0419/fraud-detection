package com.bank.alert.service;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.log.exception.LogException;
import com.bank.fraudDetection.entity.FraudResult;
import com.bank.sls.enums.SLSLogTypeEnum;
import com.bank.sls.service.SlsLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertService {

    @Resource
    private SlsLogService slsLogService;

    public void sendAlert(FraudResult fraudResult) throws LogException {
        // 日志警报 (可扩展为邮件/SMS通知)
        log.warn("欺诈警报! 交易ID: {}, 原因: {}",
                fraudResult.getTransactionId(),
                String.join(" | ", fraudResult.getReasons()));

        // 记录告警触发日志
        String message = JSON.toJSONString(fraudResult);
        slsLogService.log(SLSLogTypeEnum.alert_triggered.getName(), message);
    }
}
