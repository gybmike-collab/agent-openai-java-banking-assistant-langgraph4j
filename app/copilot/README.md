# Copilot 模块说明

`app/copilot` 是银行助手应用的后端聚合模块，采用 Maven 多模块结构，包含 5 个子项目：

- `copilot-backend`
- `langchain4j-agents`
- `langgraph4j-agents`
- `langchain4j-openapi`
- `copilot-common`

## 各子项目职责

### 1) `copilot-backend`

- 对外提供聊天接口（如 `/api/chat`）
- 负责应用启动、Spring 配置装配、策略切换
- 根据 `agent.strategy` 选择 `langchain4j` 或 `langgraph4j` 工作流

### 2) `langchain4j-agents`

- 提供基于 LangChain4j 的多智能体实现
- 包含账户、交易、支付等领域 Agent
- 封装 MCP 工具调用（连接各业务 API 的 MCP Server）

### 3) `langgraph4j-agents`

- 提供基于 LangGraph4j 的工作流编排实现
- 负责状态、节点、边和路由控制
- 复用 `langchain4j-agents` 中的领域 Agent 能力

### 4) `langchain4j-openapi`

- 提供 OpenAPI -> Tool 的导入与执行能力
- 用于把 OpenAPI 描述转换成可供 Agent 使用的工具规格

### 5) `copilot-common`

- 存放公共能力与通用组件
- 例如票据识别、Blob 存储代理等
- 供 Agent 层复用

## 模块依赖关系

当前依赖关系是单向分层为主：

- `copilot-backend` -> `langchain4j-agents`
- `copilot-backend` -> `langgraph4j-agents`
- `langgraph4j-agents` -> `langchain4j-agents`
- `langchain4j-agents` -> `copilot-common`
- `langchain4j-agents` -> `langchain4j-openapi`

可视化表示：

`copilot-backend` -> `langgraph4j-agents` -> `langchain4j-agents` -> (`copilot-common`, `langchain4j-openapi`)

并且 `copilot-backend` 也可以直接走 `langchain4j-agents` 路径（由配置控制）。

## 运行时调用链

1. 前端请求进入 `copilot-backend` 的聊天接口
2. 后端按 `agent.strategy` 选择执行路径：
   - `langchain4j`：路由型多智能体
   - `langgraph4j`：图编排工作流
3. 领域 Agent 调用 MCP Server（accounts/transactions/payments）执行工具调用
4. 汇总结果后返回给前端

## 本地开发

在 `app/copilot` 目录可按模块构建：

```bash
mvn clean install
```

只运行后端服务可进入 `copilot-backend` 模块启动 Spring Boot 应用。

关键配置文件：

- `copilot-backend/src/main/resources/application.properties`

关键参数示例：

- `agent.strategy=langgraph4j` 或 `agent.strategy=langchain4j`
- `transactions.api.url`
- `accounts.api.url`
- `payments.api.url`
