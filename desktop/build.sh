#!/bin/bash
# NJFU刷题助手 - Linux/macOS构建脚本

echo "========================================"
echo "NJFU刷题助手 桌面版构建脚本"
echo "========================================"
echo ""

# 检查Java版本
echo "[1/4] 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请安装JDK 17或更高版本"
    exit 1
fi
java -version
echo ""

# 检查Maven
echo "[2/4] 检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请安装Maven 3.6或更高版本"
    exit 1
fi
mvn -version
echo ""

# 清理并编译
echo "[3/4] 编译项目..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo "错误: 编译失败"
    exit 1
fi
echo ""

# 打包
echo "[4/4] 打包应用..."
mvn package
if [ $? -ne 0 ]; then
    echo "错误: 打包失败"
    exit 1
fi
echo ""

echo "========================================"
echo "构建完成！"
echo "JAR文件位置: target/njfu-grinding-desktop-1.0.0.jar"
echo "========================================"
echo ""
echo "运行应用: java -jar target/njfu-grinding-desktop-1.0.0.jar"
echo "或者使用: mvn javafx:run"
echo ""