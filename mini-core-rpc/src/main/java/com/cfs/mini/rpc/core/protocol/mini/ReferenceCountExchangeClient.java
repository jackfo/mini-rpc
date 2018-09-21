package com.cfs.mini.rpc.core.protocol.mini;

import com.cfs.mini.common.Parameters;
import com.cfs.mini.common.URL;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.ExchangeClient;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

final class ReferenceCountExchangeClient implements ExchangeClient {

    private final URL url;

    /**指向引用数量*/
    private final AtomicInteger refenceCount = new AtomicInteger(0);


    /**连接客户端*/
    private ExchangeClient client;

    public ReferenceCountExchangeClient(ExchangeClient client) {
        this.client = client;
        // 指向加一
        refenceCount.incrementAndGet();
        this.url = client.getUrl();
    }

    @Override
    public URL getUrl() {
        return client.getUrl();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return client.getRemoteAddress();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return client.getChannelHandler();
    }



    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return client.getLocalAddress();
    }

    @Override
    public boolean hasAttribute(String key) {
        return client.hasAttribute(key);
    }

    @Override
    public void reset(Parameters parameters) {
        client.reset(parameters);
    }

    @Override
    public void send(Object message) throws RemotingException {
        client.send(message);
    }

    @Override
    public ExchangeHandler getExchangeHandler() {
        return client.getExchangeHandler();
    }

    @Override
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        return null;
    }

    @Override
    public ResponseFuture request(Object request) throws RemotingException {
        return null;
    }

    @Override
    public Object getAttribute(String key) {
        return client.getAttribute(key);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        client.send(message, sent);
    }

    @Override
    public void setAttribute(String key, Object value) {
        client.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        client.removeAttribute(key);
    }

    /**
     * close() is not idempotent any longer
     */
    public void close() {
        close(0);
    }

    @Override
    public void close(int timeout) {
        // 防止client被关闭多次. 在 connect per jvm 的情况下，client.close 方法会调用计数器-1，当计数器小于等于0的情况下，才真正关闭
        if (refenceCount.decrementAndGet() <= 0) {
            // 关闭 `client`
            if (timeout == 0) {
                client.close();
            } else {
                client.close(timeout);
            }
            //TODO:关闭幽灵客户端
        }
    }

    @Override
    public void startClose() {
        client.startClose();
    }



    @Override
    public boolean isClosed() {
        return client.isClosed();
    }

    /**
     * 增加计数
     */
    public void incrementAndGetCount() {
        refenceCount.incrementAndGet();
    }

    @Override
    public void reset(URL url) {
        throw new RuntimeException("reset异常");
    }
}
