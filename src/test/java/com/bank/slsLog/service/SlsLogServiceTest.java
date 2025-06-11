package com.bank.slsLog.service;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogContent;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.PutLogsRequest;
import com.aliyun.openservices.log.response.PutLogsResponse;
import com.bank.sls.enums.SLSLogTypeEnum;
import com.bank.sls.service.SlsLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class SlsLogServiceTest {

    @Mock
    private Client mockSlsClient;

    @InjectMocks
    private SlsLogService slsLogService;

    private static final String TEST_PROJECT = "fraud-detection-log";
    private static final String TOPIC = "fraud-detection";

    @BeforeEach
    public void setUp() {
        // 模拟@Value注入
        ReflectionTestUtils.setField(slsLogService, "project", TEST_PROJECT);
    }

    // 辅助方法：从LogContent列表中查找指定key的值
    private Optional<String> findLogContentValue(List<LogContent> contents, String key) {
        return contents.stream()
                .filter(c -> c.mKey.equals(key))
                .map(c -> c.mValue)
                .findFirst();
    }

    @Test
    void testLog_Success() throws LogException {
        String logStore = SLSLogTypeEnum.message_received.getName();
        String message = "Test message";
        PutLogsResponse expectedResponse = Mockito.mock(PutLogsResponse.class);
        // 模拟依赖行为
        Mockito.when(mockSlsClient.PutLogs(Mockito.any())).thenReturn(expectedResponse);
        // 调用被测方法
        slsLogService.log(logStore, message);
        // 捕获日志请求
        ArgumentCaptor<PutLogsRequest> argumentCaptor = ArgumentCaptor.forClass(PutLogsRequest.class);
        Mockito.verify(mockSlsClient).PutLogs(argumentCaptor.capture());
        PutLogsRequest request = argumentCaptor.getValue();

        // 断言
        Assertions.assertEquals(TEST_PROJECT, request.GetProject());
        Assertions.assertEquals(logStore, request.GetLogStore());
        Assertions.assertEquals(TOPIC, request.GetTopic());

        List<LogItem> items = request.GetLogItems();
        Assertions.assertEquals(1, items.size());

        LogItem logItem = items.get(0);
        List<LogContent> contents = logItem.GetLogContents();

        // 验证消息内容
        Optional<String> messageValue = findLogContentValue(contents, "message");
        Assertions.assertTrue(messageValue.isPresent());
        Assertions.assertEquals(message, messageValue.get());

        // 验证时间戳存在
        Optional<String> timestampValue = findLogContentValue(contents, "timestamp");
        Assertions.assertTrue(timestampValue.isPresent());
    }

    @Test
    void testLog_WithException() throws LogException {
        String logStore = SLSLogTypeEnum.message_received.getName();
        String message = "Test message";
        Throwable exception = new RuntimeException("Test error message");

        PutLogsResponse expectedResponse = Mockito.mock(PutLogsResponse.class);
        // 模拟依赖行为
        Mockito.when(mockSlsClient.PutLogs(Mockito.any())).thenReturn(expectedResponse);
        // 调用被测方法
        slsLogService.log(logStore, message, exception);
        // 捕获日志请求
        ArgumentCaptor<PutLogsRequest> argumentCaptor = ArgumentCaptor.forClass(PutLogsRequest.class);
        Mockito.verify(mockSlsClient).PutLogs(argumentCaptor.capture());
        PutLogsRequest request = argumentCaptor.getValue();

        // 断言
        Assertions.assertEquals(TEST_PROJECT, request.GetProject());
        Assertions.assertEquals(logStore, request.GetLogStore());
        Assertions.assertEquals(TOPIC, request.GetTopic());

        List<LogItem> items = request.GetLogItems();
        Assertions.assertEquals(1, items.size());

        LogItem logItem = items.get(0);
        List<LogContent> contents = logItem.GetLogContents();

        // 验证消息内容
        Optional<String> messageValue = findLogContentValue(contents, "message");
        Assertions.assertTrue(messageValue.isPresent());
        Assertions.assertEquals(message, messageValue.get());

        // 查找error_message字段
        Optional<String> errorValue = findLogContentValue(contents, "error_message");
        Assertions.assertTrue(errorValue.isPresent());
        Assertions.assertEquals(exception.getMessage(), errorValue.get());

        // 验证时间戳存在
        Optional<String> timestampValue = findLogContentValue(contents, "timestamp");
        Assertions.assertTrue(timestampValue.isPresent());
    }

    @Test
    public void testLog_EmptyMessage() throws LogException {
        String logStore = SLSLogTypeEnum.message_received.getName();
        // 调用空消息
        slsLogService.log(logStore, null);

        // 捕获日志请求
        ArgumentCaptor<PutLogsRequest> argumentCaptor = ArgumentCaptor.forClass(PutLogsRequest.class);
        Mockito.verify(mockSlsClient).PutLogs(argumentCaptor.capture());
        PutLogsRequest request = argumentCaptor.getValue();
        List<LogItem> items = request.GetLogItems();
        Assertions.assertEquals(1, items.size());

        LogItem logItem = items.get(0);
        List<LogContent> contents = logItem.GetLogContents();

        // 验证消息内容
        Optional<String> messageValue = findLogContentValue(contents, "message");
        Assertions.assertFalse(messageValue.isPresent());
        // 验证时间戳存在
        Optional<String> timestampValue = findLogContentValue(contents, "timestamp");
        Assertions.assertTrue(timestampValue.isPresent());
    }

    @Test
    public void testLog_NullExceptionMessage() throws LogException {
        // 准备无消息的异常
        Exception exception = new RuntimeException();

        // 调用方法
        String message = "Test message";
        String logStore = SLSLogTypeEnum.message_received.getName();
        slsLogService.log(logStore, message, exception);

        // 捕获日志请求
        ArgumentCaptor<PutLogsRequest> argumentCaptor = ArgumentCaptor.forClass(PutLogsRequest.class);
        Mockito.verify(mockSlsClient).PutLogs(argumentCaptor.capture());
        PutLogsRequest request = argumentCaptor.getValue();

        List<LogItem> items = request.GetLogItems();
        Assertions.assertEquals(1, items.size());

        LogItem logItem = items.get(0);
        List<LogContent> contents = logItem.GetLogContents();
        // 查找error_message字段
        Optional<String> errorValue = findLogContentValue(contents, "error_message");
        Assertions.assertFalse(errorValue.isPresent());
    }
}
