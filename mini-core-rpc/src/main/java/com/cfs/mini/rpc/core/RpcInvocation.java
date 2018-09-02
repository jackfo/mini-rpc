package com.cfs.mini.rpc.core;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RpcInvocation implements Invocation, Serializable {

    private static final long serialVersionUID = -4355285085441097045L;

    /**
     * 方法名
     */
    private String methodName;
    /**
     * 方法参数类型数组 {@link Method#getParameterTypes()}
     */
    private Class<?>[] parameterTypes;
    /**
     * 方法参数数组
     */
    private Object[] arguments;
    /**
     * 隐式参数集合
     */
    private Map<String, String> attachments;
    /**
     * Invoker 对象
     *
     * 不序列化
     */
    private transient Invoker<?> invoker;

    public RpcInvocation() {
    }

    public RpcInvocation(Invocation invocation, Invoker<?> invoker) {
        this(invocation.getMethodName(), invocation.getParameterTypes(),
                invocation.getArguments(), new HashMap<String, String>(invocation.getAttachments()),
                invocation.getInvoker());
        //TODO:需要添加的东西
    }

    public RpcInvocation(Invocation invocation) {
        this(invocation.getMethodName(), invocation.getParameterTypes(),
                invocation.getArguments(), invocation.getAttachments(), invocation.getInvoker());
    }

    public RpcInvocation(Method method, Object[] arguments) {
        this(method.getName(), method.getParameterTypes(), arguments, null, null);
    }

    public RpcInvocation(Method method, Object[] arguments, Map<String, String> attachment) {
        this(method.getName(), method.getParameterTypes(), arguments, attachment, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        this(methodName, parameterTypes, arguments, null, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments, Map<String, String> attachments) {
        this(methodName, parameterTypes, arguments, attachments, null);
    }

    public RpcInvocation(String methodName, Class<?>[] parameterTypes, Object[] arguments, Map<String, String> attachments, Invoker<?> invoker) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes == null ? new Class<?>[0] : parameterTypes;
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.attachments = attachments == null ? new HashMap<String, String>() : attachments;
        this.invoker = invoker;
    }

    @Override
    public String getMethodName() {
        return null;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return new Class[0];
    }

    @Override
    public Object[] getArguments() {
        return new Object[0];
    }

    @Override
    public Map<String, String> getAttachments() {
        return null;
    }

    @Override
    public String getAttachment(String key) {
        return null;
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return null;
    }

    @Override
    public Invoker<?> getInvoker() {
        return null;
    }
}
