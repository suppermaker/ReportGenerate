报告生成平台架构设计文档
版本：v1.0
状态：草稿
架构形态：模块化单体架构
技术栈：Java / Spring Boot / MyBatis / MySQL / Redis / MinIO
适用范围：密码安全应用测评报告、水利行业文档、自定义报告生成场景

---
1. 文档概述
1.1 文档目的
本文档基于《报告生成平台产品需求文档 PRD》进行架构设计，明确报告生成平台的系统边界、模块划分、核心流程、数据存储、外部系统集成、安全机制、部署方案以及异常处理机制。
本文档用于指导后续详细设计、接口设计、数据库设计、编码实现、测试验证和系统部署。
1.2 系统定位
报告生成平台是一个基于智能体能力的专业文档生成系统，面向密码安全应用测评报告、水利行业文档、自定义报告等场景，提供从模板选择、参考文档上传、知识库检索、Agent 内容生成、报告渲染、在线预览、下载归档的完整能力。
系统本身不直接实现大模型能力和知识库检索能力，而是通过标准 HTTP 接口调用外部 Agent 服务和外部知识库服务。
1.3 架构目标
系统一期采用模块化单体架构，在一个 Spring Boot 应用内按照领域边界划分模块。
核心目标如下：
1. 支持密码安全应用测评报告、水利文档和自定义报告生成。
2. 支持模板上传、模板变量解析、模板选择和模板版本管理。
3. 支持参考文档上传、解析、存储和关联。
4. 支持调用外部 Agent 完成报告内容生成、合规审查和智能分析。
5. 支持调用外部知识库完成标准条款、行业规范和上下文增强检索。
6. 支持报告生成任务状态管理、日志追踪、失败重试和人工修订。
7. 支持报告文件存储、在线预览、下载、分享和历史版本管理。
8. 保证系统具备可扩展性、安全性、可观测性和稳定性。

---
2. 架构约束与设计原则
2.1 架构约束
暂时无法在飞书文档外展示此内容
2.2 设计原则
2.2.1 模块化单体
系统不是把所有代码混写在一起，而是在一个 Spring Boot 应用内部按照业务领域进行模块拆分。
每个模块拥有独立的 Controller、Application Service、Domain、Repository、Mapper 和 DTO。
2.2.2 领域边界清晰
系统按照业务能力划分为报告管理、报告生成项目、模板管理、文件管理、任务编排、Agent 适配、知识库适配、用户权限、审计日志等模块。
模块之间通过应用服务接口进行交互，避免跨模块直接访问内部数据表和 Mapper。
2.2.3 外部能力适配隔离
外部 Agent 服务、外部知识库服务、MinIO、Redis、SSO 等系统能力统一通过 Adapter 适配层封装。
业务模块不直接感知外部 API 的 URL、鉴权方式、请求格式和重试逻辑。
2.2.4 异步任务驱动
报告生成属于耗时任务，用户提交生成请求后，系统创建生成任务并异步执行。
用户可以通过任务状态接口或 WebSocket 查看生成进度，避免接口长时间阻塞。
2.2.5 全链路可追踪
报告生成过程中的输入参数、模板版本、参考文档、知识库检索记录、Agent 调用记录、生成结果、人工修改记录都需要留痕，便于问题排查、审计追踪和质量优化。

---
3. 总体架构设计
3.1 系统总体架构图
┌──────────────────────────────────────────────────────────────┐
│                         前端应用层                            │
│  Web 管理端 / 报告预览页 / 飞书或企业微信 H5 / 管理后台        │
└───────────────────────────────┬──────────────────────────────┘
                                │ HTTPS / REST / WebSocket
                                ▼
┌──────────────────────────────────────────────────────────────┐
│              报告生成平台后端 Spring Boot 应用                 │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │ 用户权限模块 │  │ 报告管理模块 │  │ 报告生成项目模块 │    │
│  └──────────────┘  └──────────────┘  └──────────────────┘    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │ 模板管理模块 │  │ 文件管理模块 │  │ 任务编排模块     │    │
│  └──────────────┘  └──────────────┘  └──────────────────┘    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │ Agent适配模块│  │ 知识库适配模块│  │ 审计日志模块     │    │
│  └──────────────┘  └──────────────┘  └──────────────────┘    │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ 导出渲染模块 │  │ 系统配置模块 │                         │
│  └──────────────┘  └──────────────┘                         │
└───────────────┬────────────────┬────────────────┬───────────┘
                │                │                │
                ▼                ▼                ▼
        ┌────────────┐   ┌────────────┐   ┌──────────────┐
        │   MySQL    │   │   Redis    │   │    MinIO     │
        │ 业务元数据 │   │ 缓存/队列  │   │ 文件对象存储 │
        └────────────┘   └────────────┘   └──────────────┘
                │
                ▼
┌──────────────────────────────────────────────────────────────┐
│                         外部能力层                            │
│                                                              │
│  外部 Agent：密码测评 Agent / 水利文档 Agent / 通用 Agent      │
│  外部知识库：密码标准库 / 水利规范库 / 模板知识库 / RAG API    │
│  外部认证：SSO / 企业账号体系                                  │
└──────────────────────────────────────────────────────────────┘
3.2 架构说明
系统采用单体应用部署，后端主体为一个 Spring Boot 应用。
该应用内部按照业务领域划分模块，各模块之间保持清晰边界。
整体分为五层：
暂时无法在飞书文档外展示此内容

---
4. 应用分层设计
4.1 分层结构
Controller 层
    ↓
Application Service 应用服务层
    ↓
Domain 领域层
    ↓
Repository / Mapper 数据访问层
    ↓
MySQL / Redis / MinIO / 外部 HTTP 服务
4.2 Controller 层
Controller 层负责对外提供 HTTP 接口，不直接编写复杂业务逻辑。
主要职责：
1. 接收前端请求。
2. 完成参数校验。
3. 获取当前登录用户信息。
4. 调用应用服务。
5. 返回统一响应结构。
6. 不直接调用 Mapper。
7. 不直接调用外部 Agent 和知识库。
4.3 Application Service 层
Application Service 层负责完整业务用例编排，是事务边界所在。
例如“提交报告生成任务”会涉及：
1. 校验用户权限。
2. 校验模板是否存在。
3. 校验参考文档是否有效。
4. 创建报告记录。
5. 创建报告版本。
6. 创建生成任务。
7. 写入任务队列。
8. 返回任务 ID。
4.4 Domain 领域层
Domain 层负责表达核心业务概念和业务规则。
典型领域对象包括：
暂时无法在飞书文档外展示此内容
领域层不直接依赖 Spring、MyBatis、MinIO SDK、HTTP Client 等技术框架。
4.5 Repository / Mapper 层
Repository 提供面向领域的持久化接口，Mapper 负责具体 SQL 实现。
约束如下：
1. Controller 禁止直接调用 Mapper。
2. 跨模块禁止直接调用对方 Mapper。
3. 写操作必须经过 Application Service。
4. 复杂查询可以放入 QueryService。
5. MyBatis XML 中维护复杂 SQL。
4.6 Infrastructure Adapter 层
Infrastructure Adapter 层负责封装外部系统和技术组件。
包括：
1. MinIO 文件上传、下载、预签名 URL。
2. Redis 缓存、分布式锁、任务队列。
3. 外部 Agent HTTP API。
4. 外部知识库 HTTP API。
5. SSO 用户认证。
6. Word / PDF / Markdown 渲染工具。

---
5. 后端模块设计
5.1 包结构设计
com.company.dgp
├── DgpApplication.java
│
├── common
│   ├── result
│   ├── exception
│   ├── config
│   ├── security
│   ├── util
│   └── enums
│
├── auth
│   ├── controller
│   ├── application
│   ├── domain
│   ├── repository
│   ├── mapper
│   └── infra
│
├── report
│   ├── controller
│   ├── application
│   ├── domain
│   ├── repository
│   ├── mapper
│   └── dto
│
├── project
│   ├── controller
│   ├── application
│   ├── domain
│   ├── repository
│   ├── mapper
│   └── dto
│
├── template
│   ├── controller
│   ├── application
│   ├── domain
│   ├── repository
│   ├── mapper
│   └── parser
│
├── file
│   ├── controller
│   ├── application
│   ├── domain
│   ├── repository
│   ├── mapper
│   └── infra
│
├── orchestrator
│   ├── controller
│   ├── application
│   ├── domain
│   ├── worker
│   ├── repository
│   ├── mapper
│   └── event
│
├── agent
│   ├── application
│   ├── domain
│   ├── infra
│   └── dto
│
├── knowledge
│   ├── application
│   ├── domain
│   ├── infra
│   └── dto
│
├── render
│   ├── application
│   ├── domain
│   ├── infra
│   └── dto
│
└── audit
    ├── controller
    ├── application
    ├── domain
    ├── repository
    └── mapper
5.2 模块职责说明
暂时无法在飞书文档外展示此内容
5.3 模块调用规则
为避免模块化单体演变为混乱单体，需要遵守以下规则：
1. Controller 只能调用本模块 Application Service。
2. Application Service 可以调用其他模块暴露的 Facade 接口。
3. 不允许跨模块直接调用 Mapper。
4. 不允许跨模块直接操作对方数据库表。
5. Domain 层不依赖 Controller、Mapper、HTTP Client。
6. Infrastructure 层只能被 Application Service 或 Repository 调用。
7. common 模块只放通用能力，不放业务逻辑。

---
6. 核心业务流程设计
6.1 报告生成总体流程
用户创建报告生成项目
        ↓
选择报告类型
        ↓
上传或选择参考文档
        ↓
选择模板
        ↓
选择或确认 Agent
        ↓
填写模板变量和生成参数
        ↓
提交生成任务
        ↓
后端创建报告记录和任务记录
        ↓
异步任务编排器开始执行
        ↓
解析模板和参考文档
        ↓
请求外部知识库检索相关标准和上下文
        ↓
请求外部 Agent 生成报告章节
        ↓
执行合规审查和内容校验
        ↓
渲染 Word / PDF / Markdown
        ↓
上传生成文件到 MinIO
        ↓
更新报告状态为生成成功
        ↓
用户在线预览、下载、分享、修订
6.2 报告生成状态机
DRAFT 草稿
  ↓
SUBMITTED 已提交
  ↓
GENERATING 生成中
  ↓
KNOWLEDGE_RETRIEVING 知识库检索中
  ↓
AGENT_PROCESSING Agent 处理中
  ↓
RENDERING 渲染中
  ↓
REVIEWING 待人工复核
  ↓
COMPLETED 已完成

异常分支：
GENERATING / KNOWLEDGE_RETRIEVING / AGENT_PROCESSING / RENDERING
  ↓
FAILED 生成失败
  ↓
RETRYING 重试中
  ↓
COMPLETED 或 FAILED
6.3 任务编排阶段
报告生成任务由 orchestrator 模块负责。
暂时无法在飞书文档外展示此内容

---
7. 外部 Agent 集成设计
7.1 Agent 集成原则
系统不直接实现 Agent 能力，而是通过 agent 模块调用外部 Agent HTTP API。
agent 模块需要统一处理：
1. Agent 地址配置。
2. 请求参数封装。
3. 认证信息注入。
4. 超时控制。
5. 重试控制。
6. 响应结果解析。
7. 调用日志记录。
8. 异常转换。
7.2 Agent 类型
暂时无法在飞书文档外展示此内容
7.3 Agent 调用流程
OrchestratorApplicationService
    ↓
AgentApplicationService
    ↓
AgentClientAdapter
    ↓
外部 Agent HTTP API
    ↓
返回结构化 JSON 结果
    ↓
AgentResultParser
    ↓
写入 agent_call_log
    ↓
返回给 OrchestratorApplicationService
7.4 Agent 请求示例
{
  "agentType": "CRYPTO_EVALUATION_AGENT",
  "taskId": "T202606190001",
  "input": {
    "reportType": "密码安全应用测评报告",
    "templateVariables": {},
    "referenceDocuments": [],
    "retrievedKnowledge": []
  },
  "outputFormat": "JSON"
}
7.5 Agent 返回示例
{
  "success": true,
  "sections": [
    {
      "sectionCode": "1.1",
      "title": "测评对象概述",
      "content": "……"
    }
  ],
  "conclusion": "部分符合",
  "risks": [],
  "evidences": []
}

---
8. 外部知识库集成设计
8.1 知识库集成原则
系统不直接实现向量数据库和 RAG 检索能力，而是通过 knowledge 模块调用外部知识库 HTTP API。
knowledge 模块负责统一封装知识库检索请求，并将检索结果注入 Agent 上下文。
8.2 知识库类型
暂时无法在飞书文档外展示此内容
8.3 知识库调用流程
OrchestratorApplicationService
    ↓
KnowledgeApplicationService
    ↓
KnowledgeClientAdapter
    ↓
外部知识库 HTTP API
    ↓
返回标准条款 / 规范片段 / 参考内容
    ↓
写入 knowledge_retrieval_log
    ↓
注入 Agent 上下文
8.4 知识库请求示例
{
  "domain": "CRYPTO",
  "query": "身份鉴别 密码应用安全性评估 标准要求",
  "topK": 5,
  "filters": {
    "standardCode": ["GM/T", "等保2.0"]
  }
}
8.5 知识库返回示例
{
  "results": [
    {
      "title": "身份鉴别要求",
      "content": "……",
      "source": "商用密码应用安全性评估标准",
      "score": 0.91
    }
  ]
}

---
9. 数据架构设计
9.1 数据分类
暂时无法在飞书文档外展示此内容
9.2 核心数据库表
9.2.1 report：报告表
暂时无法在飞书文档外展示此内容
9.2.2 report_version：报告版本表
暂时无法在飞书文档外展示此内容
9.2.3 report_project：报告生成项目表
暂时无法在飞书文档外展示此内容
9.2.4 template：模板表
暂时无法在飞书文档外展示此内容
9.2.5 template_variable：模板变量表
暂时无法在飞书文档外展示此内容
9.2.6 file_object：文件对象表
暂时无法在飞书文档外展示此内容
9.2.7 generate_task：生成任务表
暂时无法在飞书文档外展示此内容
9.2.8 task_stage_log：任务阶段日志表
暂时无法在飞书文档外展示此内容
9.2.9 agent_call_log：Agent 调用日志表
暂时无法在飞书文档外展示此内容
9.2.10 knowledge_retrieval_log：知识库检索日志表
暂时无法在飞书文档外展示此内容

---
10. 文件存储架构设计
10.1 MinIO Bucket 规划
暂时无法在飞书文档外展示此内容
10.2 文件路径规则
/templates/{reportType}/{templateId}/{versionNo}/{filename}

/references/{userId}/{projectId}/{fileId}/{filename}

/reports/{reportType}/{reportId}/{versionNo}/{filename}

/preview/{reportId}/{versionNo}/{filename}
10.3 文件访问控制
1. 文件不直接暴露真实 MinIO 地址。
2. 后端根据权限生成临时预签名 URL。
3. 预签名 URL 设置有效期，例如 10 分钟。
4. 下载行为写入审计日志。
5. 敏感报告可禁止分享或限制下载。
6. 删除文件时优先逻辑删除元数据，物理文件可由定时清理任务处理。

---
11. 接口架构设计
11.1 统一响应结构
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "REQ202606190001"
}
11.2 报告项目接口
暂时无法在飞书文档外展示此内容
11.3 报告生成接口
暂时无法在飞书文档外展示此内容
11.4 模板管理接口
暂时无法在飞书文档外展示此内容
11.5 文件接口
暂时无法在飞书文档外展示此内容
11.6 任务接口
暂时无法在飞书文档外展示此内容

---
12. 异步任务与并发设计
12.1 异步任务方案
一期可以使用 Redis Stream 或 Redis List 实现轻量任务队列。
用户提交生成任务
    ↓
写入 MySQL generate_task
    ↓
写入 Redis 任务队列
    ↓
后台 Worker 消费任务
    ↓
执行任务阶段
    ↓
更新 MySQL 任务状态
12.2 Worker 设计
系统内部启动后台任务消费者，负责扫描和执行生成任务。
@Component
public class ReportGenerateWorker {

    @Scheduled(fixedDelay = 1000)
    public void pollTask() {
        // 1. 从 Redis 队列拉取任务
        // 2. 获取 Redis 分布式锁
        // 3. 校验任务状态
        // 4. 执行 Orchestrator
        // 5. 更新任务状态
        // 6. 记录阶段日志
    }
}
12.3 并发控制
暂时无法在飞书文档外展示此内容

---
13. 缓存设计
13.1 Redis 缓存内容
暂时无法在飞书文档外展示此内容
13.2 缓存一致性
1. 模板更新后删除模板缓存。
2. 用户权限变更后删除权限缓存。
3. Agent 配置变更后删除 Agent 缓存。
4. 知识库配置变更后删除知识库配置缓存。
5. 缓存只作为加速，不作为最终数据源。
6. 数据最终以 MySQL 为准。

---
14. 安全架构设计
14.1 认证与授权
系统支持 SSO 接入，后端使用 JWT 标识用户身份。
权限模型采用 RBAC。
暂时无法在飞书文档外展示此内容
14.2 数据安全
1. 全站 HTTPS。
2. 敏感字段脱敏展示。
3. 文件下载使用预签名 URL。
4. 不同部门数据逻辑隔离。
5. 重要操作写入审计日志。
6. Agent 请求中避免传递无关敏感信息。
7. 对外部 Agent 和知识库调用设置访问白名单。
8. 外部接口调用密钥统一放在配置中心或环境变量中，不写入代码。
14.3 审计范围
需要审计的操作包括：
1. 用户登录。
2. 上传模板。
3. 上传参考文档。
4. 创建报告。
5. 生成报告。
6. 下载报告。
7. 分享报告。
8. 删除报告。
9. 调用外部 Agent。
10. 调用外部知识库。

---
15. 可观测性设计
15.1 日志设计
系统日志分为：
暂时无法在飞书文档外展示此内容
15.2 TraceId 设计
每次请求生成 requestId。
每个报告生成任务生成 taskId。
Agent 调用和知识库调用都需要携带 requestId 和 taskId，方便全链路追踪。
requestId → reportId → taskId → agentCallId → fileId
15.3 监控指标
暂时无法在飞书文档外展示此内容

---
16. 异常与容错设计
16.1 异常分类
暂时无法在飞书文档外展示此内容
16.2 重试策略
暂时无法在飞书文档外展示此内容
16.3 幂等设计
以下接口需要支持幂等：
1. 提交报告生成任务。
2. 重试报告生成任务。
3. 文件上传完成回调。
4. Agent 调用结果写入。
5. 知识库检索结果写入。
6. 报告版本状态更新。

---
17. 部署架构设计
17.1 部署拓扑
                  ┌──────────────┐
                  │  Nginx       │
                  └──────┬───────┘
                         │
        ┌────────────────┴────────────────┐
        │                                 │
┌───────▼────────┐               ┌────────▼───────┐
│ Spring Boot #1 │               │ Spring Boot #2 │
│ DGP Backend    │               │ DGP Backend    │
└───────┬────────┘               └────────┬───────┘
        │                                 │
        ├──────────────┬──────────────────┤
        │              │                  │
        ▼              ▼                  ▼
   ┌────────┐     ┌────────┐        ┌──────────┐
   │ MySQL  │     │ Redis  │        │  MinIO   │
   └────────┘     └────────┘        └──────────┘
        │
        ▼
┌──────────────────────────────────────────┐
│ 外部 Agent / 外部知识库 / SSO              │
└──────────────────────────────────────────┘
17.2 部署说明
暂时无法在飞书文档外展示此内容
17.3 多副本注意事项
由于 Spring Boot 后端可以部署多个副本，所以需要注意：
1. 文件不能存在本地磁盘，必须进入 MinIO。
2. 任务锁不能使用本地锁，需要使用 Redis 分布式锁。
3. 定时任务需要防止多实例重复执行。
4. 用户 Session 不放本地内存，使用 JWT 或 Redis。
5. 报告生成任务状态以 MySQL 为准。
6. Worker 消费任务时必须做任务状态校验，防止重复执行。

---
18. 模块治理规范
为了保证模块化单体长期可维护，需要制定模块治理规范。
18.1 包依赖规范
1. common 可以被所有模块依赖。
2. 各业务模块之间不得直接访问对方 mapper。
3. 各业务模块之间通过 application 层接口或 facade 接口交互。
4. domain 层不得依赖 controller、mapper、infra。
5. infra 层负责封装技术细节，业务逻辑不得下沉到 infra 层。
18.2 代码规范
1. Controller 只做参数接收和响应封装。
2. Application Service 负责编排业务流程。
3. Domain 负责表达业务规则。
4. Mapper 只负责数据访问。
5. DTO、DO、Domain Object 不混用。
6. 外部接口请求和响应对象单独定义。
7. 所有关键操作必须记录操作日志。
18.3 配置规范
1. Agent 地址、密钥、超时时间通过配置管理。
2. 知识库地址、密钥、topK 默认值通过配置管理。
3. MinIO bucket、访问密钥通过配置管理。
4. Redis key 前缀统一管理。
5. 不同环境使用不同 Spring Profile，例如 dev、test、prod。

---
19. 架构风险与应对
暂时无法在飞书文档外展示此内容

---
20. 技术选型总结
暂时无法在飞书文档外展示此内容

---
21. 总结
本系统采用模块化单体架构，在单个 Spring Boot 应用内按照领域模块拆分，实现报告生成平台的核心能力。
该架构具备以下特点：
1. 开发和部署成本较低，适合项目一期快速落地。
2. 模块边界清晰，避免形成混乱单体。
3. Agent 能力和知识库能力均通过外部 HTTP 接口接入。
4. 平台本身专注于报告项目管理、任务编排、模板管理、文件管理、报告生命周期管理和审计追踪。
5. 通过 MySQL、Redis、MinIO 形成稳定的数据、缓存、队列和文件支撑。
6. 通过任务状态机、阶段日志、失败重试和幂等控制保障报告生成过程可控。
7. 通过 RBAC、审计日志、预签名 URL 和敏感字段保护保障系统安全。
因此，本架构能够满足报告生成平台一期建设需求，并为后续功能扩展、多行业接入和复杂报告生成场景提供稳定基础。