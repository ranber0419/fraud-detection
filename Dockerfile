FROM openjdk:17-jdk-slim
WORKDIR /fraud-detection
COPY target/fraud-detection.jar fraud-detection.jar
RUN chmod +x fraud-detection.jar

# 暴露应用实际监听的端口
EXPOSE 80
CMD ["java", "-jar", "fraud-detection.jar"]