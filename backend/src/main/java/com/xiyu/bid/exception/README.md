# Exception Package (异常处理包)

> 一旦我所属的文件夹有所变化，请更新我。

## 功能作用

定义系统异常类，提供统一的异常处理机制。

## 文件清单

| 文件 | 地位 | 功能 |
|------|------|------|
| `BusinessException.java` | Exception | 业务异常基类 |
| `ResourceNotFoundException.java` | Exception | 资源未找到异常 |

## 异常层次

```
Throwable
└── Exception
    └── RuntimeException
        └── BusinessException
            ├── ResourceNotFoundException
            ├── ValidationException
            └── UnauthorizedException
```

## 使用方式

```java
// 抛出资源未找到异常
throw new ResourceNotFoundException("Project", projectId);

// 抛出业务异常
throw new BusinessException("操作失败: 原因说明");
```

## 全局异常处理

由 `@ControllerAdvice` 标注的类统一捕获并转换为 API 响应。
