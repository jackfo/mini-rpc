package com.cfs.mini.rpc.core.cluster.wrapper;

import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Cluster;
import com.cfs.mini.rpc.core.cluster.Directory;

public class MockClusterWrapper implements Cluster {

    /**
     * 真正的 Cluster 对象
     */
    private Cluster cluster;

    public MockClusterWrapper(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new MockClusterInvoker<T>(directory,
                this.cluster.join(directory));
    }

}