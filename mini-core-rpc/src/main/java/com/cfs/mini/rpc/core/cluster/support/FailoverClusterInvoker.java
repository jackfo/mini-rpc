package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;

public class FailoverClusterInvoker<T> extends  AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);


    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    public Class<T> getInterface() {
        return null;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        return null;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }
}
