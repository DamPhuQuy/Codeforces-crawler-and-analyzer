# Để build image:
# docker build -t codeforces-analysis .

# Sử dụng Maven để build trong một container riêng (Multi-stage build)
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
COPY flyway.conf .

# Chạy maven package gộp toàn bộ thành file jar (có file config bên trong)
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Khai báo các environment variable mặc định cho app
ENV DB_HOST=postgres_db
ENV DB_PORT=5432
ENV DB_NAME=cf_analysis
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres
ENV DISPLAY=:0

# Chỉ định lib đường dẫn vào container
COPY --from=builder /app/target/codeforces-examination-analysis-1.0-SNAPSHOT.jar app.jar

# Tạo folder logs
RUN mkdir logs

# Cài đặt file thiết lập môi trường gui cho docker java
# Lưu ý: vì là app Giao Diện (Java Swing), chạy trong Docker bằng cách X11 Forwarding
# Chứ nếu không sẽ bị lỗi "HeadlessException" hoặc "No X11 DISPLAY variable"
RUN apt-get update && apt-get install -y libxext6 libxrender1 libxtst6 && rm -rf /var/lib/apt/lists/*

CMD ["java", "-jar", "app.jar"]
