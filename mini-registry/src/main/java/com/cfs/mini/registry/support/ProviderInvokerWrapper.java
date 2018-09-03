package com.cfs.mini.registry.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;

public class ProviderInvokerWrapper<T> implements Invoker {

    /**
     * Invoker 对象
     */
    private Invoker<T> invoker;
    /**
     * 原始 URL
     */
    private URL originUrl;
    /**
     * 注册中心 URL
     */
    private URL registryUrl;
    /**
     * 服务提供者 URL
     */
    private URL providerUrl;
    /**
     * 是否注册
     */
    private volatile boolean isReg;

    public ProviderInvokerWrapper(Invoker<T> invoker, URL registryUrl, URL providerUrl) {
        this.invoker = invoker;
        this.originUrl = URL.valueOf(invoker.getUrl().toFullString());
        this.registryUrl = URL.valueOf(registryUrl.toFullString());
        this.providerUrl = providerUrl;
    }

    public Class<T> getInterface() {
        return invoker.getInterface();
    }

    public URL getUrl() {
        return invoker.getUrl();
    }

    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    public Result invoke(Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    public void destroy() {
        invoker.destroy();
    }

    public URL getOriginUrl() {
        return originUrl;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public URL getProviderUrl() {
        return providerUrl;
    }

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public boolean isReg() {
        return isReg;
    }

    public void setReg(boolean reg) {
        isReg = reg;
    }
}
