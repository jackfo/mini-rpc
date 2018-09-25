package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.ExchangeChannel;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.exchange.Request;
import com.cfs.mini.remoting.exchange.ResponseFuture;
import com.cfs.mini.remoting.exchange.suport.DefaultFuture;

import java.net.InetSocketAddress;

/**
 * 交换通道
 * */
public class HeaderExchangeChannel implements ExchangeChannel {

    /**是否关闭*/
    private volatile boolean closed = false;

    private final Channel channel;

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeChannel.class);

    HeaderExchangeChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel == null");
        }
        this.channel = channel;
    }


    @Override
    public ExchangeHandler getExchangeHandler() {
        return null;
    }

    @Override
    public ResponseFuture request(Object request) throws RemotingException {
        return request(request, channel.getUrl().getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
    }


    @Override
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        if (closed) {
            throw new RemotingException(this.getLocalAddress(), null, "Failed to send request " + request + ", cause: The channel " + this + " is closed!");
        }
        // create request. 创建请求
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true); // 需要响应
        req.setData(request);
        // 创建 DefaultFuture 对象
        DefaultFuture future = new DefaultFuture(channel, req, timeout);
        try {
            // 发送请求
            channel.send(req);
        } catch (RemotingException e) { // 发生异常，取消 DefaultFuture
            future.cancel();
            throw e;
        }
        // 返回 DefaultFuture 对象
        return future;
    }


    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
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

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public void send(Object message) throws RemotingException {

    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {

    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public void startClose() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
