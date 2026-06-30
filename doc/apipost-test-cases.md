# Apipost 测试用例

本文档用于在 Apipost 中测试当前项目已经实现的接口。

## 环境变量

在 Apipost 的环境中新增变量：

| 变量名 | 变量值 |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |

如果本地后端不是 8080 端口，请改成实际端口。

## 公共请求头

大多数请求都可以带上：

| Key | Value |
| --- | --- |
| `X-Request-Id` | 任意请求链路 ID，例如 `REQ-APIPOST-001` |

说明：

- `X-Request-Id` 用于链路追踪。
- 如果不传，后端会自动生成。
- `form-data` 请求不要手动设置 `Content-Type`，Apipost 会自动生成 multipart boundary。

## 1. 系统探活

### 接口用途

用于检查后端服务是否已经启动成功，适合作为第一个测试接口。

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/system/ping`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-001` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "ok"
  },
  "requestId": "REQ-APIPOST-001"
}
```

## 2. 上传文件

### 接口用途

用于上传文件到 MinIO，并在 MySQL 的 `file_object` 表中保存文件元数据。

可用于上传：

- 模板文件
- 参考文档
- 生成报告文件
- 预览文件
- 其他业务文件

当前阶段测试参考文档时，建议 `bizType` 使用 `REFERENCE`。

### Apipost 配置

- Method：`POST`
- URL：`{{baseUrl}}/api/files`
- Body 类型：`form-data`
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-FILE-001` |

### Body 参数

| 参数名 | Apipost 类型 | 必填 | 示例值 | 说明 |
| --- | --- | --- | --- | --- |
| `file` | `File` | 是 | 选择本地文件，例如 `evaluation-material.pdf` | 要上传的文件 |
| `bizType` | `Text` | 是 | `REFERENCE` | 文件业务类型 |
| `bizId` | `Text` | 否 | `20001` | 关联业务 ID |
| `objectName` | `Text` | 否 | 先不填 | 指定 MinIO 对象路径；不填由后端生成 |

注意：

- `file` 在 Apipost 中必须选择 `File` 类型。
- OpenAPI 中文件上传会显示为 `type: string`、`format: binary`，但在 Apipost 页面里仍然要选 `File`。
- 第一次测试建议不要勾选 `objectName`，让后端自动生成路径。

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "fileCode": "F...",
    "bizType": "REFERENCE",
    "bizId": 20001,
    "originalFilename": "evaluation-material.pdf",
    "objectName": "references/unknown/20001/F.../evaluation-material.pdf",
    "bucketName": "report-generate",
    "contentType": "application/pdf",
    "fileExt": "pdf",
    "fileSize": 204800,
    "fileHash": "sha256:...",
    "status": "ACTIVE"
  },
  "requestId": "REQ-APIPOST-FILE-001"
}
```

### 后置脚本

用于把返回的文件 ID 保存到 Apipost 环境变量，后续接口可以直接使用 `{{fileId}}`。

```javascript
const json = pm.response.json();
if (json.code === 0 && json.data) {
  pm.environment.set("fileId", json.data.id);
}
```

## 3. 查询文件详情

### 接口用途

用于根据文件 ID 查询文件元数据，例如文件名、业务类型、MinIO 对象路径、文件大小、文件 hash、状态等。

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/files/{{fileId}}`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-FILE-002` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "fileCode": "F...",
    "bizType": "REFERENCE",
    "status": "ACTIVE"
  },
  "requestId": "REQ-APIPOST-FILE-002"
}
```

## 4. 获取文件临时访问 URL

### 接口用途

用于根据文件 ID 获取一个临时访问 URL。

文件不会直接暴露 MinIO 真实地址，而是通过后端生成临时 URL，用于下载或预览。

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/files/{{fileId}}/access-url`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-FILE-003` |

### Query 参数

| 参数名 | 示例值 | 说明 |
| --- | --- | --- |
| `usage` | `DOWNLOAD` | 使用场景，当前可填 `DOWNLOAD` 或 `PREVIEW` |
| `expireSeconds` | `600` | URL 有效期，最大 3600 秒 |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "http://localhost:9000/...",
    "expireSeconds": 600,
    "usage": "DOWNLOAD"
  },
  "requestId": "REQ-APIPOST-FILE-003"
}
```

## 5. 上传模板

### 接口用途

用于上传 Word 模板文件，保存模板元数据，并解析模板中的变量占位符。

当前支持的占位符格式：

```text
${variable}
{{variable}}
```

示例 Word 内容：

```text
Project name: ${projectName}
Report date: {{reportDate}}
```

上传后，后端会解析出：

```text
projectName
reportDate
```

### Apipost 配置

- Method：`POST`
- URL：`{{baseUrl}}/api/templates`
- Body 类型：`form-data`
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-001` |

### Body 参数

| 参数名 | Apipost 类型 | 必填 | 示例值 | 说明 |
| --- | --- | --- | --- | --- |
| `file` | `File` | 是 | 选择 `template.docx` | Word 模板文件，必须是真实 `.docx` 文件 |
| `templateCode` | `Text` | 否 | `TPL_CRYPTO_001` | 模板编码，不填则后端生成 |
| `templateName` | `Text` | 是 | `Crypto Evaluation Template` | 模板名称 |
| `reportType` | `Text` | 是 | `CRYPTO` | 报告类型 |
| `versionNo` | `Text` | 否 | `1.0` | 版本号，不填默认 `1.0` |
| `description` | `Text` | 否 | `Template for crypto evaluation report` | 模板说明 |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "templateCode": "TPL_CRYPTO_001",
    "templateName": "Crypto Evaluation Template",
    "reportType": "CRYPTO",
    "versionNo": "1.0",
    "fileId": 2,
    "status": "DRAFT",
    "latest": true,
    "variables": [
      {
        "variableCode": "projectName",
        "variableName": "projectName",
        "variableType": "TEXT",
        "required": false,
        "sortNo": 1
      },
      {
        "variableCode": "reportDate",
        "variableName": "reportDate",
        "variableType": "TEXT",
        "required": false,
        "sortNo": 2
      }
    ]
  },
  "requestId": "REQ-APIPOST-TPL-001"
}
```

### 后置脚本

用于把返回的模板 ID 保存到 Apipost 环境变量，后续接口可以直接使用 `{{templateId}}`。

```javascript
const json = pm.response.json();
if (json.code === 0 && json.data) {
  pm.environment.set("templateId", json.data.id);
}
```

## 6. 查询模板列表

### 接口用途

用于分页查询模板列表。

支持按以下条件筛选：

- 报告类型 `reportType`
- 模板状态 `status`
- 关键字 `keyword`
- 页码 `pageNum`
- 每页大小 `pageSize`

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/templates`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-002` |

### Query 参数

| 参数名 | 示例值 | 说明 |
| --- | --- | --- |
| `reportType` | `CRYPTO` | 报告类型 |
| `status` | `DRAFT` | 模板状态，可填 `DRAFT`、`ENABLED`、`DISABLED` |
| `keyword` | `Crypto` | 关键字，匹配模板编码、模板名称、说明 |
| `pageNum` | `1` | 页码 |
| `pageSize` | `10` | 每页数量，最大 100 |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "templateCode": "TPL_CRYPTO_001",
        "templateName": "Crypto Evaluation Template",
        "reportType": "CRYPTO",
        "status": "DRAFT"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  },
  "requestId": "REQ-APIPOST-TPL-002"
}
```

## 7. 查询模板详情

### 接口用途

用于根据模板 ID 查询模板详情，包括模板元信息、模板文件 ID 和变量列表。

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/templates/{{templateId}}`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-003` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "templateCode": "TPL_CRYPTO_001",
    "templateName": "Crypto Evaluation Template",
    "reportType": "CRYPTO",
    "status": "DRAFT"
  },
  "requestId": "REQ-APIPOST-TPL-003"
}
```

## 8. 查询模板变量

### 接口用途

用于查询某个模板解析出来的变量清单。

前端后续可以根据这个变量清单动态渲染表单。

### Apipost 配置

- Method：`GET`
- URL：`{{baseUrl}}/api/templates/{{templateId}}/variables`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-004` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "variableCode": "projectName",
      "variableName": "projectName",
      "variableType": "TEXT",
      "required": false,
      "sortNo": 1
    },
    {
      "variableCode": "reportDate",
      "variableName": "reportDate",
      "variableType": "TEXT",
      "required": false,
      "sortNo": 2
    }
  ],
  "requestId": "REQ-APIPOST-TPL-004"
}
```

## 9. 启用模板

### 接口用途

用于将模板状态从 `DRAFT` 或 `DISABLED` 设置为 `ENABLED`。

只有启用状态的模板后续才应该被报告生成流程选择使用。

### Apipost 配置

- Method：`POST`
- URL：`{{baseUrl}}/api/templates/{{templateId}}/enable`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-005` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "requestId": "REQ-APIPOST-TPL-005"
}
```

### 验证方式

再请求：

- Method：`GET`
- URL：`{{baseUrl}}/api/templates/{{templateId}}`

预期：

```text
data.status = ENABLED
```

## 10. 停用模板

### 接口用途

用于将模板状态设置为 `DISABLED`。

停用模板不影响历史报告，但后续报告生成不应再选择这个模板。

### Apipost 配置

- Method：`POST`
- URL：`{{baseUrl}}/api/templates/{{templateId}}/disable`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-TPL-006` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "requestId": "REQ-APIPOST-TPL-006"
}
```

### 验证方式

再请求：

- Method：`GET`
- URL：`{{baseUrl}}/api/templates/{{templateId}}`

预期：

```text
data.status = DISABLED
```

## 11. 逻辑删除文件

### 接口用途

用于逻辑删除文件元数据。

说明：

- 当前接口只更新 MySQL 元数据状态。
- 不会立即物理删除 MinIO 中的对象。
- 物理文件清理后续可由定时任务处理。

### Apipost 配置

- Method：`DELETE`
- URL：`{{baseUrl}}/api/files/{{fileId}}`
- Body：无
- Headers：

| Key | Value |
| --- | --- |
| `X-Request-Id` | `REQ-APIPOST-FILE-004` |

### 预期响应

```json
{
  "code": 0,
  "message": "success",
  "data": null,
  "requestId": "REQ-APIPOST-FILE-004"
}
```

### 验证方式

删除后再请求：

- Method：`GET`
- URL：`{{baseUrl}}/api/files/{{fileId}}`

预期：

```json
{
  "code": 4041,
  "message": "file object not found",
  "data": null,
  "requestId": "REQ-APIPOST-FILE-005"
}
```

## 推荐测试顺序

1. `GET /api/system/ping`
2. `POST /api/files`
3. `GET /api/files/{{fileId}}`
4. `GET /api/files/{{fileId}}/access-url`
5. `POST /api/templates`
6. `GET /api/templates`
7. `GET /api/templates/{{templateId}}`
8. `GET /api/templates/{{templateId}}/variables`
9. `POST /api/templates/{{templateId}}/enable`
10. `POST /api/templates/{{templateId}}/disable`
11. `DELETE /api/files/{{fileId}}`

## 常见问题

### 1. 上传文件返回 500

优先检查：

- MySQL 是否启动
- MinIO 是否启动
- `file_object` 表是否存在
- `MINIO_BUCKET` 对应 bucket 是否创建
- Apipost 中 `file` 参数是否选择了 `File` 类型

### 2. 上传文件时 objectName 怎么填

第一次测试建议不要填 `objectName`，让后端自动生成。

如果手动填，建议填类似：

```text
references/test/20001/demo.pdf
```

不要随便填 `111` 这种不带目录和扩展名的值，虽然技术上可能能上传，但不利于后续排查。

### 3. 上传模板解析失败

检查：

- 文件必须是真实 `.docx`，不是 `.doc`，也不是重命名出来的 `.docx`
- 占位符必须是 `${variable}` 或 `{{variable}}`
- 变量名当前建议只使用英文、数字、下划线、点、短横线

### 4. 模板变量为空

检查 Word 模板里是否真的包含：

```text
${projectName}
{{reportDate}}
```

如果使用了复杂 Word 域、文本框、图片里的文字，当前一期解析器可能不会识别。
