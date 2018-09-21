package com.cfs.mini.remoting.transport.netty;

import com.cfs.mini.common.Constants;
import com.cfs.mini.common.URL;
import com.cfs.mini.common.utils.ExecutorUtil;
import com.cfs.mini.common.utils.NamedThreadFactory;
import com.cfs.mini.remoting.ChannelHandler;
import com.cfs.mini.remoting.RemotingException;
import com.cfs.mini.remoting.transport.AbstractClient;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NettyClient extends AbstractClient {

    private volatile org.jboss.netty.channel.Channel channel;

    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }

    private static final ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(new NamedThreadFactory("NettyClientBoss", true)),
            Executors.newCachedThreadPool(new NamedThreadFactory("NettyClientWorker", true)),
            Constants.DEFAULT_IO_THREADS);


    private ClientBootstrap bootstrap;

    @Override
    protected void doOpen() {
        // 实例化 ServerBootstrap
        bootstrap = new ClientBootstrap(channelFactory);
        // 设置可选项
        // config
        // @see org.jboss.netty.channel.socket.SocketChannelConfig
        bootstrap.setOption("keepAlive", true);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("connectTimeoutMillis", getTimeout());
        // 创建 NettyHandler 对象
        final NettyHandler nettyHandler = new NettyHandler(getUrl(), this);
        // 设置责任链路
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                // 创建 NettyCodecAdapter 对象
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyClient.this);
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("handler", nettyHandler); // 处理器
                return pipeline;
            }
        });

    }

    @Override
    protected com.cfs.mini.remoting.Channel getChannel() {
        Channel c = channel;
        if (c == null || !c.isConnected())
            return null;
        return NettyChannel.getOrAddChannel(c, getUrl(), this);
    }

    /**
     * 这里会获取相应的服务暴露的ip进行连接
     * */
    @Override
    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();

        /**连接服务器*/
        ChannelFuture future = bootstrap.connect(getConnectAddress());

        try{
            // 等待连接成功或者超时
            boolean ret = future.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);

            if(ret&&future.isSuccess()){
                Channel newChannel = future.getChannel();
                newChannel.setInterestOps(Channel.OP_READ_WRITE);
                try{
                    //关闭老的连接
                    Channel oldChannel = NettyClient.this.channel; // copy reference
                    if (oldChannel != null) {
                        try {
                            oldChannel.close();
                        } finally {
                            NettyChannel.removeChannelIfDisconnected(oldChannel);
                        }
                    }
                }finally {
                    if (NettyClient.this.isClosed()) {
                        try {
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
            }


        }finally {
            // 未连接，取消任务
            if (!isConnected()) {
                future.cancel();
            }
        }

    }

}
