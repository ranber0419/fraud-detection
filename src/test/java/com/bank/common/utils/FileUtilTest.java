package com.bank.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveTruststoreFile_FileSystemResource_ShouldReturnSamePath() throws IOException {
        // 准备测试数据
        File testFile = tempDir.resolve("external-truststore.jks").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        String filePath = testFile.getAbsolutePath();

        // 执行测试
        String result = FileUtil.resolveTruststoreFile(filePath);

        // 验证结果
        assertEquals(filePath, result, "Should return original file path");
    }
}
