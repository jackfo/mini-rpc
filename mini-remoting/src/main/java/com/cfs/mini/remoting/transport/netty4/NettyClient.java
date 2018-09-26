package com.cfs.mini.remoting.transport.netty4;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.Version;
import com.cfs.mini.common.logger.Logger;
import com.cfs.mini.common.logger.LoggerFactory;
import com.cfs.mini.common.utils.NetUtils;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.Client;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.Server;
import com.cfs.mini.remoting.transport.AbstractClient;
import com.cfs.mini.remoting.transport.AbstractServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

public class NettyClient extends AbstractClient implements Client {

    /**相应的日志记录*/
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);


    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));

    private Bootstrap bootstrap;

    private volatile io.netty.channel.Channel channel;

    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }



    @Override
    protected void doOpen() {

        //创建NettyClientHandler

        final NettyClientHandler nettyClientHandler = new NettyClientHandler(getUrl(), this);
        // 实例化 ServerBootstrap
        bootstrap = new Bootstrap();
        bootstrap
                // 设置它的线程组
                .group(nioEventLoopGroup)
                // 设置可选项
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                // 设置 Channel类型
                .channel(NioSocketChannel.class);

        // 设置连接超时时间
        if (getTimeout() < 3000) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
        } else {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout());
        }

        // 设置责任链路
        bootstrap.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                        .addLast("handler", nettyClientHandler); // 处理器
            }
        });
    }

    @Override
    @SuppressWarnings("Duplicates")
    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        try {
            // 等待连接成功或者超时
            boolean ret = future.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS);
            // 连接成功
            if (ret && future.isSuccess()) {
                Channel newChannel = future.channel();
                try {
                    // 关闭老的连接
                    // Close old channel
                    Channel oldChannel = NettyClient.this.channel; // copy reference
                    if (oldChannel != null) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                            }
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                } finally {
                    // 若 NettyClient 被关闭，关闭连接
                    if (NettyClient.this.isClosed()) {
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            }
                            newChannel.close();
                        } finally {
                            NettyClient.this.channel = null;
                            NettyChannel.removeChannelIfDisconnected(newChannel);
                        }
                        // 设置新连接
                    } else {
                        NettyClient.this.channel = newChannel;
                    }
                }
                // 发生异常，抛出 RemotingException 异常
            } else if (future.cause() != null) {
                throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
                // 无结果（连接超时），抛出 RemotingException 异常
            } else {
                throw new RemotingException(this, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());
            }
        } finally {
            if (!isConnected()) { // 【TODO 8028】为什么不取消 future
                //future.cancel(true);
            }
        }
    }

    @Override
    protected com.cfs.mini.remoting.Channel getChannel() {
        Channel c = channel;
        if (c == null || !c.isActive())
            return null;
        return NettyChannel.getOrAddChannel(c, getUrl(), this);
    }



}
