# 后端多阶段构建：CI（GitHub Actions）中执行，产物为可运行镜像
# 构建阶段：JDK8 + Maven 打包 war（与本地 ./mvnw clean package 等价）
FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B package -DskipTests

# 运行阶段：与服务器存量容器同版本 tomcat:9.0.41-jdk8-corretto
FROM tomcat:9.0.41-jdk8-corretto
# 部署为 ROOT（context path = /），nginx 按 /v2、/staging/ 路径前缀分流
COPY --from=build /build/target/alphaclass-0.0.1.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
