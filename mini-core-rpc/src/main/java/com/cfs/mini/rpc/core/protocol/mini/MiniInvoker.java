package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.exchange.ExchangeClient;
import com.cfs.mini.rpc.core.*;
import com.cfs.mini.rpc.core.protocol.AbstractInvoker;
import com.cfs.mini.rpc.core.support.RpcUtils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MiniInvoker<T> extends AbstractInvoker<T> {

    /**
     * 远程通信客户端数组
     */
    private final ExchangeClient[] clients;

    private final AtomicInteger index = new AtomicInteger();
    /**
     * 版本
     */
    private final String version;
    /**
     * 销毁锁
     *
     * 在 {@link #destroy()} 中使用
     */
    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public MiniInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients) {
        this(serviceType, url, clients, null);
    }

    public MiniInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{Constants.INTERFACE_KEY, Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
        this.invokers = invokers;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {

        RpcInvocation inv = (RpcInvocation) invocation;

        final String methodName = RpcUtils.getMethodName(invocation);

        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);

        /**获取超时时间*/
        int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);

        ExchangeClient currentClient;
        if(clients.length==1){
            currentClient = clients[0];
        }else{
            currentClient = clients[index.getAndIncrement() % clients.length];
        }

        /**开始进行远程调用*/
        try{
            return (Result) currentClient.request(inv, timeout).get();
        } catch (Exception e) {
           e.printStackTrace();
           throw new RpcException("远程调用出现异常");
        }
    }


    @Override
    public boolean isAvailable() {
        if (!super.isAvailable())
            return false;
        for (ExchangeClient client : clients) {
            if (client.isConnected() && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY)) { // 只读判断
                //cannot write == not Available ?
                return true;
            }
        }
        return false;
    }


}
