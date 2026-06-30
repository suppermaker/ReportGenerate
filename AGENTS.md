# AGENTS.md

本文件给后续智能体阅读，用于快速理解 ReportGenerate 项目的技术方向、代码边界和协作规则。进入仓库后请先读 `README.md`，它是当前业务架构设计的主要来源。

## 项目定位

ReportGenerate 是一个报告生成平台，面向密码安全应用测评报告、水利行业文档和自定义报告生成场景。

当前架构目标：

- Java 17
- Spring Boot 模块化单体
- MyBatis + MySQL 存储业务元数据
- Redis 承载缓存、锁和轻量任务队列
- MinIO 存储模板、参考文档、生成报告和预览文件
- 外部 Agent 服务负责内容生成、合规审查和智能分析
- 外部知识库服务负责标准条款、行业规范和上下文增强检索

系统自身不直接实现大模型能力和 RAG 能力，应通过适配层调用外部 HTTP 服务。

## 常用命令

```bash
mvn test
mvn spring-boot:run
mvn clean package
```

本地服务默认端口：

- 应用：`http://localhost:8080`
- 探活接口：`GET /api/system/ping`
- Actuator 健康检查：`GET /actuator/health`

运行时配置通过环境变量覆盖，常用变量包括：

- `SERVER_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET`
- `AGENT_BASE_URL`
- `KNOWLEDGE_BASE_URL`

## 目录与包结构

根包为 `com.company.dgp`。

已规划模块：

- `common`：统一响应、异常、配置、安全、工具、枚举等通用能力；不要放业务逻辑。
- `auth`：用户、认证、权限、SSO 对接。
- `report`：报告记录、报告版本、报告预览、下载、分享和修订。
- `project`：报告生成项目、报告类型、生成参数。
- `template`：模板上传、变量解析、模板版本管理。
- `file`：参考文档、对象文件元数据、MinIO 文件访问。
- `orchestrator`：生成任务编排、任务状态机、Worker、阶段日志。
- `agent`：外部 Agent 服务适配、调用日志、结果解析。
- `knowledge`：外部知识库检索适配、检索日志、上下文注入。
- `render`：Word / PDF / Markdown 渲染、预览文件生成。
- `audit`：审计日志、下载留痕、关键操作记录。

每个业务模块优先按以下分层组织：

```text
controller
application
domain
repository
mapper
dto
infra
```

并非每个模块都必须一次性创建所有分层；按实际功能需要补齐。

## 分层规则

- Controller 只处理 HTTP 入参、校验、当前用户上下文和响应封装。
- Controller 只能调用本模块 Application Service。
- Application Service 是业务用例编排和事务边界。
- 跨模块协作应通过对方暴露的 Facade 或 Application Service 接口完成。
- Domain 层表达业务概念和规则，不依赖 Spring MVC、MyBatis、HTTP Client、MinIO SDK 等技术细节。
- Repository 面向领域提供持久化接口。
- Mapper 负责 SQL 实现；复杂 SQL 优先放 XML。
- 禁止跨模块直接调用对方 Mapper。
- 禁止 Controller 直接调用 Mapper、Redis、MinIO、外部 Agent 或外部知识库。
- 外部系统统一封装在 `infra` 或专门 adapter 中。

## 响应与异常

统一响应结构位于 `common.result.ApiResponse`：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "..."
}
```

业务异常优先使用 `common.exception.BusinessException`，全局异常处理位于 `common.exception.GlobalExceptionHandler`。

请求链路 ID 由 `common.config.RequestIdFilter` 处理，响应头使用 `X-Request-Id`。

## 数据与任务约定

README 中规划的核心表包括：

- `report`
- `report_version`
- `report_project`
- `template`
- `template_variable`
- `file_object`
- `generate_task`
- `task_stage_log`
- `agent_call_log`
- `knowledge_retrieval_log`

报告生成是异步任务，不要在 HTTP 请求线程中长时间阻塞执行完整生成流程。推荐流程：

1. 创建报告、版本和生成任务记录。
2. 写入 Redis 队列或标记待执行状态。
3. Worker 获取任务并执行编排。
4. 按阶段更新任务状态和阶段日志。
5. 调用知识库、Agent、渲染和文件上传。
6. 更新最终状态为 `COMPLETED`、`FAILED` 或 `REVIEWING`。

状态机参考 README 中的：

```text
DRAFT -> SUBMITTED -> GENERATING -> KNOWLEDGE_RETRIEVING
-> AGENT_PROCESSING -> RENDERING -> REVIEWING -> COMPLETED
```

失败分支可进入 `FAILED` 或 `RETRYING`。

## 编码约定

- 使用 Java 17 语法，保持代码简单直接。
- 新增接口时配套 DTO，并使用 Bean Validation 做入参约束。
- 对外部服务调用设置超时、错误转换和调用日志。
- 文件访问不要直接暴露 MinIO 真实地址，应通过后端权限校验后生成临时 URL。
- 需要事务的业务用例放在 Application Service。
- 不要把业务判断塞进 MyBatis XML 或 Controller。
- 不要在 `common` 中沉淀具体业务概念。
- 测试优先覆盖 Application Service、状态流转、适配器错误处理和关键 Mapper。

## 智能体工作规则

- 开始修改前先阅读相关模块和 `README.md`，不要只凭文件名猜测。
- 优先遵循现有包结构和分层边界。
- 修改范围保持聚焦，不做无关重构。
- 触碰公共契约、状态机、数据库结构或外部接口时，同步更新 README 或相关文档。
- 新增可运行功能后至少执行 `mvn test`。
- 仓库可能存在用户未提交改动，禁止回滚与当前任务无关的文件。
- 生成 SQL、配置和接口示例时，避免写入真实密钥、账号或生产地址。

