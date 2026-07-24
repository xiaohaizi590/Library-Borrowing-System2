# ========== Stage 1: Maven 构建 ==========
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# 配置阿里云 Maven 镜像
COPY settings.xml /root/.m2/settings.xml

# 先复制 pom.xml 文件，利用 Docker 缓存层
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY server/pom.xml ./server/

# 下载依赖（离线构建加速）
RUN mvn dependency:go-offline -B -q

# 复制源码并构建
COPY common/ ./common/
COPY server/ ./server/
RUN mvn clean package -DskipTests -B

# ========== Stage 2: 运行 JAR ==========
FROM eclipse-temurin:21-jre-alpine

# 安装字体（验证码依赖 Arial 字体）
RUN apk add --no-cache fontconfig ttf-dejavu

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=build /app/server/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
