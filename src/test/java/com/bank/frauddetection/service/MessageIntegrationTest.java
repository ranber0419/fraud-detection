package com.bank.frauddetection.service;

import com.alibaba.fastjson.JSON;
import com.bank.fraudDetection.entity.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@SpringBootTest
@EnableConfigurationProperties(KafkaProperties.class)
@TestPropertySource(locations = "classpath:application-test.yml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS) // 测试类级别隔离
public class MessageIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MessageIntegrationTest.class);

    private static final String BOOTSTRAP_SERVERS = "alikafka-serverless-cn-zp54ajv8a02-1000.alikafka.aliyuncs.com:9093"; // 替换为实际的broker地址
    private static final String ACCESS_KEY = "fraud-detection-test"; // 替换为实际的AccessKey
    private static final String SECRET_KEY = "kafka123456"; // 替换为实际的SecretKey
    private static final String SASL_JAAS_CONFIG = String.format(
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"%s\" password=\"%s\";", ACCESS_KEY, SECRET_KEY);

    private String truststoreLocation;
    private static final String TRUSTSTORE_PASSWORD = "KafkaOnsClient";
    private static final String TOPIC = "fraud-detection-test-1";
    private static final String GROUP_ID = "fraud-detection-test-group-1";

    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        // 获取信任库文件
        URL truststoreUrl = getClass().getClassLoader().getResource("truststore.jks");
        if (truststoreUrl == null) {
            throw new IllegalStateException("truststore.jks not found in classpath");
        }

        // 使用 URI 规范化路径，解决开头的额外斜杠问题
        Path truststorePath = Paths.get(truststoreUrl.toURI());
        truststoreLocation = truststorePath.toString();

        logger.info("Using truststore location: {}", truststoreLocation);

        // 配置生产者
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put("security.protocol", "SASL_SSL");
        producerProps.put("sasl.mechanism", "PLAIN");
        producerProps.put("sasl.jaas.config", SASL_JAAS_CONFIG);
        producerProps.put("ssl.truststore.location", truststoreLocation); // 替换为实际的truststore路径
        producerProps.put("ssl.truststore.password", TRUSTSTORE_PASSWORD); // 替换为实际的truststore密码

        producer = new KafkaProducer<>(producerProps);

        // 配置消费者
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put("security.protocol", "SASL_SSL");
        consumerProps.put("sasl.mechanism", "PLAIN");
        consumerProps.put("sasl.jaas.config", SASL_JAAS_CONFIG);
        consumerProps.put("ssl.truststore.location", truststoreLocation); // 替换为实际的truststore路径
        consumerProps.put("ssl.truststore.password", TRUSTSTORE_PASSWORD); // 替换为实际的truststore密码

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    public void tearDown() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldHandleOneTransactions() throws InterruptedException, ExecutionException {
        // 发送消息并等待确认
        String key = UUID.randomUUID().toString();
        Transaction transaction = new Transaction("tx-123", "acct-999", BigDecimal.valueOf(500.0), "merchant", Instant.now());
        String value = JSON.toJSONString(transaction);
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
        RecordMetadata metadata = producer.send(record).get();
        Assertions.assertNotNull(metadata);
        // 等待消息传递（根据网络情况调整）
        Thread.sleep(5000);

        // 消费消息
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        Assertions.assertEquals(1, records.count(), "应该接收到1条消息");

        for (ConsumerRecord<String, String> receivedRecord : records) {
            Assertions.assertEquals(key, receivedRecord.key(), "消息键不匹配");
            Assertions.assertEquals(value, receivedRecord.value(), "消息值不匹配");
            Assertions.assertEquals(TOPIC, receivedRecord.topic(), "主题不匹配");
            System.out.printf("接收到消息: 主题=%s, 分区=%d, 偏移量=%d, 键=%s, 值=%s%n",
                    receivedRecord.topic(), receivedRecord.partition(),
                    receivedRecord.offset(), receivedRecord.key(), receivedRecord.value());
        }
    }
}