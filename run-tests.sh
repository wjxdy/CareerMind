#!/bin/bash

# CareerMind 测试运行脚本
# 整合单元测试、集成测试和端到端测试

set -e

echo "==================================="
echo "CareerMind 自动化测试套件"
echo "==================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试模式
MODE=${1:-all}

# 检查 Java 版本 (需要 Java 17)
check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo "✓ Java 版本: $JAVA_VERSION"
    else
        echo -e "${RED}✗ 未找到 Java，请先安装 Java 17${NC}"
        exit 1
    fi
}

# 运行后端单元测试
run_backend_tests() {
    echo ""
    echo -e "${YELLOW}>>> 运行后端单元测试和集成测试...${NC}"
    cd careermind-backend

    # 设置 Java 17 环境
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH

    mvn clean test -q

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 后端测试通过${NC}"
    else
        echo -e "${RED}✗ 后端测试失败${NC}"
        exit 1
    fi

    cd ..
}

# 运行端到端测试
run_e2e_tests() {
    echo ""
    echo -e "${YELLOW}>>> 运行端到端测试...${NC}"
    cd e2e-tests

    # 检查是否安装了 Playwright
    if [ ! -d "node_modules" ]; then
        echo "安装 Playwright 依赖..."
        npm install
        npx playwright install
    fi

    # 运行测试
    npx playwright test --reporter=list

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 端到端测试通过${NC}"
    else
        echo -e "${RED}✗ 端到端测试失败${NC}"
        echo "查看测试报告: cd e2e-tests && npm run report"
        exit 1
    fi

    cd ..
}

# 启动服务并运行完整测试
run_full_tests() {
    echo ""
    echo -e "${YELLOW}>>> 启动服务并运行完整测试...${NC}"

    # 检查端口是否被占用
    if lsof -ti:8080 > /dev/null; then
        echo "警告: 端口 8080 已被占用，尝试停止现有进程..."
        lsof -ti:8080 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi

    if lsof -ti:5173 > /dev/null; then
        echo "警告: 端口 5173 已被占用，尝试停止现有进程..."
        lsof -ti:5173 | xargs kill -9 2>/dev/null || true
        sleep 2
    fi

    # 启动后端
    echo "启动后端服务..."
    cd careermind-backend
    export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
    export PATH=$JAVA_HOME/bin:$PATH
    mvn spring-boot:run > /tmp/backend.log 2>&1 &
    BACKEND_PID=$!
    cd ..

    # 等待后端启动
    echo "等待后端启动..."
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/agents > /dev/null 2>&1; then
            echo "✓ 后端服务已启动"
            break
        fi
        sleep 1
    done

    # 启动前端
    echo "启动前端服务..."
    cd careermind-frontend
    npm run dev > /tmp/frontend.log 2>&1 &
    FRONTEND_PID=$!
    cd ..

    # 等待前端启动
    echo "等待前端启动..."
    for i in {1..30}; do
        if curl -s http://localhost:5173 > /dev/null 2>&1; then
            echo "✓ 前端服务已启动"
            break
        fi
        sleep 1
    done

    # 运行端到端测试
    cd e2e-tests
    echo "运行端到端测试..."
    npx playwright test --reporter=html || true
    TEST_RESULT=$?
    cd ..

    # 停止服务
    echo "停止服务..."
    kill $BACKEND_PID 2>/dev/null || true
    kill $FRONTEND_PID 2>/dev/null || true

    # 显示报告链接
    echo ""
    echo -e "${GREEN}===================================${NC}"
    echo -e "${GREEN}测试完成！${NC}"
    echo -e "${GREEN}===================================${NC}"
    echo ""
    echo "查看详细报告:"
    echo "  端到端测试报告: e2e-tests/playwright-report/index.html"
    echo "  后端测试报告: careermind-backend/target/site/jacoco/index.html"
    echo ""

    if [ $TEST_RESULT -eq 0 ]; then
        echo -e "${GREEN}所有测试通过!${NC}"
    else
        echo -e "${YELLOW}部分测试未通过，请查看报告${NC}"
    fi
}

# 显示帮助信息
show_help() {
    echo "CareerMind 测试运行脚本"
    echo ""
    echo "用法: ./run-tests.sh [模式]"
    echo ""
    echo "模式:"
    echo "  all      - 运行所有测试 (默认)"
    echo "  backend  - 仅运行后端单元测试"
    echo "  e2e      - 仅运行端到端测试"
    echo "  full     - 启动服务并运行完整测试"
    echo "  help     - 显示帮助"
    echo ""
    echo "示例:"
    echo "  ./run-tests.sh           # 运行所有测试"
    echo "  ./run-tests.sh backend   # 仅运行后端测试"
    echo "  ./run-tests.sh e2e       # 仅运行端到端测试"
}

# 主逻辑
case $MODE in
    all)
        check_java
        run_backend_tests
        run_e2e_tests
        ;;
    backend)
        check_java
        run_backend_tests
        ;;
    e2e)
        run_e2e_tests
        ;;
    full)
        check_java
        run_full_tests
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}未知模式: $MODE${NC}"
        show_help
        exit 1
        ;;
esac

echo ""
echo "完成!"
