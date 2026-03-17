# Document Editor Module (文档编辑器模块)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

提供文档结构的可视化管理，支持章节的树形组织、拖拽排序和层级管理，用于投标文档的结构化编辑。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `DocumentStructure.java` | Entity | 文档结构实体 |
| `DocumentSection.java` | Entity | 文档章节实体 |
| `SectionType.java` | Enum | 章节类型 (CHAPTER, SECTION, SUBSECTION, TABLE, IMAGE, ATTACHMENT) |
| `DocumentStructureRepository.java` | Repository | 文档结构数据访问层 |
| `DocumentSectionRepository.java` | Repository | 章节数据访问层 |
| `DocumentEditorService.java` | Service | 文档编辑业务逻辑层 |
| `DocumentEditorController.java` | Controller | REST API 端点 |
| `DocumentStructureDTO.java` | DTO | 文档结构数据传输对象 |
| `DocumentSectionDTO.java` | DTO | 章节数据传输对象 |
| `StructureCreateRequest.java` | DTO | 创建结构请求 |
| `SectionCreateRequest.java` | DTO | 创建章节请求 |
| `SectionUpdateRequest.java` | DTO | 更新章节请求 |
| `SectionReorderRequest.java` | DTO | 章节排序请求 |

## API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/documents/{id}/editor/structure` | 获取文档结构 |
| POST | `/api/documents/{id}/editor/structure` | 创建文档结构 |
| POST | `/api/documents/{id}/editor/sections` | 添加章节 |
| PUT | `/api/documents/{id}/editor/sections/{sectionId}` | 更新章节 |
| DELETE | `/api/documents/{id}/editor/sections/{sectionId}` | 删除章节 |
| POST | `/api/documents/{id}/editor/reorder` | 章节排序 |
| GET | `/api/documents/{id}/editor/tree` | 获取树形结构 |

## 特性

- **树形结构**: 支持多层级章节嵌套
- **拖拽排序**: 支持章节拖拽重新排序
- **多种类型**: 章节、表格、图片、附件等多种内容类型
