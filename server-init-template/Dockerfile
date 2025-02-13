# Docker 镜像构建
# @author <a href="https://github.com/1449038625">程序员爱可尼克</a>

FROM maven:3.8.1-jdk-8-slim as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
# todo 改成 自己的jar包名字
CMD ["java","-jar","/app/target/server-init-template-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]