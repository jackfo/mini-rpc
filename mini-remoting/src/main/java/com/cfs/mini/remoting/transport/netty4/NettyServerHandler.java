package com.cfs.mini.remoting.transport.netty4;

import com.cfs.mini.common.URL;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.remoting.Channel;
import com.cfs.mini.remoting.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyServerHandler extends ChannelDuplexHandler {

    public static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**通道集合*/
    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

    private final URL url;

    private final ChannelHandler handler;

    private static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    private static final AtomicInteger CONNECTION_COUNT = new AtomicInteger(0);
    private static final AtomicInteger READ_COUNT = new AtomicInteger(0);


    public NettyServerHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    /**获取所有通道*/
    public Map<String, Channel> getChannels() {
        return channels;
    }

    /**
     * 进行通道激活
     * */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        logger.info(String.format("激活次数:%d",ACTIVE_COUNT.incrementAndGet()),"服务端通道激活");

        ctx.fireChannelActive();
        //获取RPC中对应的通道
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);

        try{
            if(channel!=null){
                channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
            }
            // 提交给 `handler` 处理器。
            handler.connected(channel);
        }finally {
            //移除所有已经断开的连接对象
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future) {
        logger.info(String.format("断开次数:%d",ACTIVE_COUNT.incrementAndGet()),"服务端通道断开");
        // 因为没有请求从远端断开 Channel
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info(String.format("激活读取:%d",ACTIVE_COUNT.incrementAndGet()),"服务端通道读取");
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            handler.received(channel, msg);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logger.info(String.format("写入次数:%d",ACTIVE_COUNT.incrementAndGet()),"服务端通道写入");
        // 发送消息
        super.write(ctx, msg, promise);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            // 提交给 `handler` 处理器。
            handler.sent(channel, msg);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            handler.caught(channel, cause);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

}
