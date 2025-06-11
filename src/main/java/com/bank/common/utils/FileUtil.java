package com.bank.common.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    public static String resolveTruststoreFile(String location) throws IOException {
        if (location.startsWith("classpath:")) {
            // 从类路径加载并复制到临时文件（同之前优化的代码逻辑）
            ClassPathResource resource = new ClassPathResource(location.substring("classpath:".length()));
            File tempFile = File.createTempFile("kafka-truststore-", ".jks");
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile.deleteOnExit();
            return tempFile.getAbsolutePath();
        }
        return location;
    }
}
