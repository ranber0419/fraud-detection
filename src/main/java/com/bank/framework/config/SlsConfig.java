package com.bank.framework.config;


import com.aliyun.openservices.aliyun.log.producer.LogProducer;
import com.aliyun.openservices.aliyun.log.producer.ProducerConfig;
import com.aliyun.openservices.log.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlsConfig {

    @Value("${aliyun.sls.endpoint}")
    private String endpoint;

    @Value("${aliyun.sls.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.sls.accessKeySecret}")
    private String accessKeySecret;

    @Bean
    public Client slsClient() {
        return new Client(endpoint, accessKeyId, accessKeySecret);
    }

}