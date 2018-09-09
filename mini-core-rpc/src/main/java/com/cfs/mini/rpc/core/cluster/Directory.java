package com.cfs.mini.rpc.core.cluster;

import com.cfs.mini.common.Node;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;

import java.util.List;

public interface Directory<T> extends Node {

    /**获取服务接口名*/
    Class<T> getInterface();

    List<Invoker<T>> list(Invocation invocation) throws RpcException;

}
