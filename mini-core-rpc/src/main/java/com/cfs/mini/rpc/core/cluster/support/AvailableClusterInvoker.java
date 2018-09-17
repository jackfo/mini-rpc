package com.cfs.mini.rpc.core.cluster.support;

import com.cfs.mini.rpc.core.Invocation;
import com.cfs.mini.rpc.core.Invoker;
import com.cfs.mini.rpc.core.Result;
import com.cfs.mini.rpc.core.RpcException;
import com.cfs.mini.rpc.core.cluster.Directory;
import com.cfs.mini.rpc.core.cluster.LoadBalance;

import java.util.List;


/**
 * 找到第一个可利用的Invoker进行RPC调用
 * */
public class AvailableClusterInvoker<T> extends AbstractClusterInvoker<T> {


    public AvailableClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    public Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {

        /**调用首个可利用的invoker*/
        for(Invoker<T> invoker:invokers){
            if(invoker.isAvailable()){
                return invoker.invoke(invocation);
            }
        }

        throw new RpcException("没有可以利用的服务提供者");
    }
}
