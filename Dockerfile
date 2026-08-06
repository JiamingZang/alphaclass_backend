# 后端多阶段构建：CI（GitHub Actions）中执行，产物为可运行镜像
# 构建阶段：JDK8 + Maven 打包 war（与本地 ./mvnw clean package 等价）
FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
# application.yml 被 .gitignore 忽略（真实密钥不进仓库），CI 构建时用脱敏模板生成占位版；
# 运行时由 .env.v2（compose env_file）注入 DB_PASSWORD、AI 密钥等真实环境变量
RUN cp src/main/resources/application.example.yml src/main/resources/application.yml
RUN mvn -q -B package -DskipTests

# 运行阶段：与服务器存量容器同版本 tomcat:9.0.41-jdk8-corretto
FROM tomcat:9.0.41-jdk8-corretto
# 保持原 war 名 alphaclassV2（context path = /alphaclassV2，与现状完全一致，nginx 无需改动）
COPY --from=build /build/target/alphaclass-0.0.1.war /usr/local/tomcat/webapps/alphaclassV2.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
