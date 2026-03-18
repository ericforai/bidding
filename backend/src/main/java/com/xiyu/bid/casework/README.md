> 一旦我所属的文件夹有所变化，请更新我。

# Casework 模块

案例模块负责案例引用和案例分享记录，支撑知识资产在投标流程中的复用追踪。
该目录只管理案例引用、分享和查询记录，不承载案例正文编辑。
对外提供引用与分享记录的领域边界。

| 文件 | 地位 | 功能 |
|------|------|------|
| `entity/` | 子目录 | 案例记录实体边界 |
| `entity/CaseReferenceRecord.java` | Entity | 案例引用记录实体 |
| `entity/CaseShareRecord.java` | Entity | 案例分享记录实体 |
| `repository/` | 子目录 | 案例记录数据访问边界 |
| `repository/CaseReferenceRecordRepository.java` | Repository | 案例引用记录访问 |
| `repository/CaseShareRecordRepository.java` | Repository | 案例分享记录访问 |
| `dto/` | 子目录 | 案例记录传输边界 |
| `dto/CaseReferenceRecordDTO.java` | DTO | 案例引用记录传输对象 |
| `dto/CaseShareRecordDTO.java` | DTO | 案例分享记录传输对象 |
