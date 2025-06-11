package com.bank.framework.config;

import com.bank.common.utils.FileUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Value("${aliyun.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${aliyun.kafka.username}")
    private String username;

    @Value("${aliyun.kafka.password}")
    private String password;

    @Value("${aliyun.kafka.security-protocol}")
    private String securityProtocol;

    @Value("${aliyun.kafka.sasl-mechanism}")
    private String saslMechanism;

    @Value("${aliyun.kafka.consumer-group}")
    private String consumerGroup;

    @Value("${aliyun.kafka.ssl.truststore.location}")
    private String truststoreLocation;

    @Value("${aliyun.kafka.ssl.truststore.password}")
    private String truststorePassword;


    @Bean
    public ConsumerFactory<String, String> consumerFactory() throws IOException {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 启用批量消费
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5000);
        // 1MB最小批次
        configs.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1 * 1024 * 1024);
        // 单分区最大拉取字节
        configs.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10 * 1024 * 1024);

        configs.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        configs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configs.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configs.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        // 阿里云Kafka SASL认证配置
        configs.put("security.protocol", securityProtocol);
        configs.put("sasl.mechanism", saslMechanism);
        configs.put("sasl.jaas.config", String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";", username, password));

        configs.put("ssl.truststore.location", FileUtil.resolveTruststoreFile(truststoreLocation));
        configs.put("ssl.truststore.password", truststorePassword);
        configs.put("ssl.protocol", "TLSv1.2");
        configs.put("ssl.enabled.protocols", "TLSv1.2");
        // 这会降低安全性，仅限开发和测试环境使用
        configs.put("ssl.endpoint.identification.algorithm", "");
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean("batchFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory() throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // 设置批量监听模式
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(new CommonLoggingErrorHandler());

        // 关键修改：设置手动提交确认模式
        ContainerProperties containerProperties = factory.getContainerProperties();
        // 使用手动确认
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // 消费者线程数
        factory.setConcurrency(10);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // 启用批量消费
        factory.setBatchListener(true);
        return factory;
    }
}
