package com.xiyu.bid.exception;

/**
 * 资源不存在异常
 * 当请求的资源未找到时抛出
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final String resourceId;

    public ResourceNotFoundException(String resource, String resourceId) {
        super(String.format("%s not found: %s", resource, resourceId));
        this.resource = resource;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resource = "unknown";
        this.resourceId = null;
    }

    public String getResource() {
        return resource;
    }

    public String getResourceId() {
        return resourceId;
    }
}
