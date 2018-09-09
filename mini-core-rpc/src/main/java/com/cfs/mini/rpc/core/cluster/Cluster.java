package com.cfs.mini.rpc.core.cluster;

import com.cfs.mini.common.extension.Adaptive;
import com.cfs.mini.common.extension.SPI;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.support.FailoverCluster;

@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * Merge the directory invokers to a virtual invoker.
     *
     * 基于 Directory ，创建 Invoker 对象，实现统一、透明的 Invoker 调用过程
     *
     * @param directory Directory 对象
     * @param <T>  泛型
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;

}