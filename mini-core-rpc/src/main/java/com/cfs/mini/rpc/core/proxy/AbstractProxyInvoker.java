package com.cfs.mini.rpc.core.proxy;

import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;


public abstract class AbstractProxyInvoker<T> implements Invoker<T> {

    /**
     * 代理的对象，一般是 Service 实现对象
     */
    private final T proxy;
    /**
     * 接口类型，一般是 Service 接口
     */
    private final Class<T> type;
    /**
     * URL 对象，一般是暴露服务的 URL 对象
     */
    private final URL url;

    public AbstractProxyInvoker(T proxy, Class<T> type, URL url) {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy == null");
        }
        if (type == null) {
            throw new IllegalArgumentException("interface == null");
        }
        if (!type.isInstance(proxy)) { //
            throw new IllegalArgumentException(proxy.getClass().getName() + " not implement interface " + type);
        }
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    public Class<T> getInterface() {
        return type;
    }

    public URL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
    }

    public Result invoke(Invocation invocation) throws RpcException {

            throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: ");

    }

    /**
     * 执行调用
     *
     * @param proxy 代理的对象
     * @param methodName 方法名
     * @param parameterTypes 方法参数类型数组
     * @param arguments 方法参数数组
     * @return 调用结果
     * @throws Throwable 发生异常
     */
    protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable;

    @Override
    public String toString() {
        return getInterface() + " -> " + (getUrl() == null ? " " : getUrl().toString());
    }


}
