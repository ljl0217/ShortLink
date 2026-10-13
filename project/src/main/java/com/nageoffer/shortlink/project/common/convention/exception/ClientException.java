package com.nageoffer.shortlink.project.common.convention.exception;

import com.nageoffer.shortlink.project.common.convention.errorcode.BaseErrorCode;
import com.nageoffer.shortlink.project.common.convention.errorcode.IErrorCode;

/**
 * 客户端异常
 */
public class ClientException extends AbstractException {

    public ClientException(IErrorCode iErrorCode) {
        this(null, null, iErrorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode iErrorCode) {
        this(message, null, iErrorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode iErrorCode) {
        super(message, throwable, iErrorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + '\'' +
                ", message='" + errorMessage + '\'' +
                '}';
    }
}
