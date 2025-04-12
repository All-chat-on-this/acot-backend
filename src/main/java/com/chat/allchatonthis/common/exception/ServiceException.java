package com.chat.allchatonthis.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Service layer exception
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code
     */
    private Integer code;

    /**
     * Error message
     */
    private String message;

    /**
     * Error data
     */
    private Object data;

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public ServiceException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
