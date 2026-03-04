# 文档组装模块 (Document Assembly Module)

## 概述

文档组装模块提供了基于模板的文档生成功能，支持变量占位符替换，使投标文档的生成更加高效和标准化。

## 功能特性

- **模板管理**: 创建、查询和管理文档模板
- **变量替换**: 支持 `${variableName}` 格式的变量占位符
- **文档组装**: 使用模板和变量值生成最终文档
- **历史记录**: 保存所有文档组装历史
- **重新生成**: 支持使用相同参数重新生成文档
- **分类管理**: 支持按分类组织模板

## 技术实现

### 实体类

#### AssemblyTemplate (文档模板)
```java
@Entity
@Table(name = "assembly_templates")
public class AssemblyTemplate {
    private Long id;              // 主键ID
    private String name;          // 模板名称
    private String description;   // 模板描述
    private String category;      // 模板分类
    private String templateContent; // 模板内容（支持占位符）
    private String variables;     // 变量定义（JSON格式）
    private Long createdBy;       // 创建人ID
    private LocalDateTime createdAt; // 创建时间
}
```

#### DocumentAssembly (组装记录)
```java
@Entity
@Table(name = "document_assemblies")
public class DocumentAssembly {
    private Long id;              // 主键ID
    private Long projectId;       // 项目ID
    private Long templateId;      // 使用的模板ID
    private String assembledContent; // 组装后的内容
    private String variables;     // 实际填充的变量值（JSON）
    private Long assembledBy;     // 组装人ID
    private LocalDateTime assembledAt; // 组装时间
}
```

### 核心服务

#### DocumentAssemblyService

主要方法：
- `createTemplate(request)` - 创建新模板
- `getTemplatesByCategory(category)` - 按分类查询模板
- `assembleDocument(projectId, templateId, variables, assembledBy)` - 组装文档
- `getAssembliesByProject(projectId)` - 查询项目的组装记录
- `regenerateAssembly(assemblyId)` - 重新生成文档
- `replaceVariables(templateContent, variablesJson)` - 替换模板变量

### API端点

| 方法 | 端点 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/documents/assembly/templates` | 获取模板列表 | ADMIN, MANAGER, STAFF |
| POST | `/api/documents/assembly/templates` | 创建新模板 | ADMIN, MANAGER |
| GET | `/api/documents/assembly/{projectId}` | 获取项目的组装记录 | ADMIN, MANAGER, STAFF |
| POST | `/api/documents/assembly/{projectId}/assemble` | 组装新文档 | ADMIN, MANAGER, STAFF |
| PUT | `/api/documents/assembly/{id}/regenerate` | 重新生成文档 | ADMIN, MANAGER, STAFF |

## 使用示例

### 1. 创建模板

```http
POST /api/documents/assembly/templates
Content-Type: application/json

{
  "name": "投标书模板",
  "description": "标准投标书模板",
  "category": "BIDDING_DOCUMENT",
  "templateContent": "尊敬的${招标方名称}：\n\n我方愿意参与${项目名称}的投标，报价为${报价金额}元。",
  "variables": "{\"招标方名称\":\"string\",\"项目名称\":\"string\",\"报价金额\":\"number\"}",
  "createdBy": 1
}
```

### 2. 组装文档

```http
POST /api/documents/assembly/100/assemble
Content-Type: application/json

{
  "templateId": 1,
  "variables": "{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\",\"报价金额\":500000}",
  "assembledBy": 1
}
```

响应：
```json
{
  "success": true,
  "code": 200,
  "message": "Document assembled successfully",
  "data": {
    "id": 1,
    "projectId": 100,
    "templateId": 1,
    "assembledContent": "尊敬的XX公司：\n\n我方愿意参与ABC项目的投标，报价为500000元。",
    "variables": "{\"招标方名称\":\"XX公司\",\"项目名称\":\"ABC项目\",\"报价金额\":500000}",
    "assembledBy": 1,
    "assembledAt": "2024-03-04T16:30:00"
  }
}
```

### 3. 查询项目组装记录

```http
GET /api/documents/assembly/100
```

### 4. 重新生成文档

```http
PUT /api/documents/assembly/1/regenerate
```

## 模板语法

模板支持简单的变量占位符语法：

- `${variableName}` - 变量占位符
- 变量值以JSON格式提供
- 支持字符串、数字等基本类型

示例模板：
```
合同编号：${合同编号}
甲方：${甲方名称}
乙方：${乙方名称}
签订日期：${签订日期}
合同金额：${合同金额}元
```

对应的变量JSON：
```json
{
  "合同编号": "HT2024001",
  "甲方名称": "XX公司",
  "乙方名称": "YY公司",
  "签订日期": "2024-03-04",
  "合同金额": 1000000
}
```

## 测试

### 单元测试覆盖

- **实体测试**: AssemblyTemplateTest, DocumentAssemblyTest
- **服务测试**: DocumentAssemblyServiceTest
- **总计**: 24个测试用例，100%通过

### 运行测试

```bash
# 运行所有文档组装模块测试
mvn test -Dtest="com.xiyu.bid.documents.**"

# 只运行单元测试
mvn test -Dtest="AssemblyTemplateTest,DocumentAssemblyTest,DocumentAssemblyServiceTest"
```

## 数据库表结构

### assembly_templates

```sql
CREATE TABLE assembly_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    template_content TEXT NOT NULL,
    variables TEXT,
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    INDEX idx_template_category (category),
    INDEX idx_template_created_by (created_by)
);
```

### document_assemblies

```sql
CREATE TABLE document_assemblies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    template_id BIGINT,
    assembled_content TEXT,
    variables TEXT,
    assembled_by BIGINT,
    assembled_at DATETIME NOT NULL,
    INDEX idx_assembly_project (project_id),
    INDEX idx_assembly_template (template_id),
    INDEX idx_assembly_project_template (project_id, template_id)
);
```

## 设计原则

1. **不可变性**: 创建新的组装记录而不是修改现有记录
2. **可追溯性**: 保存所有组装历史和使用的变量值
3. **灵活性**: 支持任意格式的模板和变量
4. **简单性**: 使用简单的占位符语法，易于理解和使用
5. **安全性**: 使用@Auditable注解记录所有关键操作

## 依赖关系

- 使用 `IAuditLogService` 进行审计日志记录
- 使用 `@Auditable` 注解标记需要审计的方法
- 继承项目的全局异常处理和API响应格式

## 扩展建议

1. **模板版本管理**: 添加模板版本字段，支持版本控制
2. **富文本编辑**: 集成富文本编辑器支持可视化模板编辑
3. **模板预览**: 添加模板预览功能
4. **批量生成**: 支持批量生成多个文档
5. **模板导入导出**: 支持模板的导入和导出功能
6. **变量验证**: 添加变量类型和必填项验证

## 相关文档

- [TDD实现总结](../../TDD_IMPLEMENTATION_SUMMARY.md)
- [费用模块](../fees/README.md)
- [合规模块](../compliance/README.md)
