// Input: 业务失败、资源缺失和参数校验异常
// Output: 业务异常类型与标准化错误映射
// Pos: Exception/异常处理层
// 维护声明: 仅维护异常语义与映射；错误码改动请同步前后端契约.
package com.xiyu.bid.exception;

/**
 * 业务异常
 * 用于处理业务逻辑中的错误情况.
 *
 * <p>Carries an optional machine-readable {@link ErrorCode} so
 * downstream modules can translate failures without substring
 * matching on user-facing messages.
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final ErrorCode errorCode;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorCode = null;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    /**
     * Construct with an explicit {@link ErrorCode}. HTTP code defaults
     * to 400 (legacy behavior) unless the caller also supplies one.
     *
     * @param errorCode machine-readable error code (non-null)
     * @param message   human-readable message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = 400;
        this.errorCode = errorCode;
    }

    public int getCode() {
        return code;
    }

    /**
     * Machine-readable error code.
     *
     * @return error code, or {@code null} if this exception was raised
     *         without one (legacy call sites)
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
