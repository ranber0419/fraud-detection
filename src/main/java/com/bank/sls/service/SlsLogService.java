package com.bank.sls.service;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.request.PutLogsRequest;
import com.aliyun.openservices.log.response.PutLogsResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SlsLogService {

    private static final Logger log = LoggerFactory.getLogger(SlsLogService.class);

    private final Client slsClient;

    @Value("${aliyun.sls.project}")
    private String project;

    private final String topic = "fraud-detection";

    public SlsLogService(Client slsClient) {
        this.slsClient = slsClient;
    }

    @Async
    public void log(String logStore, String message) throws LogException {
        log(logStore, message, null);
    }

    @Async
    public void log(String logStore, String message, Throwable exception) throws LogException {

        LogItem logItem = new LogItem((int) (System.currentTimeMillis() / 1000));

        // 添加基本日志字段
        logItem.PushBack("timestamp", String.valueOf(System.currentTimeMillis()));

        // 添加消息内容
        if (StringUtils.isNotBlank(message)) {
            logItem.PushBack("message", message);
        }

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (StringUtils.isNotBlank(exceptionMessage)) {
                logItem.PushBack("error_message", exceptionMessage);
            }
        }

        List<LogItem> logItems = new ArrayList<>();
        logItems.add(logItem);
        // 发送日志到 SLS
        PutLogsRequest putLogsRequest = new PutLogsRequest(project, logStore, topic, logItems);
        slsClient.PutLogs(putLogsRequest);
    }
}
