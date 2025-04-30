# 第二阶段：使用轻量运行时镜像
FROM openjdk:21
ARG JAR_FILE=target/*.jar

# 设置工作目录和时区
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制 JAR 文件到容器
COPY target/acot-backend-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（对应 Spring Boot 配置的 server.port，默认为 8080）
EXPOSE 48080

# 启动命令
CMD ["java", "-jar", "app.jar"]