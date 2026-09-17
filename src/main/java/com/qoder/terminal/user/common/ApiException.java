package com.qoder.terminal.user.common;

import org.springframework.http.HttpStatus;

/** 业务错误，统一映射为 {"code","message"} 响应。 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
