package com.cfs.mini.remoting.transport;


import com.cfs.mini.common.Constants;
import com.cfs.mini.common.Parameters;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.extension.ExtensionLoader;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.ExecutorUtil;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Client;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.transport.dispatcher.ChannelHandlers;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractClient extends AbstractEndpoint implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    protected static final String CLIENT_THREAD_POOL_NAME = "MiniClientHandler";

    private static final AtomicInteger CLIENT_THREAD_POOL_ID = new AtomicInteger();

    /**连接锁*/
    private final Lock connectLock = new ReentrantLock();

    public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        try{
            doOpen();
        } catch (Throwable t) {
            throw new RemotingException(url.toInetSocketAddress(), null,
                    "Failed to start " + getClass().getSimpleName() + " " + NetUtils.getLocalAddress()
                            + " connect to the server " + getRemoteAddress() + ", cause: " + t.getMessage(), t);
        }
        try{
            connect();
        }catch (RemotingException e){
            logger.error("[失败进行连接]"+e.getMessage());
        }


    }

    protected abstract void doOpen() throws Throwable;


    protected void connect() throws RemotingException {
        connectLock.lock();
        try{
            if(isConnected()){
                return;
            }
            //TODO:初始化重连线程
            doConnect();
        }catch (Throwable t){
            if(!isConnected()){
                throw new RuntimeException("连接失败"+t.getMessage());
            }
        }finally {
            // 释放锁
            connectLock.unlock();
        }
    }

    @Override
    public void reconnect() throws RemotingException {

    }

    @Override
    public void reset(Parameters parameters) {

    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public boolean isConnected() {
        Channel channel = getChannel();
        return channel != null && channel.isConnected();
    }

    @Override
    public boolean hasAttribute(String key) {
        return false;
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void setAttribute(String key, Object value) {

    }

    @Override
    public void removeAttribute(String key) {

    }

    protected abstract Channel getChannel();

    /**做一个线程连接*/
    protected abstract void doConnect() throws Throwable;

    /**初始化重连线程*/
    private synchronized void initConnectStatusCheckCommand() {


    }

    public InetSocketAddress getConnectAddress() {
        return new InetSocketAddress(NetUtils.filterLocalHost(getUrl().getHost()), getUrl().getPort());
    }

    /**
     * 包装之后的通道句柄
     * */
    protected static ChannelHandler wrapChannelHandler(URL url, ChannelHandler handler) {
        // 设置线程名
        url = ExecutorUtil.setThreadName(url, CLIENT_THREAD_POOL_NAME);
        // 设置使用的线程池类型
        url = url.addParameterIfAbsent(Constants.THREADPOOL_KEY, Constants.DEFAULT_CLIENT_THREADPOOL);
        // 包装通道处理器
        return ChannelHandlers.wrap(handler, url);
    }
}
