package com.cfs.mini.rpc.core.service;

import com.cfs.mini.common.utils.StringUtils;

public class GenericException extends RuntimeException {

    private static final long serialVersionUID = -1182299763306599962L;

    private String exceptionClass;

    private String exceptionMessage;

    public GenericException() {
    }

    public GenericException(String exceptionClass, String exceptionMessage) {
        super(exceptionMessage);
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    public GenericException(Throwable cause) {
        super(StringUtils.toString(cause));
        this.exceptionClass = cause.getClass().getName();
        this.exceptionMessage = cause.getMessage();
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

}

