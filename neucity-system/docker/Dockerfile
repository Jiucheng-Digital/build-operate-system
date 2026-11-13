# 基于Debian的完整JDK镜像，默认支持TLS 1.2
FROM openjdk:8u302-jdk-slim
WORKDIR /app

# 修复Debian系统的时区设置（替换Alpine的apk为apt-get）
# DEBIAN_FRONTEND=noninteractive 避免tzdata安装时交互式弹窗
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata openssl && \
    ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    # 清理apt缓存，减小镜像体积
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 创建目录结构
RUN mkdir -p /app/config \
    && mkdir -p /app/libs \
    && mkdir -p /app/logs \
    && mkdir -p /app/tmp \
    && mkdir -p /app/data/file

# 复制文件
COPY neucity-wisdom-hospital.jar /app/neucity-wisdom-hospital.jar

# 设置环境变量：时区 + JVM参数 + 配置文件路径
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Dfile.encoding=utf-8 -Duser.timezone=Asia/Shanghai -Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2"
# 明确指定配置文件（支持多配置文件，用逗号分隔）
ENV SPRING_CONFIG_LOCATION="file:/app/config/bootstrap.yml"

# 启动命令：指定配置文件路径，优先级：外部挂载的/config > 包内classpath
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.config.location=${SPRING_CONFIG_LOCATION} -jar /app/neucity-wisdom-hospital.jar"]