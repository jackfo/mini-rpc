package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;

/**
 * 失败则自动切换
 *
 * */
public class AvailableCluster {

    public static final String NAME = "available";

    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new AvailableClusterInvoker<T>(directory);
    }
}
