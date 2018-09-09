package com.cfs.mini.rpc.core.cluster;

import com.cfs.mini.common.URL;
import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.RpcException;

import java.util.List;

public interface Router extends Comparable<Router>{

    URL getUrl();

    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
}
