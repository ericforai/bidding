#!/bin/bash

# XiYu Bid POC Backend - 启动脚本

echo "Starting XiYu Bid POC Backend..."

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or higher is required"
    exit 1
fi

# 使用dev配置启动
echo "Using H2 in-memory database (dev profile)"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
