package com.cfs.mini.rpc.core.service;

public interface GenericService {

    /**
     * Generic invocation
     *
     * 泛化调用
     *
     * @param method         Method name, e.g. findPerson. If there are overridden methods, parameter info is
     *                       required, e.g. findPerson(java.lang.String)
     *                       方法名
     * @param parameterTypes Parameter types
     *                       参数类型数组
     * @param args           Arguments
     *                       参数数组
     * @return invocation return value 调用结果
     * @throws Throwable potential exception thrown from the invocation
     */
    Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;

}