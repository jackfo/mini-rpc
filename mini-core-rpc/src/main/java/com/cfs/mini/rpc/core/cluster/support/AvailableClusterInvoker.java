package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;

public class AvailableClusterInvoker<T> extends AbstractClusterInvoker<T> {


    public AvailableClusterInvoker(Directory<T> directory) {
        super(directory);
    }
}
