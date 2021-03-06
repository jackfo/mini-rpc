package com.cfs.mini.remoting.exchange.support.header;

import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.exchange.ExchangeChannel;
import com.cfs.mini.remoting.exchange.ExchangeHandler;
import com.cfs.mini.remoting.exchange.Request;
import com.cfs.mini.remoting.exchange.suport.DefaultFuture;
import com.cfs.mini.remoting.transport.ChannelHandlerDelegate;

public class HeaderExchangeHandler implements ChannelHandlerDelegate {

    public static String KEY_READ_TIMESTAMP = HeartbeatHandler.KEY_READ_TIMESTAMP;

    public static String KEY_WRITE_TIMESTAMP = HeartbeatHandler.KEY_WRITE_TIMESTAMP;

    private final ExchangeHandler handler;

    public HeaderExchangeHandler(ExchangeHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.handler = handler;
    }


    @Override
    public ChannelHandler getHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        System.out.println("建立连接");
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
        channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());

        ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
        try {
            // 提交给装饰的 `handler`，继续处理
            handler.connected(exchangeChannel);
        } finally {
            // 移除 ExchangeChannel 对象，若已断开
            HeaderExchangeChannel.removeChannelIfDisconnected(channel);
        }
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        System.out.println("取消连接");

    }


    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        Throwable exception = null;
        try{
            // 设置最后的写时间
            channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
            ExchangeChannel exchangeChannel = HeaderExchangeChannel.getOrAddChannel(channel);
            try {
                // 提交给装饰的 `handler`，继续处理
                handler.sent(exchangeChannel, message);
            } finally {
                // 移除 ExchangeChannel 对象，若已断开
                HeaderExchangeChannel.removeChannelIfDisconnected(channel);
            }
        } catch (Throwable t) {
            exception = t; // 记录异常，等下面在抛出。其实，这里可以写成 finally 的方式。
        }
        // 若是请求，标记已发送
        if (message instanceof Request) {
            Request request = (Request) message;
            DefaultFuture.sent(channel, request);
        }

        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof RemotingException) {
                throw (RemotingException) exception;
            } else {
                throw new RemotingException(channel.getLocalAddress(), channel.getRemoteAddress(),
                        exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
       System.out.println("接受消息");
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        System.out.println("出现异常");
    }
}
