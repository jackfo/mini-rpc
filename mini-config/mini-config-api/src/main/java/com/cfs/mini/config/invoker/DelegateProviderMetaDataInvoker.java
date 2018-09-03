package com.cfs.mini.config.invoker;

import com.cfs.mini.common.URL;
import com.cfs.mini.config.ServiceConfig;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;

public class DelegateProviderMetaDataInvoker<T> implements Invoker {


    protected final Invoker<T> invoker;

    private ServiceConfig metadata;

    public DelegateProviderMetaDataInvoker(Invoker<T> invoker,ServiceConfig metadata) {
        this.invoker = invoker;
        this.metadata = metadata;
    }

    @Override
    public Class getInterface() {
        return invoker.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
       return invoker.invoke(invocation);
    }

    @Override
    public URL getUrl() {
        return invoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return invoker.isAvailable();
    }

    @Override
    public void destroy() {
        invoker.destroy();
    }

    public ServiceConfig getMetadata() {
        return metadata;
    }
}
