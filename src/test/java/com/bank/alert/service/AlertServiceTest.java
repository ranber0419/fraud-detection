package com.bank.alert.service;

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.log.exception.LogException;
import com.bank.fraudDetection.entity.FraudResult;
import com.bank.sls.service.SlsLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class AlertServiceTest {

    @InjectMocks
    private AlertService alertService;

    @Mock
    private SlsLogService slsLogService;

    @Test
    void sendAlert_ShouldLogWarningAndCallSlsService() throws LogException {
        String EXPECTED_LOG_TYPE = "fraud-detection-logstore-alert-triggered";
        // 1. 准备测试数据
        FraudResult fraudResult = new FraudResult();
        fraudResult.setTransactionId("TX123456");
        fraudResult.setReasons(List.of("高额交易", "异常地点"));

        // 2. 执行测试方法
        alertService.sendAlert(fraudResult);

        // 3. 验证SlsLogService调用
        Mockito.verify(slsLogService).log(ArgumentMatchers.eq(EXPECTED_LOG_TYPE), // 验证日志类型
                ArgumentMatchers.eq(JSON.toJSONString(fraudResult)) // 验证JSON内容
        );
    }
}
